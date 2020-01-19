/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class InvitationLandingProvider {
    private final TeamCityCoreFacade core;
    private final CopyOnWriteArrayList<Function<Invitation, String>> customProviders = new CopyOnWriteArrayList<>();

    public InvitationLandingProvider(TeamCityCoreFacade core) {
        this.core = core;
    }

    public void registerCustomProvider(@NotNull Function<Invitation, String> provider) {
        customProviders.add(provider);
    }

    @NotNull
    public String getLanding(@NotNull Invitation invitation) {
        for (Function<Invitation, String> provider : customProviders) {
            String landing = core.runAsSystem(() -> provider.apply(invitation));
            if (landing != null) {
                Loggers.SERVER.info("Custom invitation landing found for the invitation " + invitation.describe(false) + ": " + landing);
                return landing;
            }
        }
        return core.getPluginResourcesPath("invitationLanding.jsp");
    }
}
