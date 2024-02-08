

package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface InvitationType<T extends Invitation> {

    @NotNull
    String getId();

    @NotNull
    String getDescription();

    @NotNull
    String getDescriptionViewPath();

    /**
     * @param invitation null if we it's create new invitation view, not null if edit existing invitation
     */
    @NotNull
    ModelAndView getEditPropertiesView(@NotNull SUser user, @NotNull SProject project, @Nullable T invitation);

    void validate(@NotNull HttpServletRequest request, @NotNull SProject project, @NotNull ActionErrors actionErrors);

    @NotNull
    T createNewInvitation(@NotNull HttpServletRequest request, @NotNull SProject project, @NotNull String token);

    @NotNull
    T readFrom(@NotNull Map<String, String> params, @NotNull SProject project);

    boolean isAvailableFor(@NotNull AuthorityHolder authorityHolder, @NotNull SProject project);

    @NotNull
    String getLandingPage(@NotNull Invitation invitation);
}