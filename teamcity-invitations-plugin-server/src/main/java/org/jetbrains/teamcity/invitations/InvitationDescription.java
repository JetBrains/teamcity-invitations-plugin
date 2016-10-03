package org.jetbrains.teamcity.invitations;

import org.jetbrains.annotations.NotNull;

public interface InvitationDescription {

    @NotNull
    String getDescription();

    boolean isMultiUser();
}
