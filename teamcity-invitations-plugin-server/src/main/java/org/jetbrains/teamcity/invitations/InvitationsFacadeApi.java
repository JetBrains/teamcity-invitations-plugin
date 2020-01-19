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

import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class InvitationsFacadeApi {

    private final InvitationsStorage invitationsStorage;
    private final JoinProjectInvitationType joinProjectInvitationType;
    private final InvitationsLandingController invitationsLandingController;
    private final InvitationLandingProvider invitationLandingProvider;

    public InvitationsFacadeApi(InvitationsStorage invitationsStorage, JoinProjectInvitationType joinProjectInvitationType, InvitationsLandingController invitationsLandingController, InvitationLandingProvider invitationsLandingProvider) {
        this.invitationsStorage = invitationsStorage;
        this.joinProjectInvitationType = joinProjectInvitationType;
        this.invitationsLandingController = invitationsLandingController;
        this.invitationLandingProvider = invitationsLandingProvider;
    }

    public Invitation createJoinProjectInvitation(@NotNull SUser inviter, @NotNull String name, @NotNull SProject project,
                                                  @Nullable String roleId,
                                                  @Nullable String groupKey,
                                                  @NotNull String welcomeText,
                                                  boolean multiuser) {
        String token = StringUtil.generateUniqueHash();
        JoinProjectInvitationType.InvitationImpl created = joinProjectInvitationType.createNewInvitation(inviter, name, token, project, roleId, groupKey, multiuser, welcomeText);
        return invitationsStorage.addInvitation(created);
    }

    @NotNull
    public List<Invitation> getJoinProjectInvitations(@NotNull SProject project) {
        return invitationsStorage.getInvitations(project).stream()
                .filter(invitation -> invitation.getType().equals(joinProjectInvitationType))
                .collect(toList());
    }

    @Nullable
    public Invitation findInvitation(@NotNull String token) {
        return invitationsStorage.getInvitation(token);
    }

    /**
     * Returns relative link to invitations admin tab in the project
     */
    @NotNull
    public String getInvitationAdminPage(@NotNull String projectExtId) {
        return new RelativeWebLinks().getEditProjectPageUrl(projectExtId) + "&tab=" + InvitationAdminController.INVITATIONS_ADMIN_TAB_NAME;
    }

    @NotNull
    public String getAbsoluteUrl(@NotNull Invitation invitation) {
        return invitationsLandingController.getInvitationsPath() + "?token=" + invitation.getToken();
    }

    public void registerLandingPageProvider(@NotNull Function<Invitation, String> provider) {
        invitationLandingProvider.registerCustomProvider(provider);
    }
}
