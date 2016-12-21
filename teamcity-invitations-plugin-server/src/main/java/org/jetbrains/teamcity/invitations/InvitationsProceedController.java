package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InvitationsProceedController extends BaseController {
    static final String PATH = "/invitationsProceed.html";

    private static final String INVITATION_TOKEN_SESSION_ATTR = "teamcity.invitation.token";

    @NotNull
    private final InvitationsStorage invitations;

    @NotNull
    private final TeamCityCoreFacade core;

    public InvitationsProceedController(@NotNull WebControllerManager webControllerManager,
                                        @NotNull InvitationsStorage invitations,
                                        @NotNull TeamCityCoreFacade core) {
        this.invitations = invitations;
        this.core = core;
        webControllerManager.registerController(PATH, this);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        Object tokenObj = request.getSession().getAttribute(INVITATION_TOKEN_SESSION_ATTR);
        if (tokenObj != null && tokenObj instanceof String) {
            String token = (String) tokenObj;
            Invitation invitation = core.runAsSystem(() -> invitations.getInvitation(token));
            if (invitation == null) {
                Loggers.SERVER.warn("User accepted the invitation with token " + token + " but invitation doesn't exist anymore");
                return new ModelAndView(new RedirectView("/"));
            }
            ModelAndView result = invitation.invitationAccepted(SessionUser.getUser(request), request, response);
            if (!invitation.isReusable()) {
                Loggers.SERVER.info("Single user invitation " + token + " was used by user " + SessionUser.getUser(request).describe(false));
                core.runAsSystem(() -> invitations.removeInvitation(invitation.getProject(), token));
            }
            return result;
        } else {
            Loggers.SERVER.warn("User accepted the invitation but token doesn't exist in the session:" + WebUtil.getRequestDump(request));
            return new ModelAndView(new RedirectView("/"));
        }
    }
}
