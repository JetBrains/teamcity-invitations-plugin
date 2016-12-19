<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>
<input type="hidden" name="projectId" value="${project.externalId}"/>
<input type="hidden" name="invitationType" value="existingProjectInvitation"/>

<script type="application/javascript">
    var changeSelectors = function () {
        var roleOrGroupSelected = false;
        if ($("addRole").checked) {
            roleOrGroupSelected = true;
            $('role').enable();
        } else {
            $('role').disable();
        }
        if ($("addToGroup").checked) {
            roleOrGroupSelected = true;
            $('group').enable();
        } else {
            $('group').disable();
        }

        if (roleOrGroupSelected) {
            $j(".modalDialog").find(".submitButton").prop("disabled", false);
            $j(".modalDialog").find(".submitButton").prop("title", "");
        } else {
            $j(".modalDialog").find(".submitButton").prop("disabled", true);
            $j(".modalDialog").find(".submitButton").prop("title", "Please select a role or a group");
        }
    };

    $("addRole").on('click', changeSelectors);
    $("addToGroup").on('click', changeSelectors);
    changeSelectors();
</script>

<div>
    <span>
        Invite user to join the <c:out value="${project.fullName}"/> project.
    </span>

    <input type="hidden" name="token" id="token" value="${token}"/>

    <div class="spacing"></div>
    <div><label for="name">Invitation name: <l:star/></label></div>
    <div><forms:textField name="name" value="${name}"/></div>

    <div class="spacing"></div>
    <div>
        <forms:checkbox name="multiuser" checked="${multiuser}"/>
        <label for="multiuser">Allow invitation to be used multiple times.</label>
    </div>

    <div class="spacing"></div>

    <div>
        <forms:checkbox name="addRole" checked="${roleId != null}"/>
        <label for="addRole">Give user a role in the selected project:</label>
        <forms:select id="role" name="role" enableFilter="true" className="textField">
            <c:forEach items="${roles}" var="role">
                <%--@elvariable id="role" type="jetbrains.buildServer.serverSide.auth.Role"--%>
                <forms:option value="${role.id}" title="${role.name}" selected="${role.id eq roleId}">
                    <c:out value="${role.name}"/>
                </forms:option>
            </c:forEach>
        </forms:select>
    </div>

    <div class="spacing"></div>

    <div>
        <forms:checkbox name="addToGroup" checked="${groupKey != null}"/>
        <label for="addToGroup">Add user to the usergroup:</label>
        <forms:select id="group" name="group" enableFilter="true" className="textField">
            <c:forEach items="${groups}" var="group">
                <%--@elvariable id="group" type="jetbrains.buildServer.groups.SUserGroup"--%>
                <forms:option value="${group.key}" title="${group.name}" selected="${group.key eq groupKey}">
                    <c:out value="${group.name}"/>
                </forms:option>
            </c:forEach>
        </forms:select>
    </div>

    <div class="spacing"></div>
    <div>
        <label for="welcomeText">Welcome text on the landing page:</label>
        <forms:textField name="welcomeText" style="width: 40em;" value="${welcomeText}" expandable="true"/>
    </div>
</div>