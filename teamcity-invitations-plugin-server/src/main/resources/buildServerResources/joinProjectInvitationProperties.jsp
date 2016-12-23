<%@ page import="jetbrains.buildServer.serverSide.auth.Permission" %>
<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>
<%@ taglib prefix="roles" tagdir="/WEB-INF/tags/roles" %>

<table class="runnerFormTable" style="width: 99%;">
    <tr class="greyNote">
        <td colspan="2">
            <span class="greyNote">
                Invite user to join the <c:out value="${project.name}"/> project.
            </span>
        </td>
    </tr>

    <%@ include file="fragments/displayNameParam.jspf" %>

    <tr>
        <td><label for="role">Role:</label></td>
        <td>
            <forms:select id="role" name="role" enableFilter="true" className="longField">
                <forms:option value="">-- Don't assign any role --</forms:option>
                <c:forEach items="${roles}" var="role">
                    <%--@elvariable id="role" type="jetbrains.buildServer.serverSide.auth.Role"--%>
                    <forms:option value="${role.id}" title="${role.name}" selected="${role.id eq roleId}">
                        <c:out value="${role.name}"/>
                    </forms:option>
                </c:forEach>
            </forms:select>

            <span class="smallNote">
                Select a role that will be given to the invited user (<jsp:include page="fragments/rolesPopup.jsp">
                <jsp:param name="roles" value="${roles}"/>
            </jsp:include>)
            </span>
            <span id="error_role" class="roleOrGroupError error" style="display: none;"></span>
        </td>
    </tr>

    <tr>
        <td><label for="group">Group:</label></td>
        <td>
            <forms:select id="group" name="group" enableFilter="true" className="longField">
                <forms:option value="">-- Don't add to any group --</forms:option>
                <c:forEach items="${groups}" var="group">
                    <%--@elvariable id="group" type="jetbrains.buildServer.groups.SUserGroup"--%>
                    <forms:option value="${group.key}" title="${group.name}" selected="${group.key eq groupKey}">
                        <c:out value="${group.name}"/>
                    </forms:option>
                </c:forEach>
            </forms:select>
            <span class="smallNote">
                Select a usergroup where invited user will be added. <br/>
                Groups containing '<%=Permission.VIEW_PROJECT.getName()%>' permission are listed.
            </span>

            <span id="error_group" class="roleOrGroupError error" style="display: none;"></span>
        </td>
    </tr>

    <%@ include file="fragments/welcomeTextParam.jspf" %>
    <%@ include file="fragments/reusableParam.jspf" %>

</table>

<script type="application/javascript">
    var changeSelectors = function () {
        if ($j("#group").get(0).selectedIndex != 0 || $j("#role").get(0).selectedIndex != 0) {
            $j(".modalDialog").find(".submitButton").prop("disabled", false);
            $j('.roleOrGroupError').text("");
            $j('.roleOrGroupError').hide();
        } else {
            $j(".modalDialog").find(".submitButton").prop("disabled", true);
            $j('.roleOrGroupError').show();
            $j('.roleOrGroupError').text("Please select a role or a group");
        }
    };

    $("group").on('change', changeSelectors);
    $("role").on('change', changeSelectors);
    changeSelectors();
</script>
