

package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.controllers.ActionErrors;
import org.jetbrains.annotations.NotNull;

public class ValidationException extends Exception {

    @NotNull
    private final ActionErrors actionErrors;

    public ValidationException(@NotNull ActionErrors actionErrors) {
        super(actionErrors.toString());
        this.actionErrors = actionErrors;
    }

    public ValidationException(@NotNull String id, @NotNull String message) {
        this(createError(id, message));
    }

    @NotNull
    private static ActionErrors createError(@NotNull String id, @NotNull String message) {
        ActionErrors actionErrors = new ActionErrors();
        actionErrors.addError(id, message);
        return actionErrors;
    }

    @NotNull
    public ActionErrors getActionErrors() {
        return actionErrors;
    }
}