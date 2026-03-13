package com.zhaow.restful.common.resolver;


import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.zhaow.restful.method.RequestPath;
import com.zhaow.restful.navigation.action.RestServiceItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseServiceResolver implements ServiceResolver{
    Module myModule;
    Project myProject;

    @Override
    public List<RestServiceItem> findAllSupportedServiceItemsInModule() {
        List<RestServiceItem> itemList = new ArrayList<>();
        if (myModule == null) {
            return itemList;
        }

        // 使用模块范围 + 依赖库范围，确保能扫描到所有相关类
        GlobalSearchScope moduleWithDependencies = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule);

        itemList = getRestServiceItemList(myModule.getProject(), moduleWithDependencies);
        return itemList;
    }


    public abstract List<RestServiceItem> getRestServiceItemList(Project project, GlobalSearchScope globalSearchScope) ;

    @Override
    public List<RestServiceItem> findAllSupportedServiceItemsInProject() {
        List<RestServiceItem> itemList = null;
        if(myProject == null && myModule != null){
            myProject = myModule.getProject();
        }

        if (myProject == null) {
            return new ArrayList<>();
        }

        GlobalSearchScope globalSearchScope = GlobalSearchScope.projectScope(myProject);

/*        List<PsiMethod> psiMethodList = this.getServicePsiMethodList(myProject, globalSearchScope);

        for (PsiMethod psiMethod : psiMethodList) {
            List<RestServiceItem> singleMethodServices = getServiceItemList(psiMethod);

            itemList.addAll(singleMethodServices);

        }*/

        itemList = getRestServiceItemList(myProject, globalSearchScope);

        return itemList;

    }

    @NotNull
    protected RestServiceItem createRestServiceItem(PsiElement psiMethod, String fullPath, RequestPath requestMapping) {
        RestServiceItem item = new RestServiceItem(psiMethod, requestMapping.getMethod(), fullPath);
        if (myModule != null) {
            item.setModule(myModule);
        }
        return item;
    }

/*
    protected abstract List<RestServiceItem> getServiceItemList(PsiMethod psiMethod);

    protected abstract List<PsiMethod> getServicePsiMethodList(Project myProject, GlobalSearchScope globalSearchScope);*/
}
