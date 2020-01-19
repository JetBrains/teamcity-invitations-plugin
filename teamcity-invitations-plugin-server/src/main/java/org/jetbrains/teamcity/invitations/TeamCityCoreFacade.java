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

import jetbrains.buildServer.groups.SUserGroup;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public interface TeamCityCoreFacade {

    //ROLES
    @Nullable
    Role findRoleById(String roleId);

    @NotNull
    List<Role> getAvailableRoles();

    @NotNull
    Collection<SUserGroup> getAvailableGroups();

    @Nullable
    SUserGroup findGroup(String groupKey);

    @Nullable
    SUser getUser(long userId);

    @NotNull
    AuthorityHolder getLoggedInUser();

    void addRole(@NotNull SUser user, @NotNull Role role, @NotNull String projectId);

    void assignToGroup(@NotNull SUser user, @NotNull SUserGroup group);

    //PROJECTS
    @NotNull
    SProject createProject(@NotNull String parentExtId, @NotNull String name);

    @Nullable
    SProject findProjectByExtId(@Nullable String projectExtId);

    @Nullable
    SProject findProjectByIntId(String projectIntId);

    @NotNull
    List<SProject> getActiveProjects();

    void persist(@NotNull SProject project, @NotNull String description);

    <T> T runAsSystem(Supplier<T> action);

    //Plugins
    @NotNull
    String getPluginResourcesPath(@NotNull String path);

}
