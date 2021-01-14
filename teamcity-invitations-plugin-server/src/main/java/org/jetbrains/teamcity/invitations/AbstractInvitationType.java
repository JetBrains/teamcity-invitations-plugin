/*
 * Copyright 2000-2021 JetBrains s.r.o.
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
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;

public abstract class AbstractInvitationType<T extends Invitation> implements InvitationType<T> {

    private final InvitationsStorage invitationsStorage;
    private final TeamCityCoreFacade core;
    private final InvitationLandingProvider invitationLandingProvider;

    protected AbstractInvitationType(InvitationsStorage invitationsStorage, TeamCityCoreFacade core, InvitationLandingProvider invitationLandingProvider) {
        this.invitationsStorage = invitationsStorage;
        this.core = core;
        this.invitationLandingProvider = invitationLandingProvider;
        invitationsStorage.registerInvitationType(this);
    }

    protected void invitationWorkflowFinished(@NotNull Invitation invitation) {
        if (!invitation.isReusable()) {
            Loggers.ACTIVITIES.info("Single user invitation " + invitation.describe(false) + " was used and will be deleted");
            core.runAsSystem(() -> invitationsStorage.removeInvitation(invitation.getProject(), invitation.getToken()));
        }
    }

    @Override
    public void validate(@NotNull HttpServletRequest request, @NotNull SProject project, @NotNull ActionErrors errors) {
        if (StringUtil.isEmptyOrSpaces(request.getParameter("name"))) {
            errors.addError(new InvalidProperty("name", "Display name must not be empty"));
        }

        if (StringUtil.isEmptyOrSpaces(request.getParameter("welcomeText"))) {
            errors.addError(new InvalidProperty("welcomeText", "Welcome text must not be empty"));
        }
    }

    @NotNull
    @Override
    public String getLandingPage(Invitation invitation) {
        return invitationLandingProvider.getLanding(invitation);
    }
}
