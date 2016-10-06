package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.serverSide.auth.RoleScope;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FakeTeamCityCoreFacade implements TeamCityCoreFacade {

    private final Map<String, Role> roles = new HashMap<>();
    private final List<SProject> projects = new ArrayList<>();
    private final File pluginDataDir;

    public FakeTeamCityCoreFacade(File pluginDataDir) {
        this.pluginDataDir = pluginDataDir;
    }

    @Nullable
    @Override
    public Role findRoleById(String roleId) {
        return roles.get(roleId);
    }

    @NotNull
    @Override
    public String getEditProjectPageUrl(@NotNull String projectExtId) {
        return new RelativeWebLinks().getEditProjectPageUrl(projectExtId);
    }

    @NotNull
    @Override
    public SProject createProjectAsSystem(@NotNull String parentExtId, @NotNull String name) {
        SProject project = mock(SProject.class);
        when(project.getExternalId()).thenReturn(name);
        when(project.getProjectId()).thenReturn(name);
        when(project.getName()).thenReturn(name);
        when(project.getParentProjectExternalId()).thenReturn(parentExtId);
        projects.add(project);
        return project;
    }

    @Nullable
    @Override
    public SProject findProjectByExtId(String projectExtId) {
        return projects.stream().filter(pr -> pr.getExternalId().equals(projectExtId)).findFirst().orElse(null);
    }

    @Override
    public void addRoleAsSystem(@NotNull SUser user, @NotNull Role role, @NotNull SProject project) {
        when(user.getRolesWithScope(RoleScope.projectScope(project.getProjectId()))).thenReturn(asList(role));
    }

    @Override
    public List<SProject> getActiveProjects() {
        return new ArrayList<>(projects);
    }

    @Override
    public File getPluginDataDir() {
        return pluginDataDir;
    }

    void addRole(String id) {
        Role role = mock(Role.class);
        when(role.getId()).thenReturn(id);
        roles.put(id, role);
    }

    @Nullable
    SProject getProject(String extId) {
        return projects.stream().filter(p -> p.getExternalId().equals(extId)).findFirst().orElse(null);
    }

    @NotNull
    SUser createUser(String username) {
        SUser user = mock(SUser.class);
        when(user.getUsername()).thenReturn(username);
        return user;
    }
}
