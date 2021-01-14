/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractInvitation implements Invitation {
    public static final String TOKEN_PARAM_NAME = Constants.SECURE_PROPERTY_PREFIX + "token";
    protected final String token;
    protected final boolean multi;
    protected final long createdByUserId;
    protected final SProject project;
    protected final String welcomeText;
    private final InvitationType type;
    private final String name;
    protected volatile boolean enabled;
    protected volatile String disabledText;

    protected AbstractInvitation(@NotNull SProject project, String name, @NotNull String token, boolean multi, InvitationType type, long createdByUserId,
                                 @NotNull String welcomeText) {
        this.token = token;
        this.multi = multi;
        this.type = type;
        this.name = name;
        this.createdByUserId = createdByUserId;
        this.project = project;
        this.welcomeText = welcomeText;
        this.enabled = true;
    }

    protected AbstractInvitation(Map<String, String> params, SProject project, InvitationType type) {
        this.name = params.get("name");
        this.enabled = !Boolean.valueOf(params.get("disabled"));
        this.token = params.get(TOKEN_PARAM_NAME);
        this.multi = Boolean.valueOf(params.get("multi"));
        this.createdByUserId = Long.parseLong(params.get("createdByUserId"));
        this.welcomeText = params.get("welcomeText");
        this.disabledText = params.get("disabledText");
        this.type = type;
        this.project = project;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public String getToken() {
        return token;
    }

    @NotNull
    @Override
    public ModelAndView processInvitationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView(type.getLandingPage(this));
        modelAndView.addObject("loggedInUser", SessionUser.getUser(request));
        modelAndView.addObject("proceedUrl", InvitationsProceedController.PATH + "?token=" + token);
        modelAndView.addObject("invitation", this);
        modelAndView.addObject("welcomeText", welcomeText);
        modelAndView.addObject("title", getName());
        return modelAndView;
    }

    @NotNull
    @Override
    public InvitationType getType() {
        return type;
    }

    @NotNull
    public Map<String, String> asMap() {
        Map<String, String> result = new HashMap<>();
        result.put("name", name);
        result.put("disabled", !enabled + "");
        if (!StringUtil.isEmptyOrSpaces(disabledText)) {
            result.put("disabledText", disabledText);
        }
        result.put("multi", multi + "");
        result.put("createdByUserId", createdByUserId + "");
        result.put("welcomeText", welcomeText);
        result.put(Constants.SECURE_PROPERTY_PREFIX + "token", token);
        return result;
    }

    @Override
    public boolean isReusable() {
        return multi;
    }

    @NotNull
    @Override
    public SProject getProject() {
        return project;
    }

    @NotNull
    public String getWelcomeText() {
        return welcomeText;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Nullable
    public String getDisabledText() {
        return disabledText;
    }
}
