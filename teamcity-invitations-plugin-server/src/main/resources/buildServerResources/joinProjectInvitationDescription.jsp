<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.JoinProjectInvitationType.InvitationImpl"--%>

<div>
    Invite user to join the <bs:projectLink project="${invitation.project}" target="_blank"/> project
    with the <a target="_blank" href="/admin/admin.html?item=roles#${invitation.role.id}">${invitation.role.name}</a>
    role.
</div>