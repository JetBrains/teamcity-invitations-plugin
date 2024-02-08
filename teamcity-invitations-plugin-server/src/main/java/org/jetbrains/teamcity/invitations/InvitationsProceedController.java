

package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.functions.user.UserFunctions;
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
        SUser user = SessionUser.getUser(request);

        Object tokenObj = request.getParameter("token");
        if (tokenObj != null) {
            String token = (String) tokenObj;

            if (UserFunctions.isGuestUser(user)) {
                redirectTo(request.getContextPath() + InvitationsLandingController.INVITATIONS_PATH + "?guestError=1&token=" + token, response);
                return null;
            }

            Invitation invitation = core.runAsSystem(() -> invitations.getInvitation(token));
            if (invitation == null) {
                Loggers.SERVER.warn("User accepted the invitation with token " + token + " but invitation doesn't exist anymore");
                return new ModelAndView(new RedirectView("/"));
            }
            if (!invitation.isEnabled()) {
                Loggers.SERVER.warn("User accepted the invitation with token " + token + " but invitation is disabled");
                return new ModelAndView(new RedirectView("/"));
            }
            if (invitation.getValidationError() != null) {
                Loggers.SERVER.warn("User accepted the invitation with token " + token + " but invitation is invalid: " + invitation.getValidationError());
                return new ModelAndView(new RedirectView("/"));
            }
            ModelAndView result = invitation.invitationAccepted(user, request, response);
            Loggers.ACTIVITIES.info("User " + user.describe(false) + " accepted the invitation " + invitation.describe(true) + ".");
            return result;
        } else {
            Loggers.SERVER.warn("User accepted the invitation with unknown token: " + WebUtil.getRequestDump(request));
            return new ModelAndView(new RedirectView("/"));
        }
    }
}