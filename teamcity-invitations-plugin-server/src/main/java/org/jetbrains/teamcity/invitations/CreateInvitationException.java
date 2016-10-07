package org.jetbrains.teamcity.invitations;

import org.jetbrains.annotations.NonNls;

public class CreateInvitationException extends RuntimeException {

    public CreateInvitationException(@NonNls String message) {
        super(message);
    }
}
