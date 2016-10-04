package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.serverSide.auth.RoleScope;
import jetbrains.buildServer.serverSide.auth.RolesManager;
import jetbrains.buildServer.serverSide.identifiers.ProjectIdentifiersManager;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TeamCityCoreFacadeImpl implements TeamCityCoreFacade {
    private final RolesManager rolesManager;
    private final ProjectManager projectManager;
    private final ProjectIdentifiersManager projectIdentifiersManager;
    private final WebLinks webLinks;
    private final SecurityContextEx securityContext;

    public TeamCityCoreFacadeImpl(RolesManager rolesManager, ProjectManager projectManager, ProjectIdentifiersManager projectIdentifiersManager, WebLinks webLinks, SecurityContextEx securityContext) {
        this.rolesManager = rolesManager;
        this.projectManager = projectManager;
        this.projectIdentifiersManager = projectIdentifiersManager;
        this.webLinks = webLinks;
        this.securityContext = securityContext;
    }

    @Nullable
    @Override
    public Role findRoleById(String roleId) {
        return rolesManager.findRoleById(roleId);
    }

    @NotNull
    @Override
    public String getEditProjectPageUrl(@NotNull String projectExtId) {
        return webLinks.getEditProjectPageUrl(projectExtId);
    }

    @NotNull
    @Override
    public SProject createProjectAsSystem(@NotNull String parentExtId, @NotNull String name) {
        try {
            return securityContext.runAsSystem(() -> {
                SProject parent = projectManager.findProjectByExternalId(parentExtId);
                if (parent == null) {
                    throw new ProjectNotFoundException("Unable to create project for user: parent project with external id = " + parentExtId + " not found");
                }
                String projectExternalId = projectIdentifiersManager.generateNewExternalId(parentExtId, name, null);
                return parent.createProject(projectExternalId, name);
            });
        } catch (Throwable throwable) {
            ExceptionUtil.rethrowAsRuntimeException(throwable);
            return null;
        }
    }

    @Override
    public void addRoleAsSystem(@NotNull SUser user, @NotNull Role role, @NotNull SProject project) {
        try {
            securityContext.runAsSystem(() -> {
                user.addRole(RoleScope.projectScope(project.getProjectId()), role);
            });
        } catch (Throwable throwable) {
            ExceptionUtil.rethrowAsRuntimeException(throwable);
        }
    }
}
