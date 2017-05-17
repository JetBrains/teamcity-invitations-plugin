package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.groups.SUserGroup;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.serverSide.auth.RoleScope;
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

public class JoinProjectInvitationType extends AbstractInvitationType<JoinProjectInvitationType.InvitationImpl> implements InvitationType<JoinProjectInvitationType.InvitationImpl> {

    private final TeamCityCoreFacade core;

    public JoinProjectInvitationType(InvitationsStorage invitationsStorage, TeamCityCoreFacade core) {
        super(invitationsStorage, core);
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
        return "Join project";
    }

    @NotNull
    @Override
    public String getDescriptionViewPath() {
        return core.getPluginResourcesPath("joinProjectInvitationDescription.jsp");
    }

    @NotNull
    @Override
    public ModelAndView getEditPropertiesView(@NotNull SUser user, @NotNull SProject project, @Nullable InvitationImpl invitation) {
        ModelAndView modelAndView = new ModelAndView(core.getPluginResourcesPath("joinProjectInvitationProperties.jsp"));
        modelAndView.getModel().put("name", invitation == null ? getDescription() : invitation.getName());

        List<Role> availableRoles = getAvailableRoles(user, project);
        modelAndView.getModel().put("roles", availableRoles);

        List<SUserGroup> availableGroups = getAvailableGroups(user, project);
        modelAndView.getModel().put("groups", availableGroups);

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
                    .orElse(null);
            if (preselectedRole == null) {
                preselectedGroup = availableGroups.stream().findFirst().map(SUserGroup::getKey).orElse(null);
            }
        }

        modelAndView.getModel().put("roleId", preselectedRole);
        modelAndView.getModel().put("groupKey", preselectedGroup);
        modelAndView.getModel().put("welcomeText", invitation == null ? user.getDescriptiveName() + " invites you to join the " + project.getFullName() + " project" : invitation.welcomeText);
        return modelAndView;
    }

    @NotNull
    private List<SUserGroup> getAvailableGroups(@NotNull AuthorityHolder user, @NotNull SProject project) {
        return core.getAvailableGroups().stream()
                .filter(group -> !group.getRolesWithScope(RoleScope.projectScope(project.getProjectId())).isEmpty())
                .filter(group -> ServerAuthUtil.canAddToRemoveFromGroup(user, group))
                .collect(toList());
    }

    @NotNull
    private List<Role> getAvailableRoles(@NotNull AuthorityHolder currentUser, @NotNull SProject project) {
        return core.getAvailableRoles().stream()
                .filter(Role::isProjectAssociationSupported)
                .filter(role -> ServerAuthUtil.canChangeUserOrGroupRole(currentUser, RoleScope.projectScope(project.getProjectId()), role))
                .collect(toList());
    }

    @Override
    public void validate(@NotNull HttpServletRequest request, @NotNull SProject project, @NotNull ActionErrors errors) {
        super.validate(request, project, errors);
        if (StringUtil.isEmptyOrSpaces(request.getParameter("role")) && StringUtil.isEmptyOrSpaces(request.getParameter("group"))) {
            errors.addError(new InvalidProperty("role", "Either the role or the group must be specified"));
            errors.addError(new InvalidProperty("group", "Either the role or the group must be specified"));
        }
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
        return !getAvailableRoles(authorityHolder, project).isEmpty() || !getAvailableGroups(authorityHolder, project).isEmpty();
    }

    public final class InvitationImpl extends AbstractInvitation {
        @Nullable
        private final String roleId;
        @Nullable
        private final String groupKey;

        InvitationImpl(@NotNull SUser currentUser, @NotNull String name, @NotNull String token, @NotNull SProject project, @Nullable String roleId,
                       @Nullable String groupKey, boolean multi, @NotNull String welcomeText) {
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
            Role role = getRole();
            return (role == null || getAvailableRoles(user, project).contains(role))
                    && (group == null || getAvailableGroups(user, project).contains(group));
        }

        @Nullable
        @Override
        public String getValidationError() {
            String result = "";
            if (roleId != null && JoinProjectInvitationType.this.core.findRoleById(roleId) == null) {
                result += "Role '" + roleId + "' doesn't exists anymore";
            }
            if (groupKey != null && JoinProjectInvitationType.this.core.findGroup(groupKey) == null) {
                if (!result.isEmpty()) result += "; ";
                result += "Group '" + groupKey + "' doesn't exists anymore";
            }
            return result.isEmpty() ? null : result;
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

                    return project;
                });

                invitationWorkflowFinished(this);

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

        @Nullable
        public String getRoleId() {
            return roleId;
        }

        @Nullable
        public String getGroupKey() {
            return groupKey;
        }

        @NotNull
        @Override
        public String describe(boolean verbose) {
            return "'join " + project.describe(false) + ", " +
                    "role: " + (getRole() != null ? getRole().describe(false) : " <empty>") +
                    ", group: " + (getGroup() != null ? getGroup().describe(false) : " <empty>") + "'";
        }
    }
}
