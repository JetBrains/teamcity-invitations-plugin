<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.ProjectAdminInvitation"--%>

<div>
    Registration URL: <a target="_blank"
                         href="${invitation.registrationUrl}">${invitation.registrationUrl}</a><br/>
    Parent Project: <bs:projectLink project="${invitation.parentProject}" target="_blank"/><br/>
    Role: <a target="_blank"
             href="/admin/admin.html?item=roles#${invitation.role.id}">${invitation.role.name}</a><br/>
    After Registration URL: <a target="_blank"
                               href="${invitation.afterRegistrationUrl}">${invitation.afterRegistrationUrl}</a><br/>
    Multi-user: ${invitation.multiUser}
</div>