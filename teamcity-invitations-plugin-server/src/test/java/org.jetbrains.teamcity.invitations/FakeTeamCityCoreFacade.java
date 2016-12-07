package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.DuplicateProjectNameException;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.auth.*;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.impl.RoleEntryImpl;
import jetbrains.buildServer.util.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class FakeTeamCityCoreFacade implements TeamCityCoreFacade {

    private final Map<String, Role> roles = new HashMap<>();
    private final List<SProject> projects = new ArrayList<>();
    private final List<SUser> users = new ArrayList<>();
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
    public SProject createProjectAsSystem(@Nullable String parentExtId, @NotNull String name) {
        if (projects.stream().anyMatch(p -> p.getName().equals(name))) {
            throw new DuplicateProjectNameException("Already exists");
        }
        SProject project = mock(SProject.class);
        when(project.getExternalId()).thenReturn(name);
        when(project.getProjectId()).thenReturn(name);
        when(project.getName()).thenReturn(name);
        when(project.getParentProjectExternalId()).thenReturn(parentExtId);

        MultiMap<String, SProjectFeatureDescriptor> features = new MultiMap<>();

        when(project.addFeature(anyString(), anyMap())).thenAnswer(invocation -> {
            ProjectFeatureDescriptorImpl descriptor = new ProjectFeatureDescriptorImpl(features.size() + "", invocation.getArgument(0), invocation.getArgument(1), project);
            features.putValue(invocation.getArgument(0), descriptor);
            return descriptor;
        });

        when(project.getAvailableFeaturesOfType(anyString())).thenAnswer(invocation -> features.get(invocation.getArgument(0)));

        when(project.removeFeature(anyString())).thenAnswer(invocation -> {
            List<SProjectFeatureDescriptor> toRemove = features.values().stream()
                    .flatMap(List::stream)
                    .filter(feature -> feature.getId().equals(invocation.getArgument(0)))
                    .collect(toList());
            for (SProjectFeatureDescriptor featureDescriptor : toRemove) {
                features.removeValue(featureDescriptor);
            }
            return null;
        });
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
        user.addRole(RoleScope.projectScope(project.getProjectId()), role);
    }

    @NotNull
    @Override
    public List<SProject> getActiveProjects() {
        return new ArrayList<>(projects);
    }

    @Override
    public void persist(@NotNull SProject project, @NotNull String description) {

    }

    @NotNull
    @Override
    public List<Role> getAvailableRoles() {
        return new ArrayList<>(roles.values());
    }

    @Nullable
    @Override
    public SUser getUser(long userId) {
        return users.get((int) userId - 1);
    }

    @NotNull
    @Override
    public String getPluginResourcesPath(@NotNull String path) {
        return path;
    }

    Role addRole(String id, Permissions permissions, boolean isProjectAssociationSupported) {
        Role role = mock(Role.class);
        when(role.getId()).thenReturn(id);
        when(role.getPermissions()).thenReturn(permissions);
        when(role.isProjectAssociationSupported()).thenReturn(isProjectAssociationSupported);
        when(role.toString()).thenReturn(id);
        roles.put(id, role);
        return role;
    }

    @Nullable
    SProject getProject(String extId) {
        return projects.stream().filter(p -> p.getExternalId().equals(extId)).findFirst().orElse(null);
    }

    @NotNull
    SUser createUser(String username) {
        Collection<RoleEntry> roles = Collections.synchronizedSet(new HashSet<>());
        SUser user = mock(SUser.class);
        when(user.getId()).thenReturn(users.size() + 1L);
        when(user.getUsername()).thenReturn(username);
        when(user.getRoles()).thenReturn(roles);

        when(user.getRolesWithScope(any(RoleScope.class))).thenAnswer(invocation ->
                roles.stream().
                        filter(roleEntry -> roleEntry.getScope().equals(invocation.getArgument(0))).
                        map(RoleEntry::getRole).
                        collect(toList()));

        when(user.isPermissionGrantedForProject(anyString(), any(Permission.class))).thenAnswer(invocation -> {
            String projectIntId = invocation.getArgument(0);
            Permission permission = invocation.getArgument(1);

            return roles.stream().
                    filter(entry -> projectIntId.equals(entry.getScope().getProjectId()) || entry.getScope().isGlobal()).
                    findFirst().
                    map(roleEntry -> roleEntry.getRole().getPermissions().contains(permission)).
                    orElse(false);
        });

        when(user.isPermissionGrantedForAnyProject(any(Permission.class))).thenAnswer(invocation -> {
            Permission permission = invocation.getArgument(0);
            return roles.stream().
                    filter(entry -> entry.getRole().getPermissions().contains(permission)).
                    findAny().
                    isPresent();
        });

        doAnswer(invocation -> {
            roles.add(new RoleEntryImpl(invocation.getArgument(0), invocation.getArgument(1)));
            return null;
        }).when(user).addRole(any(RoleScope.class), any(Role.class));

        users.add(user);
        return user;
    }
}
