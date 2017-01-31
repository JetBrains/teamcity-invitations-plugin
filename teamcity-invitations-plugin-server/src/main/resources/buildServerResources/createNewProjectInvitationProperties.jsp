<%@ page import="jetbrains.buildServer.serverSide.auth.Permission" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>

<table class="runnerFormTable" style="width: 99%;">
    <tr class="greyNote">
        <td colspan="2">
            <span class="greyNote">Invite users to create and administer sub-projects under the <c:out value="${project.name}"/></span>
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
            <span class="smallNote">
                Select a role that will be given to the invited user.<br/>
                Only roles with '<%=Permission.EDIT_PROJECT.getName()%>' permission are listed
                (<jsp:include page="fragments/rolesPopup.jsp"><jsp:param name="roles" value="${roles}"/></jsp:include>)
            </span>
            <span class="error" id="error_role"></span>
        </td>
    </tr>

    <%@ include file="fragments/displayNameParam.jspf" %>
    <%@ include file="fragments/welcomeTextParam.jspf" %>
    <%@ include file="fragments/reusableParam.jspf" %>

</table>