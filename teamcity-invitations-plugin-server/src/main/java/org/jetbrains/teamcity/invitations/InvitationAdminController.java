package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.ActionMessages;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.SimpleView;
import jetbrains.buildServer.controllers.admin.projects.EditProjectTab;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class InvitationAdminController extends BaseFormXmlController {

    public static final String MESSAGES_KEY = "teamcity.invitations.plugin";

    @NotNull
    private final InvitationsStorage invitations;
    @NotNull
    private final TeamCityCoreFacade teamCityCoreFacade;
    @NotNull
    private final InvitationsLandingController invitationsController;
    @NotNull
    private final List<InvitationType> invitationTypes;

    public InvitationAdminController(@NotNull PagePlaces pagePlaces,
                                     @NotNull WebControllerManager webControllerManager,
                                     @NotNull PluginDescriptor pluginDescriptor,
                                     @NotNull InvitationsStorage invitations,
                                     @NotNull TeamCityCoreFacade teamCityCoreFacade,
                                     @NotNull InvitationsLandingController invitationsController,
                                     @NotNull List<InvitationType> invitationTypes) {
        this.invitations = invitations;
        this.teamCityCoreFacade = teamCityCoreFacade;
        this.invitationsController = invitationsController;
        this.invitationTypes = invitationTypes;
        new InvitationsProjectAdminPage(pagePlaces, pluginDescriptor).register();
        webControllerManager.registerController("/admin/invitations.html", this);
    }

    @Override
    protected ModelAndView doGet(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) {
        SUser currentUser = SessionUser.getUser(request);
        SProject project = teamCityCoreFacade.findProjectByExtId(request.getParameter("projectId"));
        if (project == null) {
            Loggers.SERVER.warn("Unrecognized invitation request (missing project): " + WebUtil.getRequestDump(request));
            return SimpleView.createTextView("Project not found");
        }

        ModelAndView result;
        if (StringUtil.isEmptyOrSpaces(request.getParameter("token"))) {
            //return 'new invitation' view
            InvitationType<?> invitationType = findInvitationType(request);
            if (invitationType == null) {
                Loggers.SERVER.warn("Unrecognized invitation request (missing type): " + WebUtil.getRequestDump(request));
                return SimpleView.createTextView("Invitation type not found");
            }

            if (!invitationType.isAvailableFor(currentUser, project)) {
                throw new AccessDeniedException(currentUser, "You don't have permissions to create invitation of type '" + invitationType.getDescription() + "'"
                        + " in the project " + project.describe(false));
            }

            result = invitationType.getEditPropertiesView(currentUser, project, null);
        } else {
            //return 'edit invitation' view
            String token = request.getParameter("token");

            Invitation found = invitations.getInvitation(token);
            if (found == null) {
                Loggers.SERVER.warn("Unrecognized invitation request (not found invitation): " + WebUtil.getRequestDump(request));
                return SimpleView.createTextView("Invitation not found");
            }

            if (!found.isAvailableFor(currentUser)) {
                throw new AccessDeniedException(currentUser, "You don't have permissions to edit invitation " + found.getToken());
            }

            result = found.getType().getEditPropertiesView(currentUser, project, found);
        }

        result.addObject("project", project);
        return result;
    }

    @Override
    protected void doPost(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response, @NotNull final Element xmlResponse) {
        try {
            SProject project = teamCityCoreFacade.findProjectByExtId(request.getParameter("projectId"));
            if (project == null) {
                throw new ValidationException("projectId", "Project must be specified");
            }

            String token = request.getParameter("token");

            if (request.getParameter("saveInvitation") != null) {
                if (StringUtil.isEmptyOrSpaces(token)) {
                    //new
                    token = StringUtil.generateUniqueHash();
                    Invitation invitation = createFromRequest(token, project, request);
                    invitations.addInvitation(invitation);
                    xmlResponse.setAttribute("token", invitation.getToken());
                    xmlResponse.setAttribute("token", invitation.getToken());
                    ActionMessages.getOrCreateMessages(request).addRawMessage(MESSAGES_KEY,
                            "Invitation created. Copy and send the following link to the user you want to invite: " +
                                    "<span id=\"justCreatedInvitation\">" + invitationsController.getInvitationsPath() + "?token=" + invitation.getToken() + "<span/> " +
                                    "<span class=\"clipboard-btn tc-icon icon16 tc-icon_copy\" data-clipboard-action=\"copy\"data-clipboard-target=\"#justCreatedInvitation\"></span>");
                } else {
                    //edit
                    Invitation invitation = createFromRequest(token, project, request);
                    invitations.removeInvitation(project, token);
                    invitations.addInvitation(invitation);
                    ActionMessages.getOrCreateMessages(request).addMessage(MESSAGES_KEY, "Invitation '" + invitation.getName() + "' updated.");
                }

            } else if (request.getParameter("removeInvitation") != null && token != null) {
                //delete
                Invitation invitation = invitations.getInvitation(token);
                if (invitation != null && !invitation.isAvailableFor(SessionUser.getUser(request))) {
                    throw new AccessDeniedException(SessionUser.getUser(request), "You don't have permissions to remove invitation " + token);
                }
                Invitation deleted = invitations.removeInvitation(project, token);
                if (deleted != null) {
                    ActionMessages.getOrCreateMessages(request).addMessage(MESSAGES_KEY, "Invitation '" + deleted.getName() + "' removed.");
                } else {
                    ActionMessages.getOrCreateMessages(request).addMessage(MESSAGES_KEY, "Invitation '" + token + "' doesn't exist.");
                }
            } else {
                Loggers.SERVER.warn("Unrecognized invitation request: " + WebUtil.getRequestDump(request));
            }
        } catch (ValidationException e) {
            e.getActionErrors().serialize(xmlResponse);
        }
    }

    @NotNull
    private Invitation createFromRequest(@NotNull String token, @NotNull SProject project, @NotNull HttpServletRequest request) throws ValidationException {
        ActionErrors actionErrors = new ActionErrors();
        InvitationType invitationType = findInvitationType(request);
        if (invitationType == null) {
            throw new ValidationException("invitationType", "Invitation type must be specified");
        }
        invitationType.validate(request, project, actionErrors);
        if (actionErrors.hasErrors()) {
            throw new ValidationException(actionErrors);
        }

        return invitationType.createNewInvitation(request, project, token);
    }

    @Nullable
    private InvitationType findInvitationType(@NotNull HttpServletRequest request) {
        return invitationTypes.stream().filter(type -> type.getId().equals(request.getParameter("invitationType"))).findFirst().orElse(null);
    }

    public class InvitationsProjectAdminPage extends EditProjectTab {

        InvitationsProjectAdminPage(PagePlaces pagePlaces, PluginDescriptor pluginDescriptor) {
            super(pagePlaces, "invitations", pluginDescriptor.getPluginResourcesPath("invitationsProjectAdmin.jsp"), "Invitations");
        }

        @Override
        public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
            SProject project = getProject(request);
            model.put("project", project);
            model.put("invitations", invitations.getInvitations(project).stream()
                    .filter(i -> i.isAvailableFor(SessionUser.getUser(request)))
                    .collect(toList()));
            model.put("invitationTypes", invitationTypes.stream()
                    .filter(invitationType -> invitationType.isAvailableFor(SessionUser.getUser(request), project))
                    .collect(toList()));
            model.put("invitationRootUrl", invitationsController.getInvitationsPath());
        }

        @Override
        public boolean isAvailable(@NotNull HttpServletRequest request) {
            return super.isAvailable(request) && invitationTypes.stream().anyMatch(type -> type.isAvailableFor(SessionUser.getUser(request), getProject(request)));
        }

        @NotNull
        @Override
        public String getTabTitle(@NotNull final HttpServletRequest request) {
            SProject project = getProject(request);
            String tabTitle = super.getTabTitle(request);
            if (project != null) {
                int invitationsCount = invitations.getInvitationsCount(project);
                if (invitationsCount > 0) {
                    tabTitle += " (" + invitationsCount + ")";
                }
            }
            return tabTitle;
        }
    }
}