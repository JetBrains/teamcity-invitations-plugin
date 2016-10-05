package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Invitations {

    private final Map<String, InvitationProcessor> myInvitations = new ConcurrentHashMap<>();
    private final TeamCityCoreFacade teamCityCore;

    public Invitations(@NotNull TeamCityCoreFacade teamCityCore) {
        this.teamCityCore = teamCityCore;
    }

    public String createUserAndProjectInvitation(@NotNull String registrationUrl, String parentProjectExtId) {
        String token = StringUtil.generateUniqueHash();
        CreateUserAndProjectInvitationProcessor invitation = new CreateUserAndProjectInvitationProcessor(token, registrationUrl,
                parentProjectExtId, teamCityCore.findRoleById("PROJECT_ADMIN"), teamCityCore);
        myInvitations.put(token, invitation);
        Loggers.SERVER.info("User invitation with token " + token + " created: " + invitation.getDescription());
        return token;
    }

    @Nullable
    public InvitationProcessor getInvitation(@NotNull String token) {
        return myInvitations.get(token);
    }

    @NotNull
    public Map<String, InvitationDescription> getInvitations() {
        return new HashMap<>(myInvitations);
    }
}
