package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class InvitationsFacadeApi {

    private final InvitationsStorage invitationsStorage;
    private final JoinProjectInvitationType joinProjectInvitationType;
    private final InvitationsLandingController invitationsLandingController;

    public InvitationsFacadeApi(InvitationsStorage invitationsStorage, JoinProjectInvitationType joinProjectInvitationType, InvitationsLandingController invitationsLandingController) {
        this.invitationsStorage = invitationsStorage;
        this.joinProjectInvitationType = joinProjectInvitationType;
        this.invitationsLandingController = invitationsLandingController;
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
        return invitationsLandingController.getInvitationsPath() + "?token" + invitation.getToken();
    }
}
