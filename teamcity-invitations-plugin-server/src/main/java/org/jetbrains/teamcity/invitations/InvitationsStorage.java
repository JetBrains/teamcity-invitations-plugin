package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

@ThreadSafe
public class InvitationsStorage {

    private static final String PROJECT_FEATURE_TYPE = "Invitation";
    private static final String INVITATION_TYPE = "invitationType";

    private final TeamCityCoreFacade teamCityCore;
    private final Map<String, InvitationType> invitationTypes;

    public InvitationsStorage(@NotNull TeamCityCoreFacade teamCityCore,
                              @NotNull List<InvitationType> invitationTypes) {
        this.teamCityCore = teamCityCore;
        this.invitationTypes = invitationTypes.stream().collect(Collectors.toMap(InvitationType::getId, identity()));
    }

    public synchronized Invitation addInvitation(@NotNull String token, @NotNull Invitation invitation) {
        Map<String, String> params = invitation.asMap();
        params.put(INVITATION_TYPE, invitation.getType().getId());
        invitation.getProject().addFeature(PROJECT_FEATURE_TYPE, params);
        teamCityCore.persist(invitation.getProject(), "Invitation added");
        Loggers.SERVER.info("User invitation with token " + token + " created in the project " + invitation.getProject().describe(false));
        return invitation;
    }

    @Nullable
    public synchronized Invitation getInvitation(@NotNull String token) {
        return teamCityCore.runAsSystem(() -> {
            for (SProject project : teamCityCore.getActiveProjects()) {
                for (SProjectFeatureDescriptor feature : project.getOwnFeaturesOfType(PROJECT_FEATURE_TYPE)) {
                    if (feature.getParameters().get("token").equals(token)) {
                        return fromProjectFeature(project, feature);
                    }
                }
            }
            return null;
        });
    }

    @NotNull
    public synchronized List<Invitation> getInvitations(@NotNull SProject project) {
        return project.getOwnFeaturesOfType(PROJECT_FEATURE_TYPE).stream().map(feature -> fromProjectFeature(project, feature)).collect(toList());
    }

    public synchronized boolean removeInvitation(@NotNull SProject project, @NotNull String token) {
        Optional<SProjectFeatureDescriptor> featureDescriptor = project.getOwnFeaturesOfType(PROJECT_FEATURE_TYPE).stream()
                .filter(feature -> feature.getParameters().get("token").equals(token))
                .findFirst();

        if (featureDescriptor.isPresent()) {
            project.removeFeature(featureDescriptor.get().getId());
            teamCityCore.persist(project, "Invitation removed");
            return true;
        } else {
            return false;
        }
    }

    private Invitation fromProjectFeature(SProject project, SProjectFeatureDescriptor feature) {
        InvitationType invitationType = invitationTypes.get(feature.getParameters().get(INVITATION_TYPE));
        return invitationType.readFrom(feature.getParameters(), project);
    }
}
