package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.*;
import jetbrains.buildServer.serverSide.impl.auth.ServerAuthUtil;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class CreateNewProjectInvitationType extends AbstractInvitationType<CreateNewProjectInvitationType.InvitationImpl> implements InvitationType<CreateNewProjectInvitationType.InvitationImpl> {

    @NotNull
    private final TeamCityCoreFacade core;

    @NotNull
    private final List<ProcessingInvitation> myProcessingInvitations = new CopyOnWriteArrayList<>();

    public CreateNewProjectInvitationType(@NotNull TeamCityCoreFacade core,
                                          @NotNull EventDispatcher<ProjectsModelListener> events) {
        this.core = core;
        events.addListener(new ProjectsModelListenerAdapter() {
            @Override
            public void projectCreated(@NotNull String projectId, @Nullable SUser user) {
                SProject created = core.findProjectByIntId(projectId);
                if (created != null && user != null) {
                    Optional<ProcessingInvitation> processingInvitation = myProcessingInvitations.stream().filter(i -> i.isOurProjectCreation(created, user)).findFirst();
                    if (processingInvitation.isPresent()) {
                        core.addRole(user, processingInvitation.get().invitation.getRole(), projectId);
                        processingInvitation.get().dispose();
                        myProcessingInvitations.removeIf(i -> i.isOurProjectCreation(created, user));
                        Loggers.ACTIVITIES.info("User " + user.describe(false) + " creates " + created.describe(false) + " project using the invitation " + processingInvitation.get().invitation.describe(false) + "");
                    }
                }
            }
        });
    }

    @NotNull
    @Override
    public String getId() {
        return "newProjectInvitation";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Create subproject";
    }

    @NotNull
    @Override
    public String getDescriptionViewPath() {
        return core.getPluginResourcesPath("createNewProjectInvitationDescription.jsp");
    }

    @NotNull
    public InvitationImpl readFrom(@NotNull Map<String, String> params, @NotNull SProject project) {
        return new InvitationImpl(params, project);
    }

    @Override
    public boolean isAvailableFor(@NotNull AuthorityHolder authorityHolder, @NotNull SProject project) {
        if (getAvailableRoles(authorityHolder, project).isEmpty()) {
            return false;
        }
        return core.runAsSystem(() ->
                        authorityHolder.isPermissionGrantedForProject(project.getProjectId(), Permission.CREATE_SUB_PROJECT)
        );
    }

    @NotNull
    @Override
    public ModelAndView getEditPropertiesView(@NotNull SUser user, @NotNull SProject project, @Nullable InvitationImpl invitation) {
        ModelAndView modelAndView = new ModelAndView(core.getPluginResourcesPath("createNewProjectInvitationProperties.jsp"));
        List<Role> availableRoles = getAvailableRoles(user, project);
        modelAndView.getModel().put("roles", availableRoles);
        modelAndView.getModel().put("name", invitation == null ? getDescription() : invitation.getName());
        modelAndView.getModel().put("multiuser", invitation == null ? "true" : invitation.multi);
        modelAndView.getModel().put("roleId", invitation == null ? (availableRoles.size() > 0 ? availableRoles.get(0) : null) : invitation.roleId);
        modelAndView.getModel().put("welcomeText", invitation == null ?
                user.getDescriptiveName() + " invites you to join TeamCity and create a project under " + project.getFullName() :
                invitation.welcomeText);

        return modelAndView;
    }

    @NotNull
    private List<Role> getAvailableRoles(@NotNull AuthorityHolder currentUser, @NotNull SProject project) {
        return core.getAvailableRoles().
                stream().
                filter(Role::isProjectAssociationSupported).
                filter(role -> role.getPermissions().contains(Permission.EDIT_PROJECT)).
                filter(role -> canAssignRole(currentUser, project, role)).
                collect(toList());
    }

    private boolean canAssignRole(@NotNull AuthorityHolder currentUser, @NotNull SProject project, @NotNull Role role) {
        return ServerAuthUtil.canChangeUserOrGroupRole(currentUser, RoleScope.projectScope(project.getProjectId()), role);
    }

    @Override
    public void validate(@NotNull HttpServletRequest request, @NotNull SProject project, @NotNull ActionErrors errors) {
        super.validate(request, project, errors);
        if (StringUtil.isEmptyOrSpaces(request.getParameter("role"))) {
            errors.addError(new InvalidProperty("role", "Role must not be empty"));
        }

        if (getAvailableRoles(core.getLoggedInUser(), project).stream().noneMatch(role -> role.getId().equals(request.getParameter("role")))) {
            errors.addError(new InvalidProperty("role", "Role must is inaccessible"));
        }
    }

    @NotNull
    @Override
    public InvitationImpl createNewInvitation(@NotNull HttpServletRequest request, @NotNull SProject project, @NotNull String token) {
        String name = request.getParameter("name");
        String roleId = request.getParameter("role");
        String welcomeText = StringUtil.emptyIfNull(request.getParameter("welcomeText"));
        boolean multiuser = Boolean.parseBoolean(request.getParameter("multiuser"));
        SUser currentUser = SessionUser.getUser(request);
        InvitationImpl invitation = new InvitationImpl(currentUser, name, token, project, roleId, multiuser, welcomeText);
        if (!invitation.isAvailableFor(currentUser)) {
            throw new AccessDeniedException(currentUser, "You don't have permissions to create the invitation");
        }
        return invitation;
    }

    private static final class ProcessingInvitation {
        @NotNull
        private final SUser user;
        @NotNull
        private final InvitationImpl invitation;
        @NotNull
        private final Runnable disposeAction;

        private ProcessingInvitation(@NotNull SUser user, @NotNull InvitationImpl invitation, @NotNull Runnable disposeAction) {
            this.user = user;
            this.invitation = invitation;
            this.disposeAction = disposeAction;
        }

        public boolean isOurProjectCreation(@NotNull SProject created, @NotNull SUser creator) {
            return creator.getId() == this.user.getId() && invitation.getProject().getProjectId().equals(created.getParentProjectId());
        }

        public void dispose() {
            disposeAction.run();
        }
    }

    public final class InvitationImpl extends AbstractInvitation {
        @NotNull
        private final String roleId;

        InvitationImpl(@NotNull SUser currentUser, @NotNull String name, @NotNull String token, @NotNull SProject project, @NotNull String roleId,
                       boolean multi, @NotNull String welcomeText) {
            super(project, name, token, multi, CreateNewProjectInvitationType.this, currentUser.getId(), welcomeText);
            this.roleId = roleId;
        }

        InvitationImpl(@NotNull Map<String, String> params, @NotNull SProject project) {
            super(params, project, CreateNewProjectInvitationType.this);
            this.roleId = params.get("roleId");
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
            result.put("roleId", roleId);
            return result;
        }

        @Override
        public boolean isAvailableFor(@NotNull AuthorityHolder user) {
            Role role = getRole();
            return user.isPermissionGrantedForProject(getProject().getProjectId(), Permission.CREATE_SUB_PROJECT) &&
                    role != null && canAssignRole(user, project, role);
        }

        @NotNull
        public ModelAndView invitationAccepted(@NotNull SUser user, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
            UserEx originalUser = (UserEx) SessionUser.getUser(request);

            Map<String, List<Permission>> additionalPermissions = new HashMap<>();
            getProject().getProjectPath().forEach(parent -> {
                additionalPermissions.put(parent.getProjectId(), Collections.singletonList(Permission.VIEW_PROJECT));
            });
            additionalPermissions.put(project.getProjectId(), asList(Permission.CREATE_SUB_PROJECT, Permission.VIEW_PROJECT));

            AdditionalPermissionsUserWrapper wrappedUser = new AdditionalPermissionsUserWrapper(originalUser, additionalPermissions);
            SessionUser.setUser(request, wrappedUser);
            myProcessingInvitations.add(new ProcessingInvitation(originalUser, this, wrappedUser::disable));
            return new ModelAndView(new RedirectView(new RelativeWebLinks().getCreateProjectPageUrl(project.getExternalId()), true));
        }

        @Nullable
        public SUser getUser() {
            return CreateNewProjectInvitationType.this.core.getUser(createdByUserId);
        }

        @Nullable
        public Role getRole() {
            return CreateNewProjectInvitationType.this.core.findRoleById(roleId);
        }

        @NotNull
        @Override
        public String describe(boolean verbose) {
            return "'create project under " + project.describe(false) + ", role: " + getRole().describe(false) + "'";
        }
    }
}
