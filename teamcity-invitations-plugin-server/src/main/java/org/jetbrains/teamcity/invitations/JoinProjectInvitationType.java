package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.groups.SUserGroup;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.serverSide.impl.auth.ServerAuthUtil;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class JoinProjectInvitationType implements InvitationType<JoinProjectInvitationType.InvitationImpl> {

    private final TeamCityCoreFacade core;

    public JoinProjectInvitationType(TeamCityCoreFacade core) {
        this.core = core;
    }

    @NotNull
    @Override
    public String getId() {
        return "joinProjectInvitation";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Join the project";
    }

    @NotNull
    @Override
    public String getDescriptionViewPath() {
        return core.getPluginResourcesPath("joinProjectInvitationDescription.jsp");
    }

    @NotNull
    @Override
    public ModelAndView getEditPropertiesView(@NotNull AuthorityHolder authorityHolder, @NotNull SProject project, @Nullable InvitationImpl invitation) {
        ModelAndView modelAndView = new ModelAndView(core.getPluginResourcesPath("joinProjectInvitationProperties.jsp"));
        modelAndView.getModel().put("name", invitation == null ? "Join Project Invitation" : invitation.getName());
        List<Role> availableRoles = core.getAvailableRoles().stream().filter(Role::isProjectAssociationSupported).collect(toList());
        modelAndView.getModel().put("roles", availableRoles);

        List<SUserGroup> availableGroups = core.getAvailableGroups().stream()
                .filter(group -> group.getPermissionsGrantedForProject(project.getProjectId()).contains(Permission.VIEW_PROJECT))
                .filter(group -> ServerAuthUtil.canAddToRemoveFromGroup(authorityHolder, group))
                .collect(toList());
        modelAndView.getModel().put("groups", availableGroups);

        modelAndView.getModel().put("name", invitation == null ? "Join Project Invitation" : invitation.getName());
        modelAndView.getModel().put("multiuser", invitation == null ? "true" : invitation.multi);

        String preselectedRole = null;
        String preselectedGroup = null;

        if (invitation != null) {
            preselectedGroup = invitation.groupKey;
            preselectedRole = invitation.roleId;
        } else {
            preselectedRole = availableRoles.stream()
                    .filter(role -> role.getPermissions().contains(Permission.RUN_BUILD))
                    .sorted(Comparator.comparing(role -> role.getPermissions().toList().size()))
                    .findFirst()
                    .map(Role::getId)
                    .orElse(availableRoles.get(0).getId());
        }

        modelAndView.getModel().put("roleId", preselectedRole);
        modelAndView.getModel().put("groupKey", preselectedGroup);
        modelAndView.getModel().put("welcomeText", invitation == null ?
                authorityHolder.getAssociatedUser().getDescriptiveName() + " invites you to join the " + project.getFullName() + " project" :
                invitation.welcomeText);
        return modelAndView;
    }

    @NotNull
    @Override
    public InvitationImpl createNewInvitation(@NotNull HttpServletRequest request, @NotNull SProject project, @NotNull String token) {
        String name = request.getParameter("name");
        String roleId = !StringUtil.isEmptyOrSpaces(request.getParameter("role")) ? request.getParameter("role") : null;
        String groupKey = !StringUtil.isEmptyOrSpaces(request.getParameter("group")) ? request.getParameter("group") : null;
        String welcomeText = StringUtil.emptyIfNull(request.getParameter("welcomeText"));
        boolean multiuser = Boolean.parseBoolean(request.getParameter("multiuser"));
        return createNewInvitation(SessionUser.getUser(request), name, token, project, roleId, groupKey, multiuser, welcomeText);
    }

    @NotNull
    public InvitationImpl createNewInvitation(SUser inviter, String name, String token, SProject project, String roleId,
                                              String groupKey, boolean multiuser, String welcomeText) {
        return new InvitationImpl(inviter, name, token, project, roleId, groupKey, multiuser, welcomeText);
    }

    @NotNull
    @Override
    public InvitationImpl readFrom(@NotNull Map<String, String> params, @NotNull SProject project) {
        return new InvitationImpl(params, project);
    }

    @Override
    public boolean isAvailableFor(@NotNull AuthorityHolder authorityHolder, @NotNull SProject project) {
        return core.runAsSystem(() ->
                authorityHolder.isPermissionGrantedForProject(project.getProjectId(), Permission.CHANGE_USER_ROLES_IN_PROJECT)
        );
    }

    public final class InvitationImpl extends AbstractInvitation {
        @Nullable
        private final String roleId;
        @Nullable
        private final String groupKey;

        InvitationImpl(@NotNull SUser currentUser, @NotNull String name, @NotNull String token, @NotNull SProject project, @Nullable String roleId,
                       @Nullable String groupKey, boolean multi, @Nullable String welcomeText) {
            super(project, name, token, multi, JoinProjectInvitationType.this, currentUser.getId(), welcomeText);
            if (groupKey == null && roleId == null) {
                throw new IllegalArgumentException("Role or group must be specified");
            }
            this.roleId = roleId;
            this.groupKey = groupKey;
        }

        public InvitationImpl(Map<String, String> params, SProject project) {
            super(params, project, JoinProjectInvitationType.this);
            this.roleId = params.get("roleId");
            this.groupKey = params.get("groupKey");
        }

        @NotNull
        @Override
        protected String getLandingPage() {
            return core.getPluginResourcesPath("invitationLanding.jsp");
        }

        @NotNull
        @Override
        public Map<String, String> asMap() {
            Map<String, String> result = super.asMap();
            if (roleId != null) result.put("roleId", roleId);
            if (groupKey != null) result.put("groupKey", groupKey);
            return result;
        }

        @Override
        public boolean isAvailableFor(@NotNull AuthorityHolder user) {
            SUserGroup group = getGroup();
            return user.isPermissionGrantedForProject(project.getProjectId(), Permission.CHANGE_USER_ROLES_IN_PROJECT)
                    && (group == null || ServerAuthUtil.canAddToRemoveFromGroup(user, group));
        }

        @NotNull
        public ModelAndView invitationAccepted(@NotNull SUser user, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
            try {
                SProject created = core.runAsSystem(() -> {
                    Role role = getRole();
                    SUserGroup group = getGroup();
                    if (role == null && group == null) {
                        throw new InvitationException("Failed to proceed invitation with a non-existing role '" + roleId + "' and group '" + groupKey + "'");
                    }

                    if (role != null) core.addRole(user, role, project.getProjectId());
                    if (group != null) core.assignToGroup(user, group);

                    Loggers.SERVER.info("User " + user.describe(false) + " registered on invitation '" + token + "'. " +
                            (role != null ? ("User got the role " + role.describe(false) + " in the project " + project.describe(false)) : "") +
                            (group != null ? ("User assigned to the group " + group.describe(false)) : "")
                    );
                    return project;
                });


                if (user.isPermissionGrantedForProject(created.getProjectId(), Permission.EDIT_PROJECT)) {
                    return new ModelAndView(new RedirectView("/editProject.html?projectId=" + created.getExternalId(), true));
                }
                return new ModelAndView(new RedirectView("/project.html?projectId=" + created.getExternalId(), true));
            } catch (Exception e) {
                Loggers.SERVER.warn("Failed to create project for the invited user " + user.describe(false), e);
                return new ModelAndView(new RedirectView("/", true));
            }
        }

        @Nullable
        public Role getRole() {
            return roleId != null ? JoinProjectInvitationType.this.core.findRoleById(roleId) : null;
        }

        @Nullable
        public SUserGroup getGroup() {
            return groupKey != null ? JoinProjectInvitationType.this.core.findGroup(groupKey) : null;
        }

        @Nullable
        public SUser getUser() {
            return JoinProjectInvitationType.this.core.getUser(createdByUserId);
        }
    }
}
