package com.zhaow.restful.navigation.action;

import com.intellij.ide.util.gotoByName.ChooseByNameFilterConfiguration;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;

/**
 * Configuration for service type filtering popup in "Go to | Service" action.
 *
 * @author zhaow
 */
@State(name = "GotoRequestMappingServiceTypeConfiguration", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
class GotoRequestMappingServiceTypeConfiguration extends ChooseByNameFilterConfiguration<ServiceType> {
  /**
   * Get configuration instance
   *
   * @param project a project instance
   * @return a configuration instance
   */
  public static GotoRequestMappingServiceTypeConfiguration getInstance(Project project) {
    return ServiceManager.getService(project, GotoRequestMappingServiceTypeConfiguration.class);
  }

  @Override
  protected String nameForElement(ServiceType type) {
    return type.name();
  }
}