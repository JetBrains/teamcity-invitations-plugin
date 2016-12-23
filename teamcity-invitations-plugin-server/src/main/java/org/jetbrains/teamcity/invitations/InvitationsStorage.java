package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectsModelListener;
import jetbrains.buildServer.serverSide.ProjectsModelListenerAdapter;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.jetbrains.teamcity.invitations.AbstractInvitation.TOKEN_PARAM_NAME;

@ThreadSafe
public class InvitationsStorage {

    private static final String PROJECT_FEATURE_TYPE = "Invitation";
    private static final String INVITATION_TYPE = "invitationType";

    private final TeamCityCoreFacade teamCityCore;
    private final Map<String, InvitationType> invitationTypes;

    @GuardedBy("this")
    private Map<String, Invitation> myInvitationByTokenCache;

    public InvitationsStorage(@NotNull TeamCityCoreFacade teamCityCore,
                              @NotNull List<InvitationType> invitationTypes,
                              @NotNull EventDispatcher<ProjectsModelListener> events) {
        this.teamCityCore = teamCityCore;
        this.invitationTypes = invitationTypes.stream().collect(toMap(InvitationType::getId, identity()));
        events.addListener(new ProjectsModelListenerAdapter() {
            @Override
            public void projectFeatureAdded(@NotNull SProject project, @NotNull SProjectFeatureDescriptor projectFeature) {
                resetCache();
            }

            @Override
            public void projectFeatureRemoved(@NotNull SProject project, @NotNull SProjectFeatureDescriptor projectFeature) {
                resetCache();
            }

            @Override
            public void projectFeatureChanged(@NotNull SProject project, @NotNull SProjectFeatureDescriptor before, @NotNull SProjectFeatureDescriptor after) {
                resetCache();
            }
        });
    }

    public Invitation addInvitation(@NotNull Invitation invitation) {
        Map<String, String> params = invitation.asMap();
        params.put(INVITATION_TYPE, invitation.getType().getId());
        invitation.getProject().addFeature(PROJECT_FEATURE_TYPE, params);
        teamCityCore.persist(invitation.getProject(), "Invitation added");
        Loggers.SERVER.info("User invitation with token " + invitation.getToken() + " created in the project " + invitation.getProject().describe(false));
        getInvitation(invitation.getToken());//populate cache
        return invitation;
    }

    @NotNull
    public List<Invitation> getInvitations(@NotNull SProject project) {
        return project.getOwnFeaturesOfType(PROJECT_FEATURE_TYPE).stream().map(feature -> fromProjectFeature(project, feature)).collect(toList());
    }

    public int getInvitationsCount(@NotNull SProject project) {
        return project.getOwnFeaturesOfType(PROJECT_FEATURE_TYPE).size();
    }

    public Invitation removeInvitation(@NotNull SProject project, @NotNull String token) {
        Optional<SProjectFeatureDescriptor> featureDescriptor = project.getOwnFeaturesOfType(PROJECT_FEATURE_TYPE).stream()
                .filter(feature -> feature.getParameters().get(TOKEN_PARAM_NAME).equals(token))
                .findFirst();

        if (featureDescriptor.isPresent()) {
            project.removeFeature(featureDescriptor.get().getId());
            teamCityCore.persist(project, "Invitation removed");
            return fromProjectFeature(project, featureDescriptor.get());
        } else {
            return null;
        }
    }

    @Nullable
    public Invitation getInvitation(@NotNull String token) {
        synchronized (this) {
            if (myInvitationByTokenCache == null) {
                myInvitationByTokenCache = new HashMap<>();
                for (SProject project : teamCityCore.getActiveProjects()) {
                    for (SProjectFeatureDescriptor feature : project.getOwnFeaturesOfType(PROJECT_FEATURE_TYPE)) {
                        myInvitationByTokenCache.put(feature.getParameters().get(TOKEN_PARAM_NAME), fromProjectFeature(project, feature));
                    }
                }
            }
            return myInvitationByTokenCache.get(token);
        }
    }

    private synchronized void resetCache() {
        myInvitationByTokenCache = null;
    }

    private Invitation fromProjectFeature(SProject project, SProjectFeatureDescriptor feature) {
        InvitationType invitationType = invitationTypes.get(feature.getParameters().get(INVITATION_TYPE));
        return invitationType.readFrom(feature.getParameters(), project);
    }
}
