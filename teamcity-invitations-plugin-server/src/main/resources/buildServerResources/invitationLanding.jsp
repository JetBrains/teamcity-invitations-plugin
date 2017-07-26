<%@ page import="jetbrains.buildServer.web.openapi.PlaceId" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="user" uri="/WEB-INF/functions/user" %>
<%@ taglib prefix="ext" tagdir="/WEB-INF/tags/ext" %>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>

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
    <c:if test="${intprop:getBoolean('teamcity.invitations.landing.uiExtensions.enabled')}">
        <ext:includeExtensions placeId="<%=PlaceId.ALL_PAGES_HEADER%>"/>
    </c:if>
  </jsp:attribute>
    <jsp:attribute name="body_include">
    <c:if test="${intprop:getBoolean('teamcity.invitations.landing.uiExtensions.enabled')}">
        <jsp:include page="/allPagesIncludeBeforeContentPlace.html"/>
    </c:if>

    <bs:_loginPageDecoration id="loginPage" title="${title}">
      <p id="formNote">
        <c:choose>
             <c:when test="${invitation == null}">
                The invitation is no longer actual. Either the one-time invitation was already used or the invitation was suspended. Proceed to the <a
                     href="<c:url value='/overview.html'/>">server</a>.
            </c:when>
             <c:when test="${!invitation.enabled}">
                 <c:choose>
                     <c:when test="${invitation.disabledText != null}">
                         <bs:out value="${invitation.disabledText}"/>
                     </c:when>
                     <c:otherwise>
                         This invitation is currently disabled, try again later.
                     </c:otherwise>
                 </c:choose>
            </c:when>
            <c:when test="${invitation.validationError != null}">
                This invitation is invalid, try again later.
            </c:when>
            <c:otherwise>
                <p>
                   <c:if test="${fn:length(welcomeText) > 0}">
                       <bs:out value="${welcomeText}"/>
                   </c:if>
                </p>
                <c:choose>
                  <%--@elvariable id="loggedInUser" type="jetbrains.buildServer.users.impl.UserEx"--%>
                    <c:when test="${loggedInUser == null}">
                    <p>
                        Please <a href="<c:url value='${proceedUrl}'/>">log in or register</a> to accept the invitation.
                    </p>
                  </c:when>
                  <c:when test="${loggedInUser != null && user:isGuestUser(loggedInUser)}">
                    <p>
                        You are logged in as a guest user who cannot accept invitations. <br/>
                        Please <a class="logout" href="#" onclick="BS.Invitations.logoutGuest(); return false">re-log in
                        or
                        register</a> to proceed.
                    </p>
                  </c:when>
                  <c:otherwise>
                    <%--@elvariable id="loggedInUser" type="jetbrains.buildServer.users.SUser"--%>
                      <p>
                          You are logged in as '<c:out value="${loggedInUser.descriptiveName}"/>'. <br/>
                          Please <a href="<c:url value='${proceedUrl}'/>">proceed</a> as the currently logged in user to
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
