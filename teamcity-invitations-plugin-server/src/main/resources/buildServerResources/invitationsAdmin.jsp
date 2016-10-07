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

    div .spacing {
        margin-top: 0.5em;
    }
</style>

<div>
    <forms:addButton id="createNewButton"
                     onclick="BS.InvitationsDialog.openNew();">Create new invitation...</forms:addButton>
</div>

<bs:modalDialog formId="invitationsForm"
                title=""
                closeCommand="BS.InvitationsDialog.close();"
                action="/admin/invitations.html?createInvitation=1"
                saveCommand="BS.InvitationsDialog.submit();">

    <span class="greyNote">
        Invitation that allows user to register on the server,
        creates the project based on username and gives the user Project Administrator role in the project.
    </span>

    <input type="hidden" name="token" id="token"/>

    <div class="spacing"></div>
    <div>
        <forms:checkbox name="multiuser"/>
        <label for="multiuser">Allow invitation to be used multiple times.</label>
    </div>
    <span class="greyNote">Invitation will be removed after user register using it if unchecked</span>

    <div class="spacing"></div>

    <div><label for="registrationUrl">Registration Endpoint: <l:star/></label></div>
    <div><forms:textField name="registrationUrl"/></div>
    <span class="greyNote">User will be redirected to the specified path to register</span>
    <div class="spacing"></div>

    <div><label for="afterRegistrationUrl">After Registration Endpoint: <l:star/></label></div>
    <div><forms:textField name="afterRegistrationUrl"/></div>
    <span class="greyNote">
        User will be redirected to the specified path when project is created and role is given to user.
        Path can contain project external id placeholder {projectExtId}
    </span>
    <div class="spacing"></div>

    <div>
        <label for="parentProject">Parent Project: <l:star/></label>
        <forms:select id="parentProject" name="parentProject" enableFilter="true" className="textField">
            <c:forEach items="${projects}" var="project">
                <%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>
                <forms:option value="${project.externalId}" title="${project.name}">
                    <c:out value="${project.name}"/>
                </forms:option>
            </c:forEach>
        </forms:select>
    </div>
    <span class="greyNote">The parent of the newly created project for the registered user</span>

    <div>
        <label for="role">Role: <l:star/></label>
        <forms:select id="role" name="role" enableFilter="true" className="textField">
            <c:forEach items="${roles}" var="role">
                <%--@elvariable id="role" type="jetbrains.buildServer.serverSide.auth.Role"--%>
                <forms:option value="${role.id}" title="${role.name}">
                    <c:out value="${role.name}"/>
                </forms:option>
            </c:forEach>
        </forms:select>
    </div>
    <span class="greyNote">The role will be given to user in the create project</span>

    <div class="popupSaveButtonsBlock">
        <forms:submit id="createInvitationSumbit" label="Add"/>
        <forms:cancel onclick="BS.InvitationsDialog.close();"/>
        <forms:saving id="invitationsFormProgress"/>
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
                    <th colspan="2">Actions</th>
                </tr>
                <c:forEach items="${invitations}" var="invitation">
                    <%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.Invitation"--%>
                    <c:set var="editOnClick">
                        return BS.InvitationsDialog.openEdit('${invitation.token}', ${invitation.multiUser}, '${invitation.registrationUrl}',
                        '${invitation.afterRegistrationUrl}', '${invitation.parentProject.externalId}', '${invitation.role.id}');
                    </c:set>

                    <tr>
                        <td class="highlight">
                        <span class="clipboard-btn tc-icon icon16 tc-icon_copy" data-clipboard-action="copy"
                              data-clipboard-target="#token_${invitation.token}"></span>
                            <span id="token_${invitation.token}"><c:out
                                    value="${invitationRootUrl}?token=${invitation.token}"/></span>
                        </td>

                        <td class="highlight">
                            Registration URL: <a target="_blank"
                                                 href="${invitation.registrationUrl}">${invitation.registrationUrl}</a><br/>
                            Parent Project: <bs:projectLink project="${invitation.parentProject}" target="_blank"/><br/>
                            Role: <a target="_blank"
                                     href="/admin/admin.html?item=roles#${invitation.role.id}">${invitation.role.name}</a><br/>
                            After Registration URL: <a target="_blank"
                                                       href="${invitation.afterRegistrationUrl}">${invitation.afterRegistrationUrl}</a><br/>
                            Multi-user: ${invitation.multiUser}
                        </td>
                        <td class="highlight edit" onclick="${editOnClick}">
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
