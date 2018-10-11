package jetbrains.buildServer.auth.httpheader;

import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationResult;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationSchemeAdapter;
import jetbrains.buildServer.controllers.interceptors.auth.util.HttpAuthUtil;
import jetbrains.buildServer.groups.SUserGroup;
import jetbrains.buildServer.groups.UserGroup;
import jetbrains.buildServer.groups.UserGroupException;
import jetbrains.buildServer.groups.UserGroupManager;
import jetbrains.buildServer.serverSide.auth.ServerPrincipal;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static jetbrains.buildServer.groups.UserGroupManager.MAX_GROUP_KEY_LENGTH;


public class AuthenticationScheme extends HttpAuthenticationSchemeAdapter {

    private static final Logger LOG = Logger.getLogger(AuthenticationScheme.class);
    String HTTPHEADER_AUTH_SCHEME_NAME = "Headers";
    String HTTPHEADER_AUTH_SCHEME_DESCRIPTION = "Authentication via HTTP Headers";

    private final UserModel userModel;
    private final UserGroupManager userGroupManager;

    public AuthenticationScheme(@NotNull UserModel userModel, @NotNull UserGroupManager userGroupManager) {
        this.userModel = userModel;
        this.userGroupManager = userGroupManager;
    }

    @NotNull
    @Override
    protected String doGetName() {
        return HTTPHEADER_AUTH_SCHEME_NAME;
    }

    @NotNull
    @Override
    public String getDescription() {
        return HTTPHEADER_AUTH_SCHEME_DESCRIPTION;
    }

    @Nullable
    @Override
    public String getEditPropertiesJspFilePath() {
        return null;
    }

    @Nullable
    @Override
    public Collection<String> validate(@NotNull Map<String, String> properties) {
        return super.validate(properties);
    }

    @NotNull
    @Override
    public HttpAuthenticationResult processAuthenticationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Map<String, String> schemeProperties) throws IOException {

        String username = request.getHeader("X-Forwarded-Login");

        if(username == null){
            return HttpAuthenticationResult.notApplicable();
        }

        String name = request.getHeader("X-Forwarded-Name");
        String emailAddress = request.getHeader("X-Forwarded-Email");
        List<String> groups = Arrays.asList(request.getHeader("X-Forwarded-Groups").split(","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());

        final SUser user = findOrCreateUser(username);

        // Update the user with the name, email address and groups coming from http headers.
        user.updateUserAccount(username, name, emailAddress);
        updateUserGroups(user, groups);

        return HttpAuthenticationResult.authenticated(new ServerPrincipal(HTTPHEADER_AUTH_SCHEME_NAME, username), false);
    }

    private SUser findOrCreateUser(@NotNull String username){
        SUser result = userModel.findUserAccount(null, username);
        if(result == null){
            result = userModel.createUserAccount(null, username);
        }
        return result;
    }

    private void addUserToGroup(@NotNull SUser user, @NotNull String groupName){
        SUserGroup group = userGroupManager.findUserGroupByName(groupName);
        if(group != null){
            group.addUser(user);
            LOG.info(String.format("Added user '%s' to group '%s'.", user.getUsername(), groupName));
        } else {
            // Don't attempt to create the group manually.
            LOG.warn(String.format("Could not add user '%s' to group '%s'. You would need to manually create the group to remove this warning.", user.getUsername(), groupName));
        }
    }

    private void removeUserFromGroup(@NotNull SUser user, @NotNull UserGroup userGroup){
        ((SUserGroup)userGroup).removeUser(user);
        LOG.info(String.format("Removed user '%s' from group '%s'.", user.getUsername(), userGroup.getName()));
    }

    private void updateUserGroups(@NotNull SUser user, @NotNull List<String> groups){
        // Special "All Users" group which we do not want to touch.
        UserGroup allUsersGroup = userGroupManager.getAllUsersGroup();

        List<UserGroup> currentUserGroups = userGroupManager.getHostGroupsOf(user)
                .stream()
                .filter(userGroup -> !userGroup.equals(allUsersGroup))
                .collect(Collectors.toList());

        // Remove user from groups it shouldn't belong to.
        currentUserGroups
                .stream()
                .filter(userGroup -> !groups.contains(userGroup.getName()))
                .forEach(userGroup -> removeUserFromGroup(user, userGroup));

        // Add user to groups it should belong to.
        groups.stream()
                .filter(groupName -> !currentUserGroups.stream().filter(userGroup -> userGroup.getName().equals(groupName)).findAny().isPresent())
                .forEach(groupName -> addUserToGroup(user, groupName));
    }
}
