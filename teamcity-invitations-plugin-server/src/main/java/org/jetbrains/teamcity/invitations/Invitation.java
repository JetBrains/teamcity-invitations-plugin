package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.DuplicateProjectNameException;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Invitation {
    @NotNull
    private final String token;
    @NotNull
    private final String registrationUrl;
    @NotNull
    private final String afterRegistrationUrl;
    @NotNull
    private final SProject parentProject;
    private final boolean multi;
    @NotNull
    private final Role role;
    @NotNull
    private final TeamCityCoreFacade teamCityCore;

    Invitation(@NotNull String token, @NotNull String registrationUrl, @NotNull String afterRegistrationUrl, String parentProjectExternalId, String roleId, boolean multi, @NotNull TeamCityCoreFacade core) {
        this.token = token;
        this.registrationUrl = registrationUrl;
        this.afterRegistrationUrl = afterRegistrationUrl;
        this.multi = multi;
        this.teamCityCore = core;
        Role role = core.findRoleById(roleId);
        if (role == null) {
            throw new CreateInvitationException("Unable to create invitation with a non-existing role " + roleId);
        }
        this.role = role;
        SProject parent = core.findProjectByExtId(parentProjectExternalId);
        if (parent == null) {
            throw new CreateInvitationException("Unable to create invitation with a non-existing project " + parentProjectExternalId);
        }
        this.parentProject = parent;
    }

    @Nullable
    static Invitation from(Element element, TeamCityCoreFacade core) {
        try {
            return new Invitation(element.getAttributeValue("token"),
                    element.getAttributeValue("registrationUrl"),
                    element.getAttributeValue("afterRegistrationUrl"),
                    element.getAttributeValue("parentExtId"),
                    element.getAttributeValue("roleId"),
                    Boolean.valueOf(element.getAttributeValue("multi")),
                    core);
        } catch (Exception e) {
            Loggers.SERVER.warnAndDebugDetails("Unable to load invitation from the file", e);
            return null;
        }
    }


    @NotNull
    public String getRegistrationUrl() {
        return registrationUrl;
    }

    @NotNull
    public String getAfterRegistrationUrl() {
        return afterRegistrationUrl;
    }

    public boolean isMultiUser() {
        return multi;
    }

    @NotNull
    public SProject getParentProject() {
        return parentProject;
    }

    @NotNull
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
                    createdProject = teamCityCore.createProjectAsSystem(parentProject.getExternalId(), projectName);
                } catch (DuplicateProjectNameException e) {
                    projectName = user.getUsername() + i++;
                }
            }
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
        element.setAttribute("parentExtId", parentProject.getExternalId());
        element.setAttribute("roleId", role.getId());
        element.setAttribute("multi", String.valueOf(multi));
    }
}
