package com.zhaow.restful.navigator;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.jetbrains.annotations.NotNull;

public class MyToolwindowListener implements ToolWindowManagerListener {
    private final Project project;
    private final RestServicesNavigator servicesNavigator;

    public MyToolwindowListener(Project project) {
        this.project = project;
        RestServicesNavigator servicesNavigator = RestServicesNavigator.getInstance(project);
        this.servicesNavigator = servicesNavigator;
    }

    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        servicesNavigator.stateChanged();
    }
}