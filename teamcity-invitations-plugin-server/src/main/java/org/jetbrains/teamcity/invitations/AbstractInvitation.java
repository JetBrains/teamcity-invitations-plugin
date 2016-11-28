package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.web.util.SessionUser;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractInvitation implements Invitation {
    protected final String token;
    protected final boolean multi;
    protected final long createdByUserId;
    private final InvitationType type;
    private final String name;

    protected AbstractInvitation(String name, @NotNull String token, boolean multi, InvitationType type, long createdByUserId) {
        this.token = token;
        this.multi = multi;
        this.type = type;
        this.name = name;
        this.createdByUserId = createdByUserId;
    }

    protected AbstractInvitation(Element element, InvitationType type) {
        this.name = element.getAttributeValue("name");
        this.token = element.getAttributeValue("token");
        this.multi = Boolean.valueOf(element.getAttributeValue("multi"));
        this.createdByUserId = Long.parseLong(element.getAttributeValue("createdByUserId"));
        this.type = type;
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
        modelAndView.addObject("proceedUrl", InvitationsController.computeUserRegisteredUrl());
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

    public void writeTo(@NotNull Element element) {
        element.setAttribute("name", name);
        element.setAttribute("token", token);
        element.setAttribute("multi", multi + "");
        element.setAttribute("createdByUserId", createdByUserId + "");
    }

    @Override
    public boolean isReusable() {
        return multi;
    }
}
