package org.jetbrains.teamcity.invitations;

import org.jetbrains.annotations.NonNls;

public class InvitationException extends RuntimeException {

    public InvitationException(@NonNls String message) {
        super(message);
    }
}
