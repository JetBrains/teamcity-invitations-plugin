package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.auth.RolesManager;
import jetbrains.buildServer.serverSide.identifiers.ProjectIdentifiersManager;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Invitations {

    public static final String TOKEN_SESSION_ATTR = "teamcity.invitation.token";

    private final Map<String, InvitationProcessor> myInvitations = new ConcurrentHashMap<>();
    private final ProjectManager projectManager;
    private final ProjectIdentifiersManager projectIdentifiersManager;
    private final RolesManager rolesManager;
    private final RelativeWebLinks webLinks;
    private final SecurityContextEx securityContext;

    public Invitations(ProjectManager projectManager, ProjectIdentifiersManager projectIdentifiersManager, RolesManager rolesManager, RelativeWebLinks webLinks, SecurityContextEx securityContext) {
        this.projectManager = projectManager;
        this.projectIdentifiersManager = projectIdentifiersManager;
        this.rolesManager = rolesManager;
        this.webLinks = webLinks;
        this.securityContext = securityContext;
    }

    @NotNull
    public String createInvitation(@NotNull InvitationProcessor invitationProcessor) {
        String token = StringUtil.generateUniqueHash();
        myInvitations.put(token, invitationProcessor);
        return token;
    }

    public void createUserAndProjectInvitation(@NotNull String registrationUrl, String parentProjectExtId) {
        createInvitation(new CreateUserAndProjectInvitationProcessor(registrationUrl,
                projectManager.findProjectByExternalId(parentProjectExtId), rolesManager.findRoleById("PROJECT_ADMIN"), webLinks, securityContext, projectIdentifiersManager));
    }

    @Nullable
    public InvitationProcessor getInvitation(String token) {
        return myInvitations.get(token);
    }

    @NotNull
    public Map<String, InvitationDescription> getInvitations() {
        return new HashMap<>(myInvitations);
    }
}
