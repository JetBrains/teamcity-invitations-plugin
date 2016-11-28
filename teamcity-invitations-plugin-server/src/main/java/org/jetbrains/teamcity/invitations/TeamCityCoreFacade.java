package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public interface TeamCityCoreFacade {

    //ROLES
    @Nullable
    Role findRoleById(String roleId);

    @NotNull
    List<Role> getAvailableRoles();

    @Nullable
    SUser getUser(long userId);

    void addRoleAsSystem(@NotNull SUser user, @NotNull Role role, @NotNull SProject project);

    //PROJECTS
    @NotNull
    SProject createProjectAsSystem(@NotNull String parentExtId, @NotNull String name);

    @Nullable
    SProject findProjectByExtId(String projectExtId);

    @NotNull
    List<SProject> getActiveProjects();

    //Plugins
    @NotNull
    File getPluginDataDir();

    @NotNull
    String getPluginResourcesPath(@NotNull String path);
}
