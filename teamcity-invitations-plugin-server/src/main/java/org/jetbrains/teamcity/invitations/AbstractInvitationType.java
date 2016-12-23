package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;

public abstract class AbstractInvitationType<T extends Invitation> implements InvitationType<T> {

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
