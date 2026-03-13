package com.zhaow.restful.common;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.zhaow.restful.common.resolver.FeignResolver;
import com.zhaow.restful.common.resolver.JaxrsResolver;
import com.zhaow.restful.common.resolver.ServiceResolver;
import com.zhaow.restful.common.resolver.SpringResolver;
import com.zhaow.restful.navigation.action.RestServiceItem;
import com.zhaow.restful.navigator.RestServiceProject;
import com.zhaow.utils.PluginLogger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务相关工具类
 */
public class ServiceHelper {
    PsiMethod psiMethod;

    public ServiceHelper(PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
    }

    public static List<RestServiceProject> buildRestServiceProjectListUsingResolver(Project project) {
        PluginLogger logger = new PluginLogger(ServiceHelper.class, project);
        logger.info("=== [ServiceHelper] buildRestServiceProjectListUsingResolver START ===");
        logger.info("[ServiceHelper] Project: " + project.getName());
        logger.info("[ServiceHelper] Project isInitialized: " + project.isInitialized() + ", isOpen: " + project.isOpen() + ", isDisposed: " + project.isDisposed());
        
        List<RestServiceProject> serviceProjectList = new ArrayList<>();

        Module[] modules = ModuleManager.getInstance(project).getModules();
        logger.info("[ServiceHelper] Found " + modules.length + " modules in project");
        
        for (Module module : modules) {
            logger.info("[ServiceHelper] Processing module: " + module.getName() + " (" + module.getModuleFilePath() + ")");
            List<RestServiceItem> restServices = buildRestServiceItemListUsingResolver(module);
            logger.info("[ServiceHelper] Module " + module.getName() + " has " + restServices.size() + " services");
            
            if (restServices.size() > 0) {
                RestServiceProject serviceProject = new RestServiceProject(module, restServices);
                serviceProjectList.add(serviceProject);
                logger.info("[ServiceHelper] Added RestServiceProject for module: " + module.getName());
            }
        }
        
        logger.info("=== [ServiceHelper] buildRestServiceProjectListUsingResolver END - Total projects: " + serviceProjectList.size() + " ===");
        return serviceProjectList;
    }

    public static List<RestServiceItem> buildRestServiceItemListUsingResolver(Module module) {
        PluginLogger logger = new PluginLogger(ServiceHelper.class, module.getProject());
        logger.info("--- buildRestServiceItemListUsingResolver for module: " + module.getName() + " START ---");

        List<RestServiceItem> itemList = new ArrayList<>();

        SpringResolver springResolver = new SpringResolver(module);
        JaxrsResolver jaxrsResolver = new JaxrsResolver(module);
        FeignResolver feignResolver = new FeignResolver(module);
        ServiceResolver[] resolvers = {springResolver,jaxrsResolver,feignResolver};

        for (ServiceResolver resolver : resolvers) {
            String resolverName = resolver.getClass().getSimpleName();
            logger.info("Running resolver: " + resolverName);
            List<RestServiceItem> allSupportedServiceItemsInModule = resolver.findAllSupportedServiceItemsInModule();
            logger.info("Resolver " + resolverName + " found " + allSupportedServiceItemsInModule.size() + " items");
            
            for (RestServiceItem item : allSupportedServiceItemsInModule) {
                logger.info("  Item: " + item.getName() + ", method: " + item.getMethod() + ", downstream: " + item.getDownstreamService());
            }
            
            itemList.addAll(allSupportedServiceItemsInModule);
        }

        logger.info("--- buildRestServiceItemListUsingResolver for module: " + module.getName() + " END - Total: " + itemList.size() + " ---");
        return itemList;
    }

    @NotNull
    public static List<RestServiceItem> buildRestServiceItemListUsingResolver(Project project) {
        List<RestServiceItem> itemList = new ArrayList<>();

        SpringResolver springResolver = new SpringResolver(project);
        JaxrsResolver jaxrsResolver = new JaxrsResolver(project);
        FeignResolver feignResolver = new FeignResolver(project);
        ServiceResolver[] resolvers = {springResolver,jaxrsResolver,feignResolver};
        for (ServiceResolver resolver : resolvers) {
            List<RestServiceItem> allSupportedServiceItemsInProject = resolver.findAllSupportedServiceItemsInProject();

            itemList.addAll(allSupportedServiceItemsInProject);
        }

        return itemList;
    }
}
