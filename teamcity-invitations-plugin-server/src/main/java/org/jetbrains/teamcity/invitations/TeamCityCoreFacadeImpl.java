package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.groups.SUserGroup;
import jetbrains.buildServer.groups.UserGroupManager;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.serverSide.auth.RoleScope;
import jetbrains.buildServer.serverSide.auth.RolesManager;
import jetbrains.buildServer.serverSide.identifiers.ProjectIdentifiersManager;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.util.ExceptionUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class TeamCityCoreFacadeImpl implements TeamCityCoreFacade {
    private final RolesManager rolesManager;
    private final ProjectManager projectManager;
    private final ProjectIdentifiersManager projectIdentifiersManager;
    private final SecurityContextEx securityContext;
    private final UserGroupManager userGroupManager;
    private final PluginDescriptor pluginDescriptor;
    private final UserModel userModel;
    private final ConfigActionFactory myConfigActionFactory;

    public TeamCityCoreFacadeImpl(RolesManager rolesManager, ProjectManager projectManager, ProjectIdentifiersManager projectIdentifiersManager, SecurityContextEx securityContext,
                                  UserGroupManager userGroupManager, PluginDescriptor pluginDescriptor, UserModel userModel, ConfigActionFactory myConfigActionFactory) {
        this.rolesManager = rolesManager;
        this.projectManager = projectManager;
        this.projectIdentifiersManager = projectIdentifiersManager;
        this.securityContext = securityContext;
        this.userGroupManager = userGroupManager;
        this.pluginDescriptor = pluginDescriptor;
        this.userModel = userModel;
        this.myConfigActionFactory = myConfigActionFactory;
    }

    @Nullable
    @Override
    public Role findRoleById(String roleId) {
        return rolesManager.findRoleById(roleId);
    }

    @NotNull
    @Override
    public SProject createProject(@Nullable String parentExtId, @NotNull String name) {
        SProject parent = projectManager.findProjectByExternalId(parentExtId);
        if (parent == null) {
            throw new ProjectNotFoundException("Unable to create project for user: parent project with external id = " + parentExtId + " not found");
        }
        String projectExternalId = projectIdentifiersManager.generateNewExternalId(parentExtId, name, null);
        SProject project = parent.createProject(projectExternalId, name);
        project.persist();
        return project;
    }

    @Nullable
    @Override
    public SProject findProjectByExtId(String projectExtId) {
        return projectManager.findProjectByExternalId(projectExtId);
    }

    @Nullable
    @Override
    public SProject findProjectByIntId(String projectIntId) {
        return projectManager.findProjectById(projectIntId);
    }

    @Override
    public void addRole(@NotNull SUser user, @NotNull Role role, @NotNull String project) {
        user.addRole(RoleScope.projectScope(project), role);
    }

    @Override
    public void assignToGroup(@NotNull SUser user, @NotNull SUserGroup group) {
        group.addUser(user);
    }

    @NotNull
    @Override
    public List<SProject> getActiveProjects() {
        return projectManager.getActiveProjects();
    }

    @Override
    public void persist(@NotNull SProject project, @NotNull String description) {
        project.persist(myConfigActionFactory.createAction(project, description));
    }

    @NotNull
    @Override
    public List<Role> getAvailableRoles() {
        return rolesManager.getAvailableRoles();
    }

    @NotNull
    @Override
    public Collection<SUserGroup> getAvailableGroups() {
        return userGroupManager.getUserGroups();
    }

    @Nullable
    @Override
    public SUserGroup findGroup(String groupKey) {
        return userGroupManager.findUserGroupByKey(groupKey);
    }

    @Nullable
    @Override
    public SUser getUser(long userId) {
        return userModel.findUserById(userId);
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
    public String getPluginResourcesPath(@NotNull String path) {
        return pluginDescriptor.getPluginResourcesPath(path);
    }
}
