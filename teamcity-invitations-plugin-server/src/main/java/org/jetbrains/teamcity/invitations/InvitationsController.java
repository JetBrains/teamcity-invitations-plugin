package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.web.impl.TeamCityInternalKeys;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InvitationsController extends BaseController {
    private static final String INVITATIONS_PATH = "/invitations.html";
    private static final String AFTER_REGISTRATION_PARAM = "registered";

    private static final String INVITATION_TOKEN_SESSION_ATTR = "teamcity.invitation.token";
    private static final String TOKEN_URL_PARAM = "token";

    @NotNull
    private final InvitationsStorage invitations;

    @NotNull
    private final RootUrlHolder rootUrlHolder;

    public InvitationsController(@NotNull WebControllerManager webControllerManager,
                                 @NotNull InvitationsStorage invitations,
                                 @NotNull AuthorizationInterceptor authorizationInterceptor,
                                 @NotNull RootUrlHolder rootUrlHolder) {
        this.invitations = invitations;
        this.rootUrlHolder = rootUrlHolder;
        webControllerManager.registerController(INVITATIONS_PATH, this);
        authorizationInterceptor.addPathNotRequiringAuth(INVITATIONS_PATH);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        if (request.getParameter(TOKEN_URL_PARAM) != null) {
            return processInvitationRequest(request, response);
        }

        if (request.getParameter(AFTER_REGISTRATION_PARAM) != null) {
            return processUserRegisteredRequest(request, response);
        }

        response.setStatus(404);
        return null;
    }

    @NotNull
    String getInvitationsPath() {
        return rootUrlHolder.getRootUrl() + InvitationsController.INVITATIONS_PATH;
    }

    private ModelAndView processInvitationRequest(HttpServletRequest request, HttpServletResponse response) {
        String token = request.getParameter(TOKEN_URL_PARAM);
        Invitation invitation = invitations.getInvitation(token);
        if (invitation == null) {
            Loggers.SERVER.warn("Request with unknown invitation token received: " + WebUtil.getRequestDump(request));
            return new ModelAndView(new RedirectView("/"));
        }
        request.getSession().setAttribute(INVITATION_TOKEN_SESSION_ATTR, token);
        request.getSession().setAttribute(TeamCityInternalKeys.FIRST_LOGIN_REDIRECT_URL, INVITATIONS_PATH + "?" + AFTER_REGISTRATION_PARAM + "=1");
        return invitation.processInvitationRequest(request, response);
    }

    private ModelAndView processUserRegisteredRequest(HttpServletRequest request, HttpServletResponse response) {
        Object tokenObj = request.getSession().getAttribute(INVITATION_TOKEN_SESSION_ATTR);
        if (tokenObj != null && tokenObj instanceof String) {
            String token = (String) tokenObj;
            Invitation invitation = invitations.getInvitation(token);
            if (invitation == null) {
                Loggers.SERVER.warn("User registered on invitation by token " + token + " but invitation doesn't exist anymore");
                return new ModelAndView(new RedirectView("/"));
            }
            return invitation.userRegistered(SessionUser.getUser(request), request, response);
        } else {
            Loggers.SERVER.warn("User registered on invitation but token doesn't exist in the session:" + WebUtil.getRequestDump(request));
            return new ModelAndView(new RedirectView("/"));
        }
    }
}
