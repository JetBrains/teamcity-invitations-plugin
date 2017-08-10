package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class InvitationLandingProvider {
    private final TeamCityCoreFacade core;
    private final CopyOnWriteArrayList<Function<Invitation, String>> customProviders = new CopyOnWriteArrayList<>();

    public InvitationLandingProvider(TeamCityCoreFacade core) {
        this.core = core;
    }

    public void registerCustomProvider(@NotNull Function<Invitation, String> provider) {
        customProviders.add(provider);
    }

    @NotNull
    public String getLanding(@NotNull Invitation invitation) {
        for (Function<Invitation, String> provider : customProviders) {
            String landing = core.runAsSystem(() -> provider.apply(invitation));
            if (landing != null) {
                Loggers.SERVER.info("Custom invitation landing found for the invitation " + invitation.describe(false) + ": " + landing);
                return landing;
            }
        }
        return core.getPluginResourcesPath("invitationLanding.jsp");
    }
}
