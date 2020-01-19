/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;
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
                              @NotNull EventDispatcher<ProjectsModelListener> events) {
        this.teamCityCore = teamCityCore;
        this.invitationTypes = new ConcurrentHashMap<>();
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

    public void registerInvitationType(InvitationType invitationType) {
        this.invitationTypes.put(invitationType.getId(), invitationType);
    }

    public Invitation addInvitation(@NotNull Invitation invitation) {
        Map<String, String> params = invitation.asMap();
        params.put(INVITATION_TYPE, invitation.getType().getId());
        invitation.getProject().addFeature(PROJECT_FEATURE_TYPE, params);
        teamCityCore.persist(invitation.getProject(), "Invitation added");
        Loggers.SERVER.info("Invitation " + invitation.describe(false) + " is created in the project " + invitation.getProject().describe(false));
        getInvitation(invitation.getToken());//populate cache
        return invitation;
    }

    @NotNull
    public List<Invitation> getInvitations(@NotNull SProject project) {
        return project.getOwnFeaturesOfType(PROJECT_FEATURE_TYPE).stream().map(feature -> fromProjectFeature(project, feature)).collect(toList());
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

    public void updateInvitation(@NotNull Invitation invitation, @NotNull String description) {
        Optional<SProjectFeatureDescriptor> featureDescriptor = invitation.getProject().getOwnFeaturesOfType(PROJECT_FEATURE_TYPE).stream()
                .filter(feature -> feature.getParameters().get(TOKEN_PARAM_NAME).equals(invitation.getToken()))
                .findFirst();

        if (featureDescriptor.isPresent()) {
            Map<String, String> params = invitation.asMap();
            params.put(INVITATION_TYPE, invitation.getType().getId());
            invitation.getProject().updateFeature(featureDescriptor.get().getId(), PROJECT_FEATURE_TYPE, params);
            teamCityCore.persist(invitation.getProject(), description);
        }
    }

    @Nullable
    public Invitation getInvitation(@NotNull String token) {
        synchronized (this) {
            if (myInvitationByTokenCache == null) {
                myInvitationByTokenCache = new HashMap<>();
                teamCityCore.runAsSystem(() -> {
                    for (SProject project : teamCityCore.getActiveProjects()) {
                        for (SProjectFeatureDescriptor feature : project.getOwnFeaturesOfType(PROJECT_FEATURE_TYPE)) {
                            myInvitationByTokenCache.put(feature.getParameters().get(TOKEN_PARAM_NAME), fromProjectFeature(project, feature));
                        }
                    }
                    return null;
                });
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
