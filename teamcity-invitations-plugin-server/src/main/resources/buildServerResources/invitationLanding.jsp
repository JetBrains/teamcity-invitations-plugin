<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="user" uri="/WEB-INF/functions/user" %>
<%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.JoinProjectInvitationType.InvitationImpl"--%>
<%@ include file="/include-internal.jsp" %>
<c:url value="/ajax.html?logout=1" var="logoutUrl"/>
<c:url value='${proceedUrl}' var="proceedFullUrl"/>

<bs:externalPage>
    <jsp:attribute name="page_title">${title}</jsp:attribute>
    <jsp:attribute name="head_include">
    <bs:linkCSS>
      /css/forms.css
      /css/maintenance-initialPages-common.css
      /css/initialPages.css
    </bs:linkCSS>
    <bs:linkScript>
      /js/bs/bs.js
    </bs:linkScript>
    <bs:ua/>
  </jsp:attribute>
    <jsp:attribute name="body_include">

    <bs:_loginPageDecoration id="loginPage" title="${title}">
      <p id="formNote">
        <c:choose>
             <c:when test="${invitation == null}">
                The invitation does not exist. The provided URL is incorrect or the invitation was deleted on server.
            </c:when>
             <c:when test="${!invitation.enabled}">
                This invitation is currently disabled, try again later.
            </c:when>
            <c:when test="${invitation.validationError != null}">
                This invitation is invalid, try again later.
            </c:when>
            <c:otherwise>
                <p>
                   <c:if test="${fn:length(welcomeText) > 0}">
                       <c:out value="${welcomeText}"/>
                   </c:if>
                </p>
                <c:choose>
                  <%--@elvariable id="loggedInUser" type="jetbrains.buildServer.users.impl.UserEx"--%>
                    <c:when test="${loggedInUser == null}">
                    <p>
                        Please <a href="<c:url value='${proceedUrl}'/>">login or register</a> to accept the invitation.
                    </p>
                  </c:when>
                  <c:when test="${loggedInUser != null && user:isGuestUser(loggedInUser)}">
                    <p>
                        You are logged in as a Guest user who can't accept invitations. <br/>
                        Please <a class="logout" href="#" onclick="BS.Invitations.logoutGuest(); return false">re-login
                        or
                        register</a> to proceed.
                    </p>
                  </c:when>
                  <c:otherwise>
                    <%--@elvariable id="loggedInUser" type="jetbrains.buildServer.users.SUser"--%>
                      <p>
                          You are logged in as '<c:out value="${loggedInUser.descriptiveName}"/>'. <br/>
                          Please <a href="<c:url value='${proceedUrl}'/>">proceed</a> as a currently logged-in user to
                          accept the
                          invitation.
                      </p>
                  </c:otherwise>
                </c:choose>
           </c:otherwise>
        </c:choose>

    </bs:_loginPageDecoration>
  </jsp:attribute>
</bs:externalPage>

<script type="application/javascript">
    BS.Invitations = {
        logoutGuest: function () {
            BS.ajaxRequest("${logoutUrl}", {
                onComplete: function () {
                    document.location.href = "<c:url value='${proceedFullUrl}'/>";
                }
            });
        }
    };
</script>
