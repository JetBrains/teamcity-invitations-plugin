package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.groups.SUserGroup;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface TeamCityCoreFacade {

    //ROLES
    @Nullable
    Role findRoleById(String roleId);

    @NotNull
    List<Role> getAvailableRoles();

    @NotNull
    Collection<SUserGroup> getAvailableGroups();

    @Nullable
    SUserGroup findGroup(String groupKey);

    @Nullable
    SUser getUser(long userId);

    void addRoleAsSystem(@NotNull SUser user, @NotNull Role role, @NotNull SProject project);

    void assignToGroup(@NotNull SUser user, @NotNull SUserGroup group);

    //PROJECTS
    @NotNull
    SProject createProjectAsSystem(@Nullable String parentExtId, @NotNull String name);

    @Nullable
    SProject findProjectByExtId(String projectExtId);

    @NotNull
    List<SProject> getActiveProjects();

    void persist(@NotNull SProject project, @NotNull String description);

    //Plugins
    @NotNull
    String getPluginResourcesPath(@NotNull String path);

}
