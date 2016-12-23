package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.groups.SUserGroup;
import jetbrains.buildServer.serverSide.DuplicateProjectNameException;
import jetbrains.buildServer.serverSide.ProjectsModelListener;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.auth.*;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.impl.auth.SecurityContextImpl;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.impl.RoleEntryImpl;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.ExceptionUtil;
import jetbrains.buildServer.util.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class FakeTeamCityCoreFacade implements TeamCityCoreFacade {

    private final Map<String, Role> roles = new HashMap<>();
    private final List<SProject> projects = new ArrayList<>();
    private final List<SUser> users = new ArrayList<>();
    private final ConcurrentMap<SUserGroup, List<SUser>> groups = new ConcurrentHashMap<>();
    private SecurityContextImpl securityContext;
    private EventDispatcher<ProjectsModelListener> events;

    public FakeTeamCityCoreFacade(SecurityContextImpl securityContext, EventDispatcher<ProjectsModelListener> events) {
        this.securityContext = securityContext;
        this.events = events;
        doCreateProject(null, "_Root");
    }

    @Nullable
    @Override
    public Role findRoleById(String roleId) {
        return roles.get(roleId);
    }

    @NotNull
    @Override
    public SProject createProject(@NotNull String parentExtId, @NotNull String name) {
        if (projects.stream().anyMatch(p -> p.getName().equals(name))) {
            throw new DuplicateProjectNameException("Already exists");
        }
        SProject parent = projects.stream().filter(p -> p.getExternalId().equals(parentExtId)).findFirst().orElse(null);
        if (parent == null) {
            throw new IllegalArgumentException("No project with " + parentExtId + " found");
        }
        if (!securityContext.getAuthorityHolder().isPermissionGrantedForProject(parent.getProjectId(), Permission.CREATE_SUB_PROJECT)) {
            throw new AccessDeniedException(securityContext.getAuthorityHolder(), "You can't create project under " + parentExtId);
        }

        SProject project = doCreateProject(parentExtId, name);
        events.getMulticaster().projectCreated(project.getProjectId(), ((SUser) securityContext.getAuthorityHolder()));
        return project;
    }

    @NotNull
    private SProject doCreateProject(@Nullable String parentExtId, @NotNull String name) {
        SProject project = mock(SProject.class);
        when(project.getExternalId()).thenReturn(name);
        when(project.getProjectId()).thenReturn(name);
        when(project.getName()).thenReturn(name);
        when(project.getParentProjectExternalId()).thenReturn(parentExtId);
        when(project.getParentProjectId()).thenReturn(parentExtId);

        MultiMap<String, SProjectFeatureDescriptor> features = new MultiMap<>();

        when(project.addFeature(anyString(), anyMap())).thenAnswer(invocation -> {
            ProjectFeatureDescriptorImpl descriptor = new ProjectFeatureDescriptorImpl(features.size() + "", invocation.getArgument(0), invocation.getArgument(1), project);
            features.putValue(invocation.getArgument(0), descriptor);
            events.getMulticaster().projectFeatureAdded(project, descriptor);
            return descriptor;
        });

        when(project.getOwnFeaturesOfType(anyString())).thenAnswer(invocation -> features.get(invocation.getArgument(0)));

        when(project.removeFeature(anyString())).thenAnswer(invocation -> {
            List<SProjectFeatureDescriptor> toRemove = features.values().stream()
                    .flatMap(List::stream)
                    .filter(feature -> feature.getId().equals(invocation.getArgument(0)))
                    .collect(toList());
            for (SProjectFeatureDescriptor featureDescriptor : toRemove) {
                features.removeValue(featureDescriptor);
                events.getMulticaster().projectFeatureRemoved(project, featureDescriptor);
            }
            return null;
        });
        projects.add(project);
        return project;
    }

    @Nullable
    @Override
    public SProject findProjectByExtId(@Nullable String projectExtId) {
        if (projectExtId == null) return null;
        SProject found = projects.stream().filter(pr -> pr.getExternalId().equals(projectExtId)).findFirst().orElse(null);
        if (found != null) {
            if (!securityContext.getAuthorityHolder().isPermissionGrantedForProject(found.getProjectId(), Permission.CREATE_SUB_PROJECT)) {
                throw new AccessDeniedException(securityContext.getAuthorityHolder(), "You can't create project under " + found.getProjectId());
            }
        }
        return found;
    }

    @Nullable
    @Override
    public SProject findProjectByIntId(String projectIntId) {
        SProject found = projects.stream().filter(pr -> pr.getProjectId().equals(projectIntId)).findFirst().orElse(null);
        if (found != null) {
            if (!securityContext.getAuthorityHolder().isPermissionGrantedForProject(found.getProjectId(), Permission.CREATE_SUB_PROJECT)) {
                throw new AccessDeniedException(securityContext.getAuthorityHolder(), "You can't create project under " + found.getProjectId());
            }
        }
        return found;
    }

    @Override
    public void addRole(@NotNull SUser user, @NotNull Role role, @NotNull String project) {
        if (!securityContext.getAuthorityHolder().isPermissionGrantedForProject(project, Permission.CHANGE_USER_ROLES_IN_PROJECT)) {
            throw new AccessDeniedException(securityContext.getAuthorityHolder(), "You can't assign roles in the project " + project);
        }
        user.addRole(RoleScope.projectScope(project), role);
    }

    @Override
    public void assignToGroup(@NotNull SUser user, @NotNull SUserGroup group) {
        if (!securityContext.getAuthorityHolder().isPermissionGrantedGlobally(Permission.ASSIGN_USERS_ADD_SUBGROUPS)) {
            throw new AccessDeniedException(securityContext.getAuthorityHolder(), "You can't add users to groups");
        }
        groups.computeIfAbsent(group, g -> new ArrayList<>()).add(user);
    }

    @NotNull
    @Override
    public List<SProject> getActiveProjects() {
        return projects.stream().filter(p -> securityContext.getAuthorityHolder().isPermissionGrantedForProject(p.getProjectId(), Permission.VIEW_PROJECT)).collect(toList());
    }

    @Override
    public void persist(@NotNull SProject project, @NotNull String description) {
        if (!securityContext.getAuthorityHolder().isPermissionGrantedForProject(project.getProjectId(), Permission.EDIT_PROJECT)) {
            throw new AccessDeniedException(securityContext.getAuthorityHolder(), "You can't edit project " + project.getProjectId());
        }
    }

    @Override
    public <T> T runAsSystem(Supplier<T> action) {
        try {
            return securityContext.runAsSystem(action::get);
        } catch (Throwable throwable) {
            ExceptionUtil.rethrowAsRuntimeException(throwable);
            return null;
        }
    }

    @NotNull
    @Override
    public List<Role> getAvailableRoles() {
        return new ArrayList<>(roles.values());
    }

    @NotNull
    @Override
    public Collection<SUserGroup> getAvailableGroups() {
        return groups.keySet();
    }

    @Nullable
    @Override
    public SUserGroup findGroup(String groupKey) {
        return groups.keySet().stream().filter(g -> g.getKey().equals(groupKey)).findFirst().orElse(null);
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
        SUser user = mock(UserEx.class);
        when(user.getId()).thenReturn(users.size() + 1L);
        when(user.getUsername()).thenReturn(username);
        when(user.describe(anyBoolean())).thenReturn(username);
        setupRolesMocks(user);
        users.add(user);
        return user;
    }

    @NotNull
    SUserGroup createGroup(String groupKey) {
        SUserGroup group = mock(SUserGroup.class);
        when(group.getKey()).thenReturn(groupKey);
        setupRolesMocks(group);
        groups.put(group, new ArrayList<>());
        return group;
    }

    List<SUser> getGroupUsers(SUserGroup group) {
        return groups.get(group);
    }

    private <T extends RolesHolder & AuthorityHolder> void setupRolesMocks(T user) {
        Collection<RoleEntry> roles = Collections.synchronizedSet(new HashSet<>());

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
                    anyMatch(entry -> entry.getRole().getPermissions().contains(permission));
        });

        doAnswer(invocation -> {
            roles.add(new RoleEntryImpl(invocation.getArgument(0), invocation.getArgument(1)));
            return null;
        }).when(user).addRole(any(RoleScope.class), any(Role.class));
    }
}
