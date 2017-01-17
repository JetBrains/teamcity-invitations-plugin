<%@ page import="org.jetbrains.teamcity.invitations.InvitationAdminController" %>
<%@ taglib prefix="forms" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>
<jsp:useBean id="invitations" type="java.util.List" scope="request"/>
<c:set var="projectExternalId" value="${project.externalId}"/>
<bs:linkScript>
    ${teamcityPluginResourcesPath}invitationsAdmin.js
</bs:linkScript>

<style type="text/css">
    .invitationsList {
        margin-top: 1em;
    }

    .textField {
        width: 100%;
    }

    #invitationFormDialog {
        width: 45em;
    }

    table.runnerFormTable td:first-child {
        width: 10em;
    }

</style>

<bs:modalDialog formId="invitationForm"
                title="Create Invitation"
                action="#"
                closeCommand="BS.InvitationDialog.close();"
                saveCommand="BS.InvitationDialog.submit();">
    <table id="invitationTypeChooser" class="runnerFormTable" style="width: 99%;">
        <tr>
            <td>
                <label for="invitationType">Invitation type: </label>
            </td>
            <td>
                <div id="invitationTypeContainer">
                    <forms:select name="invitationTypeSelect" enableFilter="true"
                                  onchange="BS.InvitationDialog.invitationTypeChanged(this, '${projectExternalId}');"
                                  className="longField">
                        <forms:option value="">-- Select invitation type --</forms:option>
                        <c:forEach var="type" items="${invitationTypes}">
                            <%--@elvariable id="type" type="org.jetbrains.teamcity.invitations.InvitationType"--%>
                            <forms:option value="${type.id}"><c:out value="${type.description}"/></forms:option>
                        </c:forEach>
                    </forms:select>
                    <forms:saving id="loadInvitationTypeProgress" className="progressRingInline"/>
                </div>
                <span id="readOnlyInvitationType"></span>
            </td>
        </tr>

    </table>

    <div class="content"></div>

    <div class="popupSaveButtonsBlock">
        <forms:submit label="Save" onclick="return BS.InvitationDialog.submit();"/>
        <forms:cancel onclick="return BS.InvitationDialog.close();"/>
        <forms:saving id="invitationFormProgress"/>
        <input type="hidden" name="token" value=""/>
        <input type="hidden" name="invitationType" value=""/>
        <input type="hidden" name="projectId" value="${projectExternalId}"/>
        <input type="hidden" name="saveInvitation" value="true"/>
    </div>
</bs:modalDialog>

<div class="section noMargin">
    <h2 class="noBorder">Invitations</h2>
    <bs:smallNote>
        Create an invitation link and send it to anyone you want to invite to this project.
    </bs:smallNote>
    <c:if test="${not project.readOnly}">
        <div>
            <forms:addButton
                    onclick="BS.InvitationDialog.openAddDialog('${projectExternalId}');">Create invitation...</forms:addButton>
        </div>
    </c:if>

    <bs:refreshable containerId="invitationsList" pageUrl="${pageUrl}">
        <bs:messages key="<%=InvitationAdminController.MESSAGES_KEY%>"/>
        <div class="invitationsList">
            <c:if test="${not empty invitations}">
                <l:tableWithHighlighting className="parametersTable" highlightImmediately="true">
                    <tr>
                        <th>Invitation</th>
                        <th>Parameters Description</th>
                        <th colspan="3">URL</th>
                    </tr>
                    <c:forEach items="${invitations}" var="invitation">
                        <%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.Invitation"--%>
                        <c:set value="BS.InvitationDialog.openEditDialog('${invitation.token}', '${invitation.type.description}', '${invitation.type.id}', '${projectExternalId}');"
                               var="onclick"/>
                        <tr style="${not invitation.enabled ? 'color: #888': ''}">
                            <td class="highlight">
                                <c:if test="${invitation.type.description != invitation.name}"><em>(<c:out
                                        value='${invitation.type.description}'/>)</em><br/> </c:if><c:out
                                    value='${invitation.name}'/>
                                <c:if test="${!invitation.enabled}"> (disabled)</c:if>
                            </td>
                            <td class="highlight">
                                <c:set value="${invitation}" scope="request" var="invitation"/>
                                <jsp:include page="${invitation.type.descriptionViewPath}"/>
                            </td>
                            <td class="highlight">
                                <span class="clipboard-btn tc-icon icon16 tc-icon_copy" data-clipboard-action="copy"
                                      data-clipboard-target="#token_${invitation.token}"></span>
                                <span id="token_${invitation.token}"><c:out
                                        value="${invitationRootUrl}?token=${invitation.token}"/></span>
                            </td>
                            <td class="edit highlight" onclick="${onclick}">
                                <a href="#">Edit</a>
                            </td>
                            <td class="edit">
                                <bs:actionsPopup controlId="invitationActions${invitation.token}"
                                                 popup_options="shift: {x: -150, y: 20}, className: 'quickLinksMenuPopup'">
                                    <jsp:attribute name="content">
                                        <div>
                                            <ul class="menuList">
                                              <l:li>
                                                  <c:if test="${invitation.enabled}">
                                                    <a href="#"
                                                       onclick="BS.Invitations.setEnabled('${invitation.token}', '${projectExternalId}', false); return false">Disable
                                                        invitation</a>
                                                  </c:if>
                                                  <c:if test="${!invitation.enabled}">
                                                    <a href="#"
                                                       onclick="BS.Invitations.setEnabled('${invitation.token}', '${projectExternalId}', true); return false">Enable
                                                        invitation</a>
                                                  </c:if>
                                              </l:li>
                                                <l:li>
                                                <a href="#"
                                                   onclick="BS.Invitations.deleteInvitation('${invitation.token}', '${projectExternalId}'); return false">Delete...</a>
                                              </l:li>
                                            </ul>
                                        </div>
                                    </jsp:attribute>
                                    <jsp:body></jsp:body>
                                </bs:actionsPopup>
                            </td>
                        </tr>
                    </c:forEach>
                </l:tableWithHighlighting>
            </c:if>
        </div>
    </bs:refreshable>

</div>
<script type="text/javascript">
    BS.Clipboard('.clipboard-btn');
</script>
