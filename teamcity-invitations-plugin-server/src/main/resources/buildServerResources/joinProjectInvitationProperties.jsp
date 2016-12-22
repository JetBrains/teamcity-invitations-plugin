<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>
<input type="hidden" name="projectId" value="${project.externalId}"/>
<input type="hidden" name="invitationType" value="existingProjectInvitation"/>
<input type="hidden" name="token" id="token" value="${token}"/>


<table class="runnerFormTable" style="width: 99%;">
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
            <span class="smallNote">Give user a role in the selected project</span>
            <span class="roleOrGroupError error" style="display: none;"></span>
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
            <span class="smallNote">Add user to the usergroup</span>
            <span class="roleOrGroupError error" style="display: none;"></span>
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
