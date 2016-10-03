package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.auth.RolesManager;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Invitations {

    public static final String TOKEN_SESSION_ATTR = "teamcity.invitation.token";

    private final Map<String, InvitationProcessor> myInvitations = new ConcurrentHashMap<>();

    public Invitations(ProjectManager projectManager, RolesManager rolesManager, RelativeWebLinks webLinks, SecurityContextEx securityContext) {
        //temporal solution - create one invitation
        myInvitations.put("testDrive", new CreateUserAndProjectInvitationProcessor(projectManager,
                rolesManager.findRoleById("PROJECT_ADMIN"), webLinks, securityContext));
    }

    @NotNull
    public String createInvitation(@NotNull InvitationProcessor invitationProcessor) {
        String token = StringUtil.generateUniqueHash();
        myInvitations.put(token, invitationProcessor);
        return token;
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
