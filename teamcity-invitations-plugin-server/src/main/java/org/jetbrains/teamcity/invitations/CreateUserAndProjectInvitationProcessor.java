package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static jetbrains.buildServer.serverSide.auth.RoleScope.projectScope;

class CreateUserAndProjectInvitationProcessor implements InvitationProcessor {
    private final ProjectManager projectManager;
    private final Role role;
    private final RelativeWebLinks webLinks;
    private final SecurityContextEx securityContext;

    public CreateUserAndProjectInvitationProcessor(ProjectManager projectManager, Role role, RelativeWebLinks webLinks, SecurityContextEx securityContext) {
        this.projectManager = projectManager;
        this.role = role;
        this.webLinks = webLinks;
        this.securityContext = securityContext;
    }

    @Nullable
    @Override
    public ModelAndView processInvitationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        request.getSession().setAttribute(Invitations.TOKEN_SESSION_ATTR, this);
        return new ModelAndView(new RedirectView("/registerUser.html"));
    }

    @Override
    public boolean userRegistered(@NotNull SUser user, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws IOException {
        try {
            securityContext.runAsSystem(() -> {
                SProject project = projectManager.createProject(user.getUsername() + " project");
                user.addRole(projectScope(project.getProjectId()), role);
                Loggers.SERVER.info("Creating project for the invited user " + user.describe(false) + ": " + project.describe(false) +
                        " Giving " + role.describe(false) + " role to user in the project.");
                response.sendRedirect(webLinks.getEditProjectPageUrl(project.getExternalId()));
            });
            return false;
        } catch (Throwable throwable) {
            Loggers.SERVER.warn("Failed to create project for the invited user", throwable);
            return true;
        }
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
}
