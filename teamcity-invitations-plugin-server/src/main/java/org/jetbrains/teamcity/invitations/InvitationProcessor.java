package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface InvitationProcessor extends InvitationDescription {

    @Nullable
    ModelAndView processInvitationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response);

    boolean userRegistered(@NotNull SUser user, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response);
}
