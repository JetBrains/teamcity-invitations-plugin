package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

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
    T createNewInvitation(HttpServletRequest request, String token);

    @NotNull
    T readFrom(@NotNull Element element);

    boolean isAvailableFor(AuthorityHolder authorityHolder);
}
