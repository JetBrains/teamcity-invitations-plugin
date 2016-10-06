<%@ taglib prefix="forms" uri="http://www.springframework.org/tags/form" %>
<%@ include file="/include-internal.jsp" %>
<jsp:useBean id="invitations" type="java.util.Map" scope="request"/>

<bs:linkScript>
    ${teamcityPluginResourcesPath}invitationsAdmin.js
</bs:linkScript>

<style type="text/css">
    #invitationsTable {
        margin-top: 1em;
    }

</style>

<div>
    <forms:addButton
            onclick="BS.InvitationsDialog.showCentered(); return false;">Create new invitation...</forms:addButton>
</div>

<bs:modalDialog formId="invitationsForm"
                title="Create Invitation"
                closeCommand="BS.InvitationsDialog.close();"
                action="/admin/invitations.html?createInvitation=1"
                saveCommand="BS.InvitationsDialog.submit();">

    <span class="greyNote">Invite user to create a project and give him administrator role in the project</span>

    <div class="clr spacing"></div>

    <label for="registrationUrl" class="tableLabel">Registration Endpoint: <l:star/></label>
    <forms:textField name="registrationUrl" value="/registerUser.html"/>
    <div class="clr spacing"></div>

    <label for="parentProject" class="tableLabel">Parent Project: <l:star/></label>
    <forms:select id="parentProject" name="parentProject">
        <c:forEach items="${projects}" var="project">

            <forms:option value="${project.externalId}" selected="${project.externalId eq '_Root'}"
                          title="${project.name}"><c:out value="${project.name}"/>
            </forms:option>
        </c:forEach>
    </forms:select>


    <div class="popupSaveButtonsBlock">
        <forms:submit id="createInvitationSumbit" label="Add"/>
        <forms:cancel onclick="BS.InvitationsDialog.close();"/>
        <forms:saving id="invitationsFormProgress"/>
    </div>
</bs:modalDialog>


<bs:refreshable containerId="invitationsList" pageUrl="${pageUrl}">

    <c:if test="${not empty invitations}">

        <h2>Pending invitations</h2>

        <table id="invitationsTable" class="dark borderBottom">
            <tr>
                <th>URL</th>
                <th>Description</th>
                <th>Multi-user</th>
                <th>Delete</th>
            </tr>
            <c:forEach items="${invitations}" var="invitation">
                <tr>
                    <td>
                        <span class="clipboard-btn tc-icon icon16 tc-icon_copy" data-clipboard-action="copy"
                              data-clipboard-target="#token_${invitation.key}"></span>
                        <span id="token_${invitation.key}"><c:out
                                value="${invitationRootUrl}?token=${invitation.key}"/></span>
                    </td>
                    <td>
                        <c:out value="${invitation.value.description}"/>
                    </td>
                    <td>
                        <c:out value="${invitation.value.multiUser}"/>
                    </td>
                    <td>
                        <a href="#"
                           onclick="BS.Invitations.deleteInvitation('${invitation.key}'); return false">Delete</a>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </c:if>
</bs:refreshable>

<script type="text/javascript">
    BS.Clipboard('.clipboard-btn');
</script>
