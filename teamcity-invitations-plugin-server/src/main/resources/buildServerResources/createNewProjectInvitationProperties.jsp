<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>

<input type="hidden" name="invitationType" value="newProjectInvitation"/>
<input type="hidden" name="projectId" value="${project.externalId}"/>
<input type="hidden" name="token" id="token" value="${token}"/>

<table class="runnerFormTable" style="width: 99%;">
    <tr>
        <td><label for="name">Display name:</label><l:star/></td>
        <td>
            <forms:textField name="name" value="${name}" className="longField"/>
            <span class="smallNote">Provide some name to distinguish this invitation from others.</span>
        </td>
    </tr>

    <tr>
        <td><label for="role">Role:</label><l:star/></td>
        <td>
            <forms:select id="role" name="role" enableFilter="true" className="longField">
                <c:forEach items="${roles}" var="role">
                    <%--@elvariable id="role" type="jetbrains.buildServer.serverSide.auth.Role"--%>
                    <forms:option value="${role.id}" title="${role.name}" selected="${role.id eq roleId}">
                        <c:out value="${role.name}"/>
                    </forms:option>
                </c:forEach>
            </forms:select>
        </td>
    </tr>

    <tr>
        <td><label for="welcomeText">Welcome Text:</label><l:star/></td>
        <td>
            <forms:textField name="welcomeText" value="${welcomeText}" expandable="true"/>
            <span class="smallNote">Text that will be shown on the landing page</span>
        </td>
    </tr>

    <tr>
        <td><label for="multiuser">Reusable:</label></td>
        <td>
            <forms:checkbox name="multiuser" checked="${multiuser}"/>
            <span class="smallNote">Allow invitation to be used multiple times.</span>
        </td>
    </tr>

</table>