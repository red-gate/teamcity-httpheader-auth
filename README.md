# teamcity-httpheader-auth

HTTP Headers authentication plugin.

## Summary
Will allow authenticated access to Teamcity to http requests with the following (hardcoded) headers:
* `X-Forwarded-Login` (the username of the user)
* `X-Forwarded-Name` (The name of the user)
* `X-Forwarded-Email` (The email address of the user)
* `X-Forwarded-Groups` (`,` separated list of groups the user belongs to)

This might be useful when Teamcity instances are hosted behind a reverse-proxy which is responsible for authentication.

## Installation

Install to Teamcity following https://confluence.jetbrains.com/display/TCD18/Installing+Additional+Plugins

## Configuration

Nope

## Development

Plugin can be build by `gradle serverPlugin`. The plugin zip file will be available in `build/distributions`

## Release a new version

```
# Example for version 0.1.0
git tag teamcity-httpheader-auth-0.1.0
gradle serverPlugin
git push origin teamcity-httpheader-auth-0.1.0
# Create release on github https://github.com/red-gate/teamcity-httpheader-auth/releases
# and upload build/distributions/teamcity-httpheader-auth-0.1.0.zip there.
```
