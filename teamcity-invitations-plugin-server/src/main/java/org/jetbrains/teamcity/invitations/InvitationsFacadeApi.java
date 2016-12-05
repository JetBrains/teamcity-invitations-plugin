package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.StringUtil;

public class InvitationsFacadeApi {

    private final InvitationsStorage invitationsStorage;
    private final JoinProjectInvitationType joinProjectInvitationType;

    public InvitationsFacadeApi(InvitationsStorage invitationsStorage, JoinProjectInvitationType joinProjectInvitationType) {
        this.invitationsStorage = invitationsStorage;
        this.joinProjectInvitationType = joinProjectInvitationType;
    }

    public Invitation createJoinProjectInvitation(SUser inviter, String name, String projectExtId, String roleId, boolean multiuser) {
        String token = StringUtil.generateUniqueHash();
        JoinProjectInvitationType.InvitationImpl created = joinProjectInvitationType.createNewInvitation(inviter, token, name, projectExtId, roleId, multiuser);
        return invitationsStorage.addInvitation(token, created);
    }
}
