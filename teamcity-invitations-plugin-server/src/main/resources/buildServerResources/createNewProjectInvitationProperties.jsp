<%@ page import="jetbrains.buildServer.serverSide.auth.Permission" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>

<input type="hidden" name="invitationType" value="newProjectInvitation"/>
<input type="hidden" name="projectId" value="${project.externalId}"/>
<input type="hidden" name="token" id="token" value="${token}"/>

<table class="runnerFormTable" style="width: 99%;">
    <tr class="greyNote">
        <td colspan="2">
            <span class="greyNote"> Invite user to create and administer a project under <c:out
                    value="${project.name}"/> </span>
        </td>
    </tr>

    <%@ include file="fragments/displayNameParam.jspf" %>

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
            <span class="smallNote">
                Select a role that will be given to the invited user.<br/>
                Roles with '<%=Permission.EDIT_PROJECT.getName()%>' permission are listed.<br/>
                <jsp:include page="fragments/rolesPopup.jsp">
                    <jsp:param name="roles" value="${roles}"/>
                </jsp:include>
            </span>
        </td>
    </tr>

    <%@ include file="fragments/welcomeTextParam.jspf" %>
    <%@ include file="fragments/reusableParam.jspf" %>

</table>