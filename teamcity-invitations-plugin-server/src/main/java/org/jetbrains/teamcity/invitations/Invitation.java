package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface Invitation {

    @NotNull
    String getName();

    @NotNull
    String getToken();

    @NotNull
    SProject getProject();

    @NotNull
    ModelAndView processInvitationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response);

    @NotNull
    ModelAndView userRegistered(@NotNull SUser user, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response);

    @NotNull
    InvitationType getType();

    Map<String, String> asMap();

    boolean isReusable();

    /**
     * Check whether the user can view and edit the invitation.
     */
    boolean isAvailableFor(@NotNull AuthorityHolder user);
}
