package com.zhaow.restful.method.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.zhaow.restful.common.PsiMethodHelper;
import com.zhaow.restful.method.Parameter;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.awt.datatransfer.StringSelection;
import java.util.List;

/**
 * 生成Request Body JSON 字符串
 */
public class GenerateQueryParamJsonAction extends SpringAnnotatedMethodAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor myEditor = e.getData(CommonDataKeys.EDITOR);
        PsiMethod psiMethod = getPsiMethod(e);

        if (psiMethod == null) {
            return;
        }

        PsiMethodHelper psiMethodHelper = PsiMethodHelper.create(psiMethod);
        List<Parameter> parameterList = psiMethodHelper.getParameterList();

        for (Parameter parameter : parameterList) {
            if (parameter.isRequestBodyFound()) {
                String queryJson = psiMethodHelper.buildRequestBodyJson(parameter);
                CopyPasteManager.getInstance().setContents(new StringSelection(queryJson));
                if (myEditor != null) {
                    showPopupBalloon("复制成功", myEditor);
                }
                break;
            }
        }
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }
}
