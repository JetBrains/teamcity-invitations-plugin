package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.users.SUser;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Invitation extends InvitationDescription {

    @NotNull
    ModelAndView processInvitationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response);

    @NotNull
    ModelAndView userRegistered(@NotNull SUser user, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response);

    void writeTo(@NotNull Element element);
}
