package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InvitationsController extends BaseController {
    private final Invitations invitations;

    public InvitationsController(WebControllerManager webControllerManager, Invitations invitations, AuthorizationInterceptor authorizationInterceptor) {
        this.invitations = invitations;
        webControllerManager.registerController("/invitations.html", this);
        authorizationInterceptor.addPathNotRequiringAuth("/invitations.html");
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        String token = request.getParameter("token");
        if (StringUtil.isEmpty(token)) {
            response.setStatus(404);
            return null;
        }

        InvitationProcessor invitation = invitations.getInvitation(token);
        if (invitation == null) {
            Loggers.SERVER.warn("Invitation with token " + token + " not found");
            response.setStatus(404);
            return null;
        }

        return invitation.processInvitationRequest(request, response);
    }
}
