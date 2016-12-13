package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractInvitation implements Invitation {
    protected final String token;
    protected final boolean multi;
    protected final long createdByUserId;
    protected final SProject project;
    private final InvitationType type;
    private final String name;

    protected AbstractInvitation(@NotNull SProject project, String name, @NotNull String token, boolean multi, InvitationType type, long createdByUserId) {
        this.token = token;
        this.multi = multi;
        this.type = type;
        this.name = name;
        this.createdByUserId = createdByUserId;
        this.project = project;
    }

    protected AbstractInvitation(Map<String, String> params, SProject project, InvitationType type) {
        this.name = params.get("name");
        this.token = params.get("token");
        this.multi = Boolean.valueOf(params.get("multi"));
        this.createdByUserId = Long.parseLong(params.get("createdByUserId"));
        this.type = type;
        this.project = project;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public String getToken() {
        return token;
    }

    @NotNull
    @Override
    public ModelAndView processInvitationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView(getLandingPage());
        modelAndView.addObject("loggedInUser", SessionUser.getUser(request));
        modelAndView.addObject("proceedUrl", InvitationsProceedController.PATH);
        modelAndView.addObject("invitation", this);
        return modelAndView;
    }

    @NotNull
    protected abstract String getLandingPage();

    @NotNull
    @Override
    public InvitationType getType() {
        return type;
    }

    @NotNull
    public Map<String, String> asMap() {
        Map<String, String> result = new HashMap<>();
        result.put("name", name);
        result.put("multi", multi + "");
        result.put("createdByUserId", createdByUserId + "");
        result.put("token", token);
        return result;
    }

    @Override
    public boolean isReusable() {
        return multi;
    }

    @NotNull
    @Override
    public SProject getProject() {
        return project;
    }
}
