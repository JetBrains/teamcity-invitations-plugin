package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class InvitationsFacadeApi {

    private final InvitationsStorage invitationsStorage;
    private final JoinProjectInvitationType joinProjectInvitationType;

    public InvitationsFacadeApi(InvitationsStorage invitationsStorage, JoinProjectInvitationType joinProjectInvitationType) {
        this.invitationsStorage = invitationsStorage;
        this.joinProjectInvitationType = joinProjectInvitationType;
    }

    public Invitation createJoinProjectInvitation(@NotNull SUser inviter, @NotNull String name, @NotNull String projectExtId, @NotNull String roleId, boolean multiuser) {
        String token = StringUtil.generateUniqueHash();
        JoinProjectInvitationType.InvitationImpl created = joinProjectInvitationType.createNewInvitation(inviter, name, token, projectExtId, roleId, multiuser);
        return invitationsStorage.addInvitation(token, created);
    }
}
