package org.jetbrains.teamcity.invitations;

import org.jetbrains.annotations.NotNull;

public interface InvitationDescription {

    @NotNull
    String getToken();

    @NotNull
    String getDescription();

    boolean isMultiUser();
}
