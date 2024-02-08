

package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.web.impl.TeamCityInternalKeys;
import jetbrains.buildServer.web.invitations.InvitationsRegistry;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

public class InvitationsLandingController extends BaseController {
    public static final String INVITATIONS_PATH = "/invitations.html";

    private static final String TOKEN_URL_PARAM = "token";

    @NotNull
    private final InvitationsStorage invitations;

    @NotNull
    private final TeamCityCoreFacade core;

    @NotNull
    private final RootUrlHolder rootUrlHolder;


    public InvitationsLandingController(@NotNull WebControllerManager webControllerManager,
                                        @NotNull InvitationsStorage invitations,
                                        @NotNull AuthorizationInterceptor authorizationInterceptor,
                                        @NotNull TeamCityCoreFacade core, @NotNull RootUrlHolder rootUrlHolder,
                                        @NotNull InvitationsRegistry invitationRegistry) {
        this.invitations = invitations;
        this.core = core;
        this.rootUrlHolder = rootUrlHolder;
        webControllerManager.registerController(INVITATIONS_PATH, this);
        authorizationInterceptor.addPathNotRequiringAuth(INVITATIONS_PATH);
        invitationRegistry.registerInvitationsProvider(request -> {
            String token = request.getParameter(TOKEN_URL_PARAM);
            Invitation invitation = core.runAsSystem(() -> invitations.getInvitation(token));
            return invitation != null;
        });
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        String token = request.getParameter(TOKEN_URL_PARAM);
        Invitation invitation = core.runAsSystem(() -> invitations.getInvitation(token));
        if (invitation == null) {
            Loggers.SERVER.warn("Request with unknown invitation token received: " + WebUtil.getRequestDump(request));
            return new ModelAndView(core.getPluginResourcesPath("invitationLanding.jsp"), Collections.singletonMap("title", "Invitation not found"));
        }
        if (invitation.getValidationError() != null) {
            Loggers.SERVER.warn("User tries to accept the invitation '" + token + "' that is invalid: " + invitation.getValidationError());
        }
        request.getSession().setAttribute(TeamCityInternalKeys.FIRST_LOGIN_REDIRECT_URL,
                InvitationsProceedController.PATH + "?token=" + token);
        return invitation.processInvitationRequest(request, response);
    }

    @NotNull
    String getInvitationsPath() {
        return rootUrlHolder.getRootUrl() + InvitationsLandingController.INVITATIONS_PATH;
    }
}