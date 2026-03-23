package com.zhaow.restful.method.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.psi.*;
import com.zhaow.restful.annotations.JaxrsHttpMethodAnnotation;
import com.zhaow.restful.annotations.SpringRequestMethodAnnotation;
import com.zhaow.restful.common.PsiMethodHelper;

import java.awt.datatransfer.StringSelection;
import java.util.Arrays;

/**
 * 生成并复制相对URL（不带域名）
 */
public class GenerateUrlAction extends SpringAnnotatedMethodAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor myEditor = e.getData(CommonDataKeys.EDITOR);
        PsiMethod psiMethod = getPsiMethod(e);
        if (psiMethod == null) {
            return;
        }

        // 生成相对URL
        String servicePath = PsiMethodHelper.create(psiMethod).buildServiceUriPathWithParams();

        CopyPasteManager.getInstance().setContents(new StringSelection(servicePath));
        if (myEditor != null) {
            showPopupBalloon("复制成功: " + servicePath, myEditor);
        }
    }
}
