/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
