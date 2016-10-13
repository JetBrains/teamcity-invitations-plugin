<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.CreateNewProjectInvitationType.InvitationImpl"--%>

<div>
    Invite user to create a project under <bs:projectLink project="${invitation.parent}" target="_blank"/> project.
</div>