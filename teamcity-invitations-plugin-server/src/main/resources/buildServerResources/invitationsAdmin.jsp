<%@ page import="org.jetbrains.teamcity.invitations.InvitationAdminController" %>
<%@ taglib prefix="forms" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>
<jsp:useBean id="invitations" type="java.util.List" scope="request"/>

<bs:linkScript>
    ${teamcityPluginResourcesPath}invitationsAdmin.js
</bs:linkScript>

<style type="text/css">
    #createNewButton {
        margin-bottom: 1em;
    }

    .invitationsList {
        margin-top: 1em;
    }

    #invitationsTable td,
    #invitationsTable th {
        padding: 0.6em 1em;
    }

    #invitationsTable .edit {
        vertical-align: top;
        width: 6%;
        padding-left: 0.5em;
        padding-right: 0.5em;
        white-space: nowrap;
    }

    .textField {
        width: 100%;
    }

    .content {
        margin-top: 0.5em;
    }

    div .spacing {
        margin-top: 0.5em;
    }
</style>

<div>
    <forms:addButton id="createNewButton"
                     onclick="BS.CreateInvitationDialog.open();">Create invitation...</forms:addButton>
</div>

<bs:modalDialog formId="createInvitationForm"
                title="Create Invitation"
                action="#"
                closeCommand="BS.CreateInvitationDialog.close();"
                saveCommand="BS.CreateInvitationDialog.submit();">
    <div id="invitationTypeChooser">
        <label for="invitationType">Create invitation:</label>
        <forms:select id="invitationType" name="invitationType" enableFilter="true"
                      onchange="BS.CreateInvitationDialog.reloadInvitationType();" className="longField">
            <forms:option value="">-- Select invitation type --</forms:option>
            <c:forEach var="type" items="${invitationTypes}">
                <%--@elvariable id="type" type="org.jetbrains.teamcity.invitations.InvitationType"--%>
                <forms:option value="${type.id}"><c:out value="${type.description}"/></forms:option>
            </c:forEach>
        </forms:select>
        <forms:saving id="loadInvitationTypeProgress" className="progressRingInline"/>
    </div>

    <div class="content"></div>

    <div class="popupSaveButtonsBlock">
        <forms:submit label="Add" onclick="return BS.CreateInvitationDialog.submit();"/>
        <forms:cancel onclick="return BS.CreateInvitationDialog.close();"/>
        <forms:saving id="addInvitationFormProgress"/>
    </div>
</bs:modalDialog>

<bs:modalDialog formId="editInvitationForm"
                title="Edit invitation"
                closeCommand="BS.EditInvitationDialog.close();"
                action="#"
                saveCommand="BS.EditInvitationDialog.submit();">

    <forms:saving id="loadInvitationProgress" className="progressRingInline"/>

    <div class="content"></div>

    <div class="popupSaveButtonsBlock">
        <forms:submit id="createInvitationSumbit" label="Save"/>
        <forms:cancel onclick="BS.EditInvitationDialog.close();"/>
        <forms:saving id="editInvitationsFormProgress"/>
    </div>
</bs:modalDialog>

<h2>Pending invitations</h2>

<bs:refreshable containerId="invitationsList" pageUrl="${pageUrl}">
    <bs:messages key="<%=InvitationAdminController.MESSAGES_KEY%>"/>

    <div class="invitationsList">
        <c:if test="${empty invitations}">
            There are no invitations.
        </c:if>

        <c:if test="${not empty invitations}">
            <table id="invitationsTable" class="parametersTable">
                <tr>
                    <th>URL</th>
                    <th>Description</th>
                    <th>Reusable</th>
                    <th colspan="2">Actions</th>
                </tr>
                <c:forEach items="${invitations}" var="invitation">
                    <%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.Invitation"--%>
                    <tr>
                        <td class="highlight">
                        <span class="clipboard-btn tc-icon icon16 tc-icon_copy" data-clipboard-action="copy"
                              data-clipboard-target="#token_${invitation.token}"></span>
                            <span id="token_${invitation.token}"><c:out
                                    value="${invitationRootUrl}?token=${invitation.token}"/></span>
                        </td>
                        <td class="highlight">
                            <c:set value="${invitation}" scope="request" var="invitation"/>
                            <jsp:include page="${invitation.type.descriptionViewPath}"/>
                        </td>
                        <td class="highlight">
                            <c:if test="${!invitation.reusable}">No</c:if>
                            <c:if test="${invitation.reusable}">Yes</c:if>
                        </td>
                        <td class="highlight edit" onclick="BS.EditInvitationDialog.open('${invitation.token}');">
                            <a href="#">Edit</a></td>
                        <td class="edit">
                            <a href="#" onclick="BS.Invitations.deleteInvitation('${invitation.token}'); return false">Delete</a>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </c:if>
    </div>
</bs:refreshable>

<script type="text/javascript">
    BS.Clipboard('.clipboard-btn');
</script>
