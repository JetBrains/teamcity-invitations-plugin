package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TeamCityCoreFacade {

    @Nullable
    Role findRoleById(String roleId);

    @NotNull
    String getEditProjectPageUrl(@NotNull String projectExtId);

    @NotNull
    SProject createProjectAsSystem(@NotNull String parentExtId, @NotNull String name);

    void addRoleAsSystem(@NotNull SUser user, @NotNull Role role, @NotNull SProject project);
}
