package com.zhaow.restful.navigation.action;

import com.intellij.ide.util.gotoByName.ChooseByNameFilter;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class GotoRequestMappingServiceTypeFilter extends ChooseByNameFilter<ServiceType> {
    GotoRequestMappingServiceTypeFilter(final ChooseByNamePopup popup, GotoRequestMappingServiceTypeModel model, final Project project) {
        super(popup, model, GotoRequestMappingServiceTypeConfiguration.getInstance(project), project);
    }

    @Override
    @NotNull
    protected List<ServiceType> getAllFilterValues() {
        return Arrays.asList(ServiceType.values());
    }

    @Override
    protected String textForFilterValue(@NotNull ServiceType value) {
        return value.getDisplayName();
    }

    @Override
    protected Icon iconForFilterValue(@NotNull ServiceType value) {
        return null;
    }
}