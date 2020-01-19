<%@ include file="/include-internal.jsp"
%><%--
  ~ Copyright 2000-2020 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<c:set var="rolesParam" value=""
/><c:forEach items="${roles}" var="role"
><c:set var="rolesParam">${rolesParam}&role=${role.id}</c:set
></c:forEach
><c:url var="showPermsUrl" value="/rolesDescription.html?${rolesParam}"
/><span>view roles <a href="#" onclick="BS.Util.popupWindow('${showPermsUrl}', '_blank', {width: 550, height: 600}); return false" title="View permissions">permissions</a></span>