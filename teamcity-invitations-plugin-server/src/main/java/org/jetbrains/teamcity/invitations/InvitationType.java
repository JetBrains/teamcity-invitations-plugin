package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
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
    ModelAndView getEditPropertiesView(@Nullable T invitation);

    @NotNull
    T createNewInvitation(@NotNull HttpServletRequest request, @NotNull SProject project, @NotNull String token);

    @NotNull
    T readFrom(@NotNull Map<String, String> params, @NotNull SProject project);

    boolean isAvailableFor(AuthorityHolder authorityHolder, @NotNull SProject project);
}
