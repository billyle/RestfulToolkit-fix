package com.zhaow.restful.navigation.action;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.gotoByName.CustomMatcherModel;
import com.intellij.ide.util.gotoByName.FilteringGotoByModel;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.zhaow.restful.common.spring.AntPathMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

/**
 * Model for "Go to | File" action with service type filtering
 */
public class GotoRequestMappingServiceTypeModel extends FilteringGotoByModel<ServiceType> implements DumbAware, CustomMatcherModel {

    protected GotoRequestMappingServiceTypeModel(@NotNull Project project, @NotNull ChooseByNameContributor[] contributors) {
        super(project, contributors);
    }

    @Nullable
    @Override
    protected ServiceType filterValueFor(NavigationItem item) {
        if (item instanceof RestServiceItem) {
            RestServiceItem restServiceItem = (RestServiceItem) item;
            if (restServiceItem.getDownstreamService() != null && !restServiceItem.getDownstreamService().isEmpty()) {
                return ServiceType.FEIGN;
            } else {
                return ServiceType.REST;
            }
        }
        return null;
    }

    @Override
    public String getPromptText() {
        return "Enter service URL path :";
    }

    @Override
    public String getNotInMessage() {
       return IdeBundle.message("label.no.matches.found.in.project");
    }

    @Override
    public String getNotFoundMessage() {
       return IdeBundle.message("label.no.matches.found");
    }

    @Override
    public char getCheckBoxMnemonic() {
        return SystemInfo.isMac?'P':'n';
    }

    @Override
    public boolean loadInitialCheckBoxState() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
        return propertiesComponent.isTrueValue("GoToRestService.OnlyCurrentModule");
    }

    @Override
    public void saveInitialCheckBoxState(boolean state) {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
        propertiesComponent.setValue("GoToRestService.OnlyCurrentModule", Boolean.toString(state));
    }

    @Nullable
    @Override
    public String getFullName(Object element) {
        return getElementName(element);
    }

    @NotNull
    @Override
    public String[] getSeparators() {
        return new String[]{"/","?"};
    }

    /** return null to hide checkbox panel */
    @Nullable
    @Override
    public String getCheckBoxName() {
        return "Only This Module";
    }

    @Override
    public boolean willOpenEditor() {
        return true;
    }

    @Override
    public boolean matches(@NotNull String popupItem, @NotNull String userPattern) {
        String pattern = userPattern;
        if(pattern.equals("/")) return true;
        MinusculeMatcher matcher = NameUtil.buildMatcher("*" + pattern, NameUtil.MatchingCaseSensitivity.NONE);
        boolean matches = matcher.matches(popupItem);
        if (!matches) {
            AntPathMatcher pathMatcher = new AntPathMatcher();
            matches = pathMatcher.match(popupItem,userPattern);
        }
        return matches;
    }

    @NotNull
    @Override
    public String removeModelSpecificMarkup(@NotNull String pattern) {
        return super.removeModelSpecificMarkup(pattern);
    }

    @Override
    public ListCellRenderer getListCellRenderer() {
        return super.getListCellRenderer();
    }
}