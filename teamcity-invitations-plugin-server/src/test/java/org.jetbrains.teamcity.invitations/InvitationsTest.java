package org.jetbrains.teamcity.invitations;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.controllers.ActionMessages;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.groups.SUserGroup;
import jetbrains.buildServer.serverSide.ProjectsModelListener;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.ServerSideEventDispatcher;
import jetbrains.buildServer.serverSide.auth.*;
import jetbrains.buildServer.serverSide.impl.auth.SecurityContextImpl;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.web.openapi.*;
import jetbrains.buildServer.web.util.SessionUser;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static jetbrains.buildServer.serverSide.auth.RoleScope.projectScope;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@Test
public class InvitationsTest extends BaseTestCase {

    private InvitationsStorage invitations;
    private InvitationsLandingController invitationsController;
    private InvitationsProceedController invitationsProceedController;
    private InvitationAdminController invitationsAdminController;
    private CreateNewProjectInvitationType createNewProjectInvitationType;
    private JoinProjectInvitationType joinProjectInvitationType;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;
    private FakeTeamCityCoreFacade core;
    private SProject testDriveProject;
    private SecurityContextImpl securityContext;

    private Role adminRole;
    private Role developerRole;
    private Role systemAdminRole;
    private SUser systemAdmin;
    private EventDispatcher<ProjectsModelListener> events;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        securityContext = new SecurityContextImpl();
        events = ServerSideEventDispatcher.create(securityContext, ProjectsModelListener.class);
        core = new FakeTeamCityCoreFacade(securityContext, events);
        systemAdminRole = core.addRole("SYSTEM_ADMIN", new Permissions(Permission.values()), false);
        adminRole = core.addRole("PROJECT_ADMIN", new Permissions(Permission.CREATE_SUB_PROJECT, Permission.CHANGE_USER_ROLES_IN_PROJECT), true);
        developerRole = core.addRole("PROJECT_DEVELOPER", new Permissions(Permission.RUN_BUILD), true);

        systemAdmin = core.createUser("admin");
        systemAdmin.addRole(RoleScope.globalScope(), systemAdminRole);

        login(systemAdmin);
        testDriveProject = core.createProject("_Root", "TestDriveProjectId");
        createNewProjectInvitationType = new CreateNewProjectInvitationType(core, events);
        joinProjectInvitationType = new JoinProjectInvitationType(core);
        invitations = createInvitationStorage();

        WebControllerManager webControllerManager = createWebControllerManager();

        invitationsController = new InvitationsLandingController(webControllerManager, invitations, Mockito.mock(AuthorizationInterceptor.class),
                Mockito.mock(RootUrlHolder.class));

        invitationsProceedController = new InvitationsProceedController(webControllerManager, invitations);

        PluginDescriptor pluginDescriptor = Mockito.mock(PluginDescriptor.class);
        when(pluginDescriptor.getPluginResourcesPath(anyString())).thenReturn("fake.jsp");
        invitationsAdminController = new InvitationAdminController(createPagePlaces(), webControllerManager,
                pluginDescriptor, invitations, core, invitationsController, asList(createNewProjectInvitationType, joinProjectInvitationType));

        newRequest(HttpMethod.GET, "/");
    }

    private PagePlaces createPagePlaces() {
        PagePlaces pagePlaces = Mockito.mock(PagePlaces.class);
        when(pagePlaces.getPlaceById(any())).thenReturn(Mockito.mock(PagePlace.class));
        return pagePlaces;
    }

    @NotNull
    private WebControllerManager createWebControllerManager() {
        WebControllerManager webControllerManager = Mockito.mock(WebControllerManager.class);
        Multimap<BaseController, ControllerAction> actions = Multimaps.synchronizedMultimap(HashMultimap.create());

        when(webControllerManager.getAction(any(BaseController.class), any(HttpServletRequest.class)))
                .thenAnswer(inv -> actions.get(inv.getArgument(0)).stream().filter(action -> action.canProcess(request)).findAny().orElseGet(null));
        doAnswer(inv -> actions.put(inv.getArgument(0), inv.getArgument(1)))
                .when(webControllerManager).registerAction(any(BaseController.class), any(ControllerAction.class));
        return webControllerManager;
    }

    @NotNull
    private InvitationsStorage createInvitationStorage() {
        return new InvitationsStorage(core, asList(createNewProjectInvitationType, joinProjectInvitationType), events);
    }

    @Test
    public void invite_user_to_create_a_project() throws Exception {
        login(systemAdmin);
        String token = createInvitationToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", true).getToken();

        //user go to invitation url
        logout();
        ModelAndView invitationResponse = goToInvitationUrl(token);
        then(invitationResponse.getViewName()).isEqualTo("createNewProjectInvitationLanding.jsp");
        then(invitationResponse.getModel().get("invitation")).isInstanceOf(CreateNewProjectInvitationType.InvitationImpl.class);
        then(((CreateNewProjectInvitationType.InvitationImpl) invitationResponse.getModel().get("invitation")).getUser()).isEqualTo(systemAdmin);
        then(((CreateNewProjectInvitationType.InvitationImpl) invitationResponse.getModel().get("invitation")).getProject()).isEqualTo(testDriveProject);
        then(invitationResponse.getModel().get("loggedInUser")).isEqualTo(null);

        //user registered
        SUser user = core.createUser("oleg");
        login(user);
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl();
        then(afterRegistrationMAW.getView()).isInstanceOf(RedirectView.class);
        then(((RedirectView) afterRegistrationMAW.getView()).getUrl()).endsWith("/admin/createObjectMenu.html?showMode=createProjectMenu&projectId=TestDriveProjectId");

        newRequest(HttpMethod.GET, ((RedirectView) afterRegistrationMAW.getView()).getUrl());
        core.createProject("TestDriveProjectId", "New Project");
        then(user.getRolesWithScope(projectScope("New Project"))).extracting(Role::getId).contains("PROJECT_ADMIN");

        //try to create one more project under parent
        newRequest(HttpMethod.GET, ((RedirectView) afterRegistrationMAW.getView()).getUrl());
        try {
            core.createProject("TestDriveProjectId", "New Project 2");
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException e) {
            //ok
        }

    }

    @Test
    public void invite_user_to_join_the_project_using_direct_role() throws Exception {
        login(systemAdmin);
        String token = createInvitationToJoinProject("PROJECT_DEVELOPER", null, "TestDriveProjectId", true).getToken();

        //user go to invitation url
        logout();
        ModelAndView invitationResponse = goToInvitationUrl(token);
        then(invitationResponse.getViewName()).isEqualTo("joinProjectInvitationLanding.jsp");
        then(invitationResponse.getModel().get("invitation")).isInstanceOf(JoinProjectInvitationType.InvitationImpl.class);
        then(((JoinProjectInvitationType.InvitationImpl) invitationResponse.getModel().get("invitation")).getUser()).isEqualTo(systemAdmin);
        then(((JoinProjectInvitationType.InvitationImpl) invitationResponse.getModel().get("invitation")).getProject()).isEqualTo(testDriveProject);
        then(invitationResponse.getModel().get("loggedInUser")).isEqualTo(null);

        //user registered
        SUser user = core.createUser("oleg");
        login(user);
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl();
        then(afterRegistrationMAW.getView()).isInstanceOf(RedirectView.class);
        then(user.getRolesWithScope(projectScope("TestDriveProjectId"))).extracting(Role::getId).contains("PROJECT_DEVELOPER");
    }

    @Test
    public void invite_user_to_join_the_project_using_group() throws Exception {
        login(systemAdmin);
        SUserGroup developers = core.createGroup("developers");
        developers.addRole(projectScope(testDriveProject.getProjectId()), developerRole);
        String token = createInvitationToJoinProject(null, "developers", "TestDriveProjectId", true).getToken();

        //user go to invitation url
        logout();
        ModelAndView invitationResponse = goToInvitationUrl(token);
        then(invitationResponse.getViewName()).isEqualTo("joinProjectInvitationLanding.jsp");
        then(invitationResponse.getModel().get("invitation")).isInstanceOf(JoinProjectInvitationType.InvitationImpl.class);
        then(((JoinProjectInvitationType.InvitationImpl) invitationResponse.getModel().get("invitation")).getUser()).isEqualTo(systemAdmin);
        then(((JoinProjectInvitationType.InvitationImpl) invitationResponse.getModel().get("invitation")).getProject()).isEqualTo(testDriveProject);
        then(invitationResponse.getModel().get("loggedInUser")).isEqualTo(null);

        //user registered
        SUser user = core.createUser("oleg");
        login(user);
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl();
        then(afterRegistrationMAW.getView()).isInstanceOf(RedirectView.class);
        then(core.getGroupUsers(developers)).contains(user);
    }

    @Test
    public void process_invitation_when_user_already_logged_in() throws Exception {
        login(systemAdmin);
        String token = createInvitationToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", true).getToken();

        //logged in user go to invitation url
        SUser user = core.createUser("oleg");
        login(user);
        ModelAndView invitationResponse = goToInvitationUrl(token);
        then(invitationResponse.getViewName()).isEqualTo("createNewProjectInvitationLanding.jsp");
        then(invitationResponse.getModel().get("invitation")).isInstanceOf(CreateNewProjectInvitationType.InvitationImpl.class);
        then(((CreateNewProjectInvitationType.InvitationImpl) invitationResponse.getModel().get("invitation")).getUser()).isEqualTo(systemAdmin);
        then(((CreateNewProjectInvitationType.InvitationImpl) invitationResponse.getModel().get("invitation")).getProject()).isEqualTo(testDriveProject);
        then((invitationResponse.getModel().get("loggedInUser"))).isEqualTo(user);
        then((invitationResponse.getModel().get("proceedUrl"))).isEqualTo(InvitationsProceedController.PATH);

        //user registered
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl();
        then(((RedirectView) afterRegistrationMAW.getView()).getUrl()).endsWith("/admin/createObjectMenu.html?showMode=createProjectMenu&projectId=TestDriveProjectId");

        newRequest(HttpMethod.GET, ((RedirectView) afterRegistrationMAW.getView()).getUrl());
        core.createProject("TestDriveProjectId", "oleg project");
        then(user.getRolesWithScope(projectScope("oleg project"))).extracting(Role::getId).contains("PROJECT_ADMIN");
    }

    public void should_survive_server_restart() throws Exception {
        login(systemAdmin);
        String token1 = createInvitationToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", true).getToken();
        String token2 = createInvitationToJoinProject("PROJECT_DEVELOPER", null, "_Root", false).getToken();
        Map<String, String> beforeRestartEl1 = invitations.getInvitation(token1).asMap();
        Map<String, String> beforeRestartEl2 = invitations.getInvitation(token2).asMap();

        invitations = createInvitationStorage();

        then(invitations.getInvitation(token1)).isNotNull();
        then(invitations.getInvitation(token2)).isNotNull();

        Map<String, String> afterRestartEl1 = invitations.getInvitation(token1).asMap();
        Map<String, String> afterRestartEl2 = invitations.getInvitation(token2).asMap();
        then(afterRestartEl1).isEqualTo(beforeRestartEl1);
        then(afterRestartEl2).isEqualTo(beforeRestartEl2);
    }

    public void remove_invitation() throws Exception {
        login(systemAdmin);
        String token = createInvitationToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", true).getToken();
        invitations.removeInvitation(testDriveProject, token);

        then(invitations.getInvitation(token)).isNull();

        //user go to invitation url
        logout();
        ModelAndView invitationResponse = goToInvitationUrl(token);
        assertRedirectTo(invitationResponse, "/");
    }

    public void invitation_removed_during_user_registration() throws Exception {
        login(systemAdmin);
        String token = createInvitationToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", true).getToken();

        //user go to invitation url
        logout();
        goToInvitationUrl(token);

        invitations.removeInvitation(testDriveProject, token);

        SUser user = core.createUser("oleg");
        login(user);
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl();
        assertRedirectTo(afterRegistrationMAW, "/");
    }

    public void multiple_user_invitation_can_be_used_several_times() throws Exception {
        login(systemAdmin);
        String token = createInvitationToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", true).getToken();

        //first
        logout();
        assertViewName(goToInvitationUrl(token), "createNewProjectInvitationLanding.jsp");
        login(core.createUser("oleg"));
        goToAfterRegistrationUrl();
        newRequest(HttpMethod.GET, "/");
        core.createProject("TestDriveProjectId", "oleg project");

        //second
        logout();
        assertViewName(goToInvitationUrl(token), "createNewProjectInvitationLanding.jsp");
        login(core.createUser("ivan"));
        goToAfterRegistrationUrl();
        newRequest(HttpMethod.GET, "/");
        core.createProject("TestDriveProjectId", "ivan project");

        then(core.getProject("oleg project")).isNotNull();
        then(core.getProject("ivan project")).isNotNull();
    }

    public void single_user_invitation_can_be_used_once() throws Exception {
        login(systemAdmin);
        String token = createInvitationToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", false).getToken();

        //first
        logout();
        assertViewName(goToInvitationUrl(token), "createNewProjectInvitationLanding.jsp");
        login(core.createUser("oleg"));
        goToAfterRegistrationUrl();

        //second
        assertRedirectTo(goToInvitationUrl(token), "/");
    }

    public void user_cant_invite_project_admin_to_inaccessible_project() throws Exception {
        SUser projectAdmin = core.createUser("oleg");
        projectAdmin.addRole(projectScope(testDriveProject.getProjectId()), adminRole);
        login(projectAdmin);

        newRequest(HttpMethod.GET, "/admin/invitations.html?addInvitation=1&projectId=" + testDriveProject.getExternalId() + "&invitationType=newProjectInvitation");
        ModelAndView modelAndView = invitationsAdminController.handleRequestInternal(request, response);
        then(((List<Role>) modelAndView.getModel().get("roles"))).containsOnly(adminRole, developerRole).doesNotContain(systemAdminRole);

        newRequest(HttpMethod.GET, "/admin/invitations.html?addInvitation=1&projectId=_Root&invitationType=newProjectInvitation");
        invitationsAdminController.handleRequestInternal(request, response);
        then(response.getStatus()).isEqualTo(403);

        try {
            createInvitationToCreateProject(adminRole.getId(), "_Root", true); //can't invite to roo
            fail("Access denied expected");
        } catch (AccessDeniedException ignored) {
        }
    }

    public void user_cant_invite_project_admin_without_assign_role_permission() throws Exception {
        SUser oleg = core.createUser("oleg");
        Role role = core.addRole("PROJECT_ADMIN2", new Permissions(Permission.CREATE_SUB_PROJECT), true);//no CHANGE_USER_ROLES_IN_PROJECT permission
        oleg.addRole(projectScope(testDriveProject.getProjectId()), role);
        login(oleg);

        newRequest(HttpMethod.GET, "/admin/invitations.html?addInvitation=1&invitationType=newProjectInvitation&projectId=" + testDriveProject.getProjectId());
        invitationsAdminController.handleRequestInternal(request, response);
        then(ActionMessages.getMessages(request).getMessage("accessDenied")).isNotNull();

        try {
            createInvitationToCreateProject(adminRole.getId(), testDriveProject.getExternalId(), true);
            fail("Access denied expected");
        } catch (AccessDeniedException ignored) {
        }
    }

    public void user_cant_edit_invitation_without_necessary_permission() throws Exception {
        login(systemAdmin);
        String token = createInvitationToCreateProject("PROJECT_ADMIN", "_Root", false).getToken();

        SUser oleg = core.createUser("oleg");
        oleg.addRole(projectScope(testDriveProject.getProjectId()), adminRole);
        login(oleg);

        //try to open edit page
        newRequest(HttpMethod.GET, "/admin/invitations.html?editInvitation=1&projectId=_Root&token=" + token);
        invitationsAdminController.handleRequestInternal(request, response);
        then(ActionMessages.getMessages(request).getMessage("accessDenied")).isNotNull();

        //try to submit edit
        newRequest(HttpMethod.POST, "/admin/invitations.html?editInvitation=1&projectId=_Root&token=" + token);
        invitationsAdminController.handleRequestInternal(request, response);
        then(ActionMessages.getMessages(request).getMessage("accessDenied")).isNotNull();

        //try to remove
        newRequest(HttpMethod.POST, "/admin/invitations.html?removeInvitation=" + token);
        invitationsAdminController.handleRequestInternal(request, response);
        then(ActionMessages.getMessages(request).getMessage("accessDenied")).isNotNull();
    }

    private ModelAndView goToAfterRegistrationUrl() throws Exception {
        newRequest(HttpMethod.GET, InvitationsProceedController.PATH);
        return invitationsProceedController.doHandle(request, response);
    }

    private ModelAndView goToInvitationUrl(String token) throws Exception {
        newRequest(HttpMethod.GET, "/invitations.html?token=" + token);
        return invitationsController.doHandle(request, response);
    }

    private void assertRedirectTo(ModelAndView invitationResponse, String expectedRedirect) {
        then(invitationResponse.getView()).isInstanceOf(RedirectView.class);
        then(((RedirectView) invitationResponse.getView()).getUrl()).isEqualTo(expectedRedirect);
    }

    private void assertViewName(ModelAndView invitationResponse, String expectedViewName) {
        then((invitationResponse.getViewName())).isEqualTo(expectedViewName);
    }

    private void newRequest(HttpMethod method, String url) {
        if (session == null) session = new MockHttpSession();
        request = MockMvcRequestBuilders.request(method, url).session(session).buildRequest(new MockServletContext());
        response = new MockHttpServletResponse();
        ActionMessages messages = ActionMessages.getMessages(request);
        if (messages != null) messages.clearMessages();
        if (SessionUser.getUser(request) != null) securityContext.setAuthorityHolder(SessionUser.getUser(request));
    }

    private Invitation createInvitationToCreateProject(String role, String parentExtId, boolean multiuser) throws Exception {
        newRequest(HttpMethod.POST, "/admin/invitations.html?createInvitation=1");
        request.addParameter("name", "Create Project Invitation");
        request.addParameter("invitationType", createNewProjectInvitationType.getId());
        request.addParameter("projectId", parentExtId);
        request.addParameter("role", role);
        request.addParameter("multiuser", multiuser + "");
        invitationsAdminController.handleRequestInternal(request, response);
        if (ActionMessages.getMessages(request) != null && ActionMessages.getMessages(request).getMessage("accessDenied") != null) {
            throw new AccessDeniedException(securityContext.getAuthorityHolder(), ActionMessages.getMessages(request).getMessage("accessDenied"));
        }
        Element resp = FileUtil.parseDocument(new StringReader(response.getContentAsString()), false);
        String token = resp.getAttributeValue("token");

        return invitations.getInvitation(token);
    }

    private Invitation createInvitationToJoinProject(@Nullable String role, @Nullable String group, String projectExtId, boolean multiuser) throws Exception {
        newRequest(HttpMethod.POST, "/admin/invitations.html?createInvitation=1");
        request.addParameter("invitationType", joinProjectInvitationType.getId());
        request.addParameter("name", "Join Project Invitation");
        request.addParameter("projectId", projectExtId);
        if (role != null) {
            request.addParameter("addRole", "true");
            request.addParameter("role", role);
        }
        if (group != null) {
            request.addParameter("addToGroup", "true");
            request.addParameter("group", group);
        }
        request.addParameter("multiuser", multiuser + "");
        invitationsAdminController.handleRequestInternal(request, response);
        Element resp = FileUtil.parseDocument(new StringReader(response.getContentAsString()), false);
        String token = resp.getAttributeValue("token");
        return invitations.getInvitation(token);
    }

    private void login(SUser user) {
        logout();
        securityContext.setAuthorityHolder(user);
        if (request != null) SessionUser.setUser(request, user);
    }

    private void logout() {
        securityContext.clearContext();
        if (request != null && SessionUser.getUser(request) != null) SessionUser.removeUser(request);
    }
}

