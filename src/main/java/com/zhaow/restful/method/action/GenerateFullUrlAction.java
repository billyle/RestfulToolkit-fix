package com.zhaow.restful.method.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.zhaow.restful.common.PsiMethodHelper;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.awt.datatransfer.StringSelection;
import java.util.List;

import static com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT;

/**
 * 生成并复制完整URL（带域名）
 */
public class GenerateFullUrlAction extends SpringAnnotatedMethodAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Module module = myModule(e);
        Editor myEditor = e.getData(CommonDataKeys.EDITOR);
        PsiMethod psiMethod = getPsiMethod(e);

        if (psiMethod != null) {
            String url = PsiMethodHelper.create(psiMethod).withModule(module).buildFullUrlWithParams();
            CopyPasteManager.getInstance().setContents(new StringSelection(url));
            if (myEditor != null) {
                showPopupBalloon("复制成功: " + url, myEditor);
            }
        }

        // 处理 Kotlin 函数
        PsiElement psiElement = e.getData(PSI_ELEMENT);
        if (psiElement instanceof KtNamedFunction) {
            KtNamedFunction ktNamedFunction = (KtNamedFunction) psiElement;
            PsiElement parentPsi = psiElement.getParent().getParent();
            if (parentPsi instanceof KtClassOrObject) {
                List<PsiMethod> psiMethods = LightClassUtilsKt.toLightMethods(ktNamedFunction);
                PsiMethod ktPsiMethod = psiMethods.get(0);
                String url = PsiMethodHelper.create(ktPsiMethod).withModule(module).buildFullUrlWithParams();
                CopyPasteManager.getInstance().setContents(new StringSelection(url));
                if (myEditor != null) {
                    showPopupBalloon("复制成功: " + url, myEditor);
                }
            }
        }
    }
}
