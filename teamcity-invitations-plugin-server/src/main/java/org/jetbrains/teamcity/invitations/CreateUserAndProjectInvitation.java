package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class CreateUserAndProjectInvitation implements Invitation {
    private final String token;
    private final String registrationUrl;
    private final String parentProjectExternalId;
    private final String roleId;
    private volatile TeamCityCoreFacade teamCityCore;

    CreateUserAndProjectInvitation(String token, String registrationUrl, String parentProjectExternalId, String roleId) {
        this.token = token;
        this.registrationUrl = registrationUrl;
        this.parentProjectExternalId = parentProjectExternalId;
        this.roleId = roleId;
    }

    static CreateUserAndProjectInvitation from(Element element) {
        return new CreateUserAndProjectInvitation(element.getAttributeValue("token"),
                element.getAttributeValue("registrationUrl"),
                element.getAttributeValue("parentExtId"),
                element.getAttributeValue("role"));
    }

    public void setTeamCityCore(TeamCityCoreFacade teamCityCore) {
        this.teamCityCore = teamCityCore;
    }

    @NotNull
    @Override
    public ModelAndView processInvitationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        return new ModelAndView(new RedirectView(registrationUrl));
    }

    @NotNull
    public ModelAndView userRegistered(@NotNull SUser user, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        try {
            SProject createdProject = teamCityCore.createProjectAsSystem(parentProjectExternalId, user.getUsername() + " project");
            Role role = teamCityCore.findRoleById(roleId);
            teamCityCore.addRoleAsSystem(user, role, createdProject);
            Loggers.SERVER.info("User " + user.describe(false) + " registered on invitation '" + token + "'. " +
                    "Project " + createdProject.describe(false) + " created, user got the role " + role.describe(false));
            return new ModelAndView(new RedirectView(teamCityCore.getEditProjectPageUrl(createdProject.getExternalId()), true));
        } catch (Exception e) {
            Loggers.SERVER.warn("Failed to create project for the invited user " + user.describe(false), e);
            return new ModelAndView(new RedirectView("/", true));
        }
    }

    @NotNull
    @Override
    public String getToken() {
        return token;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Register and create own project invitation";
    }

    @Override
    public boolean isMultiUser() {
        return true;
    }

    public void writeTo(@NotNull Element element) {
        element.setAttribute("registrationUrl", registrationUrl);
        element.setAttribute("token", token);
        element.setAttribute("parentExtId", parentProjectExternalId);
        element.setAttribute("roleId", roleId);
    }
}
