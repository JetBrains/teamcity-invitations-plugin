package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.Used;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.DuplicateProjectNameException;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Invitation {
    private final String token;
    private final String registrationUrl;
    private final String afterRegistrationUrl;
    private final String parentProjectExternalId;
    private final String roleId;
    private final boolean multi;
    private volatile TeamCityCoreFacade teamCityCore;
    private volatile Role role;
    private volatile SProject parentProject;

    Invitation(String token, String registrationUrl, String afterRegistrationUrl, String parentProjectExternalId, String roleId, boolean multi) {
        this.token = token;
        this.registrationUrl = registrationUrl;
        this.afterRegistrationUrl = afterRegistrationUrl;
        this.parentProjectExternalId = parentProjectExternalId;
        this.roleId = roleId;
        this.multi = multi;
    }

    static Invitation from(Element element) {
        return new Invitation(element.getAttributeValue("token"),
                element.getAttributeValue("registrationUrl"),
                element.getAttributeValue("afterRegistrationUrl"),
                element.getAttributeValue("parentExtId"),
                element.getAttributeValue("roleId"),
                Boolean.valueOf(element.getAttributeValue("multi")));
    }

    void setTeamCityCore(TeamCityCoreFacade teamCityCore) {
        this.teamCityCore = teamCityCore;
        this.role = teamCityCore.findRoleById(roleId);
        this.parentProject = teamCityCore.findProjectByExtId(parentProjectExternalId);
    }

    @Used("jsp")
    public String getRegistrationUrl() {
        return registrationUrl;
    }

    @Used("jsp")
    public String getAfterRegistrationUrl() {
        return afterRegistrationUrl;
    }

    @Used("jsp")
    public String getParentProjectExternalId() {
        return parentProjectExternalId;
    }

    @Used("jsp")
    public boolean isMultiUser() {
        return multi;
    }


    public SProject getParentProject() {
        return parentProject;
    }

    public Role getRole() {
        return role;
    }

    @NotNull
    public String getToken() {
        return token;
    }

    @NotNull
    public ModelAndView processInvitationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        return new ModelAndView(new RedirectView(registrationUrl));
    }

    @NotNull
    public ModelAndView userRegistered(@NotNull SUser user, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        try {
            SProject createdProject = null;
            String projectName = user.getUsername();
            int i = 1;
            while (createdProject == null) {
                try {
                    createdProject = teamCityCore.createProjectAsSystem(parentProjectExternalId, projectName);
                } catch (DuplicateProjectNameException e) {
                    projectName = user.getUsername() + i++;
                }
            }
            Role role = teamCityCore.findRoleById(roleId);
            teamCityCore.addRoleAsSystem(user, role, createdProject);
            Loggers.SERVER.info("User " + user.describe(false) + " registered on invitation '" + token + "'. " +
                    "Project " + createdProject.describe(false) + " created, user got the role " + role.describe(false));
            String afterRegistrationUrlFinal = afterRegistrationUrl.replace("{projectExtId}", createdProject.getExternalId());
            return new ModelAndView(new RedirectView(afterRegistrationUrlFinal, true));
        } catch (Exception e) {
            Loggers.SERVER.warn("Failed to create project for the invited user " + user.describe(false), e);
            return new ModelAndView(new RedirectView("/", true));
        }
    }

    public void writeTo(@NotNull Element element) {
        element.setAttribute("registrationUrl", registrationUrl);
        element.setAttribute("afterRegistrationUrl", afterRegistrationUrl);
        element.setAttribute("token", token);
        element.setAttribute("parentExtId", parentProjectExternalId);
        element.setAttribute("roleId", roleId);
        element.setAttribute("multi", String.valueOf(multi));
    }
}
