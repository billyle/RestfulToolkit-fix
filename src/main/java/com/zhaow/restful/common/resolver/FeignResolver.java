package com.zhaow.restful.common.resolver;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.zhaow.restful.annotations.FeignClientAnnotation;
import com.zhaow.restful.annotations.SpringRequestMethodAnnotation;
import com.zhaow.restful.common.PsiAnnotationHelper;
import com.zhaow.restful.method.RequestPath;
import com.zhaow.restful.navigation.action.RestServiceItem;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FeignResolver extends BaseServiceResolver {
    
    public FeignResolver(Project project) {
        myProject = project;
    }
    
    @Override
    public List<RestServiceItem> getRestServiceItemList(Project project, GlobalSearchScope globalSearchScope) {
        List<RestServiceItem> itemList = new ArrayList<>();
        
        // 查找所有标注了 @FeignClient 的接口
        Collection<PsiAnnotation> feignClientAnnotations = JavaAnnotationIndex.getInstance()
                .get(FeignClientAnnotation.FEIGN_CLIENT.getShortName(), project, globalSearchScope);
        
        for (PsiAnnotation feignClientAnnotation : feignClientAnnotations) {
            PsiModifierList psiModifierList = (PsiModifierList) feignClientAnnotation.getParent();
            PsiElement psiElement = psiModifierList.getParent();
            
            if (!(psiElement instanceof PsiClass)) {
                continue;
            }
            
            PsiClass feignInterface = (PsiClass) psiElement;
            Module module = ModuleUtilCore.findModuleForPsiElement(feignInterface);
            
            // 获取 FeignClient 的 path 和 name/value 属性
            String feignPath = getAnnotationAttributeValue(feignClientAnnotation, "path");
            if (feignPath == null) {
                feignPath = "/";
            }
            if (!feignPath.startsWith("/")) {
                feignPath = "/" + feignPath;
            }
            
            String downstreamServiceName = getAnnotationAttributeValue(feignClientAnnotation, "name");
            if (downstreamServiceName == null) {
                downstreamServiceName = getAnnotationAttributeValue(feignClientAnnotation, "value");
            }
            
            // 处理接口中的方法
            PsiMethod[] methods = feignInterface.getMethods();
            for (PsiMethod method : methods) {
                // 获取方法上的 Spring RequestMapping 注解
                RequestPath[] methodRequestPaths = getMethodRequestPaths(method);
                
                for (RequestPath methodRequestPath : methodRequestPaths) {
                    String fullPath = concatenatePaths(feignPath, methodRequestPath.getPath());
                    
                    RestServiceItem item = createRestServiceItem(method, fullPath, methodRequestPath);
                    if (module != null) {
                        item.setModule(module);
                    }
                    
                    // 添加下游服务名称信息到 URL 中（用于显示）
                    if (downstreamServiceName != null) {
                        item.setDownstreamService(downstreamServiceName);
                    }
                    
                    itemList.add(item);
                }
            }
        }
        
        return itemList;
    }
    
    private RequestPath[] getMethodRequestPaths(PsiMethod method) {
        List<RequestPath> requestPaths = new ArrayList<>();
        PsiAnnotation[] annotations = method.getModifierList().getAnnotations();
        
        for (PsiAnnotation annotation : annotations) {
            for (SpringRequestMethodAnnotation mappingAnnotation : SpringRequestMethodAnnotation.values()) {
                if (mappingAnnotation.getQualifiedName().equals(annotation.getQualifiedName())) {
                    String defaultValue = "/";
                    List<RequestPath> paths = getRequestMappings(annotation, defaultValue);
                    requestPaths.addAll(paths);
                }
            }
        }
        
        return requestPaths.toArray(new RequestPath[0]);
    }
    
    private List<RequestPath> getRequestMappings(PsiAnnotation annotation, String defaultValue) {
        List<RequestPath> mappingList = new ArrayList<>();
        
        SpringRequestMethodAnnotation requestAnnotation = SpringRequestMethodAnnotation.getByQualifiedName(annotation.getQualifiedName());
        if (requestAnnotation == null) {
            return new ArrayList<>();
        }
        
        List<String> methodList;
        if (requestAnnotation.methodName() != null) {
            methodList = Arrays.asList(requestAnnotation.methodName());
        } else {
            methodList = PsiAnnotationHelper.getAnnotationAttributeValues(annotation, "method");
        }
        
        List<String> pathList = PsiAnnotationHelper.getAnnotationAttributeValues(annotation, "value");
        if (pathList.isEmpty()) {
            pathList = PsiAnnotationHelper.getAnnotationAttributeValues(annotation, "path");
        }
        
        if (pathList.isEmpty()) {
            pathList.add(defaultValue);
        }
        
        if (!methodList.isEmpty()) {
            for (String method : methodList) {
                for (String path : pathList) {
                    mappingList.add(new RequestPath(path, method));
                }
            }
        } else {
            for (String path : pathList) {
                mappingList.add(new RequestPath(path, null));
            }
        }
        
        return mappingList;
    }
    
    private String concatenatePaths(String basePath, String methodPath) {
        if (basePath == null) basePath = "";
        if (methodPath == null) methodPath = "";
        
        if (basePath.equals("/") && methodPath.startsWith("/")) {
            return methodPath;
        }
        
        if (basePath.endsWith("/") && methodPath.startsWith("/")) {
            return basePath + methodPath.substring(1);
        }
        
        if (!basePath.endsWith("/") && !methodPath.startsWith("/")) {
            return basePath + "/" + methodPath;
        }
        
        return basePath + methodPath;
    }
    
    private String getAnnotationAttributeValue(PsiAnnotation annotation, String attributeName) {
        PsiAnnotationMemberValue attributeValue = annotation.findDeclaredAttributeValue(attributeName);
        if (attributeValue instanceof PsiLiteralExpression) {
            Object value = ((PsiLiteralExpression) attributeValue).getValue();
            return value != null ? value.toString() : null;
        }
        return null;
    }
}