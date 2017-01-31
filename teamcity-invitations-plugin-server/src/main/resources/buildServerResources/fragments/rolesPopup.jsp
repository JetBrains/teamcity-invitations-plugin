<%@ include file="/include-internal.jsp"
%><c:set var="rolesParam" value=""
/><c:forEach items="${roles}" var="role"
><c:set var="rolesParam">${rolesParam}&role=${role.id}</c:set
></c:forEach
><c:url var="showPermsUrl" value="/rolesDescription.html?${rolesParam}"
/><span>view roles <a href="#" onclick="BS.Util.popupWindow('${showPermsUrl}', '_blank', {width: 550, height: 600}); return false" title="View permissions">permissions</a></span>