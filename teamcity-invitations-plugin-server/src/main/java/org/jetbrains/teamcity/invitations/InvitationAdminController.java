package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.admin.AdminPage;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.web.openapi.*;
import jetbrains.buildServer.web.util.WebUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class InvitationAdminController extends BaseFormXmlController {

    @NotNull
    private final WebControllerManager webControllerManager;
    @NotNull
    private final Invitations invitations;
    @NotNull
    private final TeamCityCoreFacade teamCityCoreFacade;
    @NotNull
    private final InvitationsController invitationsController;

    public InvitationAdminController(final PagePlaces pagePlaces,
                                     @NotNull WebControllerManager webControllerManager,
                                     @NotNull RootUrlHolder rootUrlHolder,
                                     @NotNull PluginDescriptor pluginDescriptor,
                                     @NotNull Invitations invitations,
                                     @NotNull TeamCityCoreFacade teamCityCoreFacade,
                                     @NotNull InvitationsController invitationsController) {
        this.webControllerManager = webControllerManager;
        this.invitations = invitations;
        this.teamCityCoreFacade = teamCityCoreFacade;
        this.invitationsController = invitationsController;
        new InvitationsAdminPage(pagePlaces, rootUrlHolder, pluginDescriptor).register();
        webControllerManager.registerController("/admin/invitations.html", this);
        webControllerManager.registerAction(this, new CreateInvitationAction());
    }

    @Override
    protected ModelAndView doGet(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) {
        return null;
    }

    @Override
    protected synchronized void doPost(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response, @NotNull final Element xmlResponse) {
        ControllerAction action = webControllerManager.getAction(this, request);
        if (action == null) {
            Loggers.SERVER.warn("Unrecognized request: " + WebUtil.getRequestDump(request));
        } else {
            action.process(request, response, xmlResponse);

        }
    }

    public class InvitationsAdminPage extends AdminPage {

        private RootUrlHolder rootUrlHolder;

        InvitationsAdminPage(@NotNull PagePlaces pagePlaces,
                             @NotNull RootUrlHolder rootUrlHolder,
                             @NotNull PluginDescriptor pluginDescriptor) {
            super(pagePlaces, "invitations", pluginDescriptor.getPluginResourcesPath("invitationsAdmin.jsp"), "Invitations");
            this.rootUrlHolder = rootUrlHolder;
            setPosition(PositionConstraint.last());
        }

        @Override
        public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
            model.put("invitations", invitations.getInvitations());
            model.put("invitationRootUrl", invitationsController.getInvitationsPath());
            model.put("projects", teamCityCoreFacade.getActiveProjects());
        }

        @NotNull
        @Override
        public String getGroup() {
            return AdminPage.USER_MANAGEMENT_GROUP;
        }
    }

    private class CreateInvitationAction implements ControllerAction {
        @Override
        public boolean canProcess(@NotNull final HttpServletRequest request) {
            return request.getParameter("createInvitation") != null;
        }

        @Override
        public void process(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response, @Nullable final Element ajaxResponse) {
            String registrationUrl = request.getParameter("registrationUrl");
            String parentProjectExtId = request.getParameter("parentProject");
            //TODO validate
            invitations.createUserAndProjectInvitation(registrationUrl, parentProjectExtId);
        }
    }

}