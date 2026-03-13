/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.zhaow.restful.navigator;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.zhaow.restful.common.ServiceHelper;
import com.zhaow.restful.navigation.action.RestServiceItem;
import com.zhaow.utils.PluginLogger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "RestServiceProjectsManager")
public class RestServiceProjectsManager implements PersistentStateComponent<RestServicesNavigatorState>, Disposable, ProjectComponent {
  protected final Project myProject;
  private final PluginLogger LOG;

  private RestServicesNavigatorState myState = new RestServicesNavigatorState();

  public static RestServiceProjectsManager getInstance(Project p) {
    return p.getComponent(RestServiceProjectsManager.class);
  }

  public RestServiceProjectsManager(Project project) {
      myProject = project;
      this.LOG = new PluginLogger(RestServiceProjectsManager.class, project);
      LOG.info("RestServiceProjectsManager initialized");
  }


  @Override
  public void dispose() {

  }

  @Nullable
  @Override
  public RestServicesNavigatorState getState() {
    return null;
  }

  @Override
  public void loadState(RestServicesNavigatorState state) {

  }

  public List<RestServiceProject> getServiceProjects() {
    LOG.info("=== [RestServiceProjectsManager] getServiceProjects START ===");
    LOG.info("[RestServiceProjectsManager] Current thread: " + Thread.currentThread().getName());
    LOG.info("[RestServiceProjectsManager] isDispatchThread: " + com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread());
    LOG.info("[RestServiceProjectsManager] Project isOpen: " + myProject.isOpen() + ", isDisposed: " + myProject.isDisposed() + ", isInitialized: " + myProject.isInitialized());
    
    // 检查项目是否可用
    if (myProject.isDisposed() || !myProject.isOpen()) {
        LOG.warn("[RestServiceProjectsManager] Project is disposed or not open, returning empty list");
        return new ArrayList<>();
    }
    
    List<RestServiceProject> list = null;
    
    try {
        // 检查是否在 dumb mode（索引重建中）
        boolean isDumb = DumbService.getInstance(myProject).isDumb();
        LOG.info("[RestServiceProjectsManager] Is in dumb mode: " + isDumb);
        
        if (isDumb) {
            LOG.warn("[RestServiceProjectsManager] Still in dumb mode, services may not be fully available yet. Running in smart mode...");
            // 在哑模式下，等待智能模式后再获取
            list = DumbService.getInstance(myProject).runReadActionInSmartMode(() -> {
                LOG.info("[RestServiceProjectsManager] Now in smart mode, calling ServiceHelper");
                return ServiceHelper.buildRestServiceProjectListUsingResolver(myProject);
            });
        } else {
            LOG.info("[RestServiceProjectsManager] In smart mode, calling service resolver directly");
            list = ServiceHelper.buildRestServiceProjectListUsingResolver(myProject);
        }
    } catch (Exception e) {
        LOG.error("[RestServiceProjectsManager] Error getting service projects: " + e.getMessage());
        LOG.error("[RestServiceProjectsManager] Exception stack trace: " + e.toString());
        list = new ArrayList<>();
    }
    
    LOG.info("[RestServiceProjectsManager] getServiceProjects returned " + (list != null ? list.size() : 0) + " projects");
    if (list != null && !list.isEmpty()) {
        for (RestServiceProject project : list) {
            int serviceCount = project.serviceItems != null ? project.serviceItems.size() : 0;
            LOG.info("[RestServiceProjectsManager]   Project: " + project.getModuleName() + ", services: " + serviceCount);
            if (project.serviceItems != null) {
                for (RestServiceItem item : project.serviceItems) {
                    LOG.info("[RestServiceProjectsManager]     - " + item.getName() + " [" + item.getMethod() + "] " + item.getFullUrl());
                }
            }
        }
    } else {
        LOG.warn("[RestServiceProjectsManager] No service projects found! This might be because:");
        LOG.warn("[RestServiceProjectsManager]   1. No Controller/FeignClient classes in the project");
        LOG.warn("[RestServiceProjectsManager]   2. Index is not fully built yet");
        LOG.warn("[RestServiceProjectsManager]   3. Wrong search scope (e.g., only main module but controllers are in test module)");
    }
    LOG.info("=== [RestServiceProjectsManager] getServiceProjects END ===");
    return list;
  }
/*
  public boolean hasProjects() {
    if (! myProject.isInitialized()) {
      return false;
    }
    System.out.println("======hasProjects=====");
    return getServiceProjects().size() > 0;
  }*/

  public void forceUpdateAllProjects() {

  }
}
