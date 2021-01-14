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

import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.Permissions;
import jetbrains.buildServer.users.impl.UserEx;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdditionalPermissionsUserWrapper {
    private final UserEx delegate;
    private final Map<String, List<Permission>> additionalPermissions;
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public AdditionalPermissionsUserWrapper(UserEx originalUser, @NotNull Map<String, List<Permission>> additionalPermissions) {
        this.delegate = originalUser;
        this.additionalPermissions = new ConcurrentHashMap<>(additionalPermissions);
    }

    public UserEx getWrappedUser() {
        return (UserEx) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{UserEx.class},
                (proxy, method, args) -> {
                    if (enabled.get()) {
                        switch (method.getName()) {
                            case "isPermissionGrantedForProject":
                                List<Permission> projectPermissions = additionalPermissions.get(args[0]);
                                if (projectPermissions != null && projectPermissions.contains(args[1])) {
                                    return true;
                                } else {
                                    return invoke(method, args);
                                }

                            case "isPermissionGrantedForAnyProject":
                                if (additionalPermissions.values().stream().anyMatch(list -> list.contains(args[0]))) {
                                    return true;
                                } else {
                                    return invoke(method, args);
                                }

                            case "getPermissionsGrantedForProject": {
                                projectPermissions = additionalPermissions.get(args[0]);
                                if (projectPermissions != null) {
                                    List<Permission> base = ((Permissions) invoke(method, args)).toList();
                                    base.addAll(projectPermissions);
                                    return new Permissions(base);
                                } else {
                                    return invoke(method, args);
                                }
                            }

                            default:
                                return invoke(method, args);
                        }
                    } else {
                        return invoke(method, args);
                    }
                });
    }

    private Object invoke(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public void disable() {
        enabled.set(false);
    }
}
