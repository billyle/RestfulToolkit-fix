package com.zhaow.restful.method.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiUtil;
import com.zhaow.restful.common.PsiMethodHelper;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.awt.datatransfer.StringSelection;
import java.util.List;

/**
 * 生成查询参数
 */
public class GenerateQueryParamAction extends SpringAnnotatedMethodAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor myEditor = e.getData(CommonDataKeys.EDITOR);
        PsiMethod psiMethod = getPsiMethod(e);

        if (psiMethod != null) {
            String params = PsiMethodHelper.create(psiMethod).buildParamString();
            if (params != null && !params.isEmpty()) {
                CopyPasteManager.getInstance().setContents(new StringSelection(params));
                if (myEditor != null) {
                    showPopupBalloon("复制成功: " + params, myEditor);
                }
            } else {
                if (myEditor != null) {
                    showPopupBalloon("没有找到查询参数", myEditor);
                }
            }
        }
    }
}
