package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.lang.reflect.InvocationTargetException;

import static org.jetbrains.teamcity.invitations.Invitations.TOKEN_SESSION_ATTR;

/**
 * TODO create extension point in the TeamCity core and use it instead of the interceptor
 */
public class InvitationInterceptor extends HandlerInterceptorAdapter {

    public InvitationInterceptor(ApplicationContext applicationContext) {
        try {
            Class<?> requestInterceptorsClass = Class.forName("jetbrains.buildServer.controllers.interceptors.RequestInterceptors");
            Object requestInterceptors = applicationContext.getBean(requestInterceptorsClass);
            requestInterceptorsClass.getMethod("addInterceptor", HandlerInterceptor.class).invoke(requestInterceptors, this);
        } catch (Exception e) {
            Loggers.SERVER.error("Invitations plugin failed to init", e);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        if (session == null) return true;

        Object tokenAttr = session.getAttribute(TOKEN_SESSION_ATTR);
        if (tokenAttr == null || !(tokenAttr instanceof InvitationProcessor)) return true;

        InvitationProcessor invitation = ((InvitationProcessor) tokenAttr);

        SUser user = SessionUser.getUser(request);
        if (user != null) {
            return invitation.userRegistered(user, request, response);
        }

        return true;
    }
}
