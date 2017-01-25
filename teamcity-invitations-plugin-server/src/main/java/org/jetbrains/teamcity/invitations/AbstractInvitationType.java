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

    protected AbstractInvitationType(InvitationsStorage invitationsStorage, TeamCityCoreFacade core) {
        this.invitationsStorage = invitationsStorage;
        this.core = core;
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
}
