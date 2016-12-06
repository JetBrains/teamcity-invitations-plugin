package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

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

    public Invitation createJoinProjectInvitation(@NotNull SUser inviter, @NotNull String name, @NotNull String projectExtId, @NotNull String roleId, boolean multiuser) {
        String token = StringUtil.generateUniqueHash();
        JoinProjectInvitationType.InvitationImpl created = joinProjectInvitationType.createNewInvitation(inviter, name, token, projectExtId, roleId, multiuser);
        return invitationsStorage.addInvitation(token, created);
    }

    @NotNull
    public List<Invitation> getJoinProjectInvitations(@NotNull SProject project) {
        return invitationsStorage.getInvitations().stream()
                .filter(invitation -> invitation.getType().equals(joinProjectInvitationType))
                .filter(invitation -> project.equals(((JoinProjectInvitationType.InvitationImpl) invitation).getProject()))
                .collect(toList());
    }

    @NotNull
    public String getAbsoluteUrl(@NotNull Invitation invitation) {
        return invitationsLandingController.getInvitationsPath() + "?token" + invitation.getToken();
    }
}
