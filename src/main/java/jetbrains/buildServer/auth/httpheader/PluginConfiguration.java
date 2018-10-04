package jetbrains.buildServer.auth.httpheader;

import jetbrains.buildServer.groups.UserGroupManager;
import jetbrains.buildServer.serverSide.auth.LoginConfiguration;
import jetbrains.buildServer.users.UserModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PluginConfiguration {

    @Bean
    public AuthenticationScheme oAuthAuthenticationScheme(LoginConfiguration loginConfiguration, @NotNull UserModel userModel, @NotNull UserGroupManager userGroupManager) {
        AuthenticationScheme authenticationScheme = new AuthenticationScheme(userModel, userGroupManager);
        loginConfiguration.registerAuthModuleType(authenticationScheme);
        return authenticationScheme;
    }
}
