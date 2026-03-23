package com.zhaow.restful.method.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.zhaow.restful.action.AbstractBaseAction;
import com.zhaow.restful.annotations.FeignClientAnnotation;
import com.zhaow.restful.annotations.SpringControllerAnnotation;
import com.zhaow.restful.annotations.SpringRequestMethodAnnotation;
import com.zhaow.utils.PluginLogger;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.asJava.classes.KtLightClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.util.List;

/**
 * Restful method （restful 方法添加方法 ）
 */
public abstract class SpringAnnotatedMethodAction extends AbstractBaseAction {

    private PluginLogger LOG;

    /**
     * spring rest 方法被选中才触发
     */
    @Override
    public void update(AnActionEvent e) {
        if (LOG == null) {
            Project project = e.getProject();
            if (project != null) {
                LOG = new PluginLogger(getClass(), project);
            }
        }

        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);

        if (LOG != null) {
            LOG.info("[SpringAnnotatedMethodAction] update called, psiElement: " + (psiElement != null ? psiElement.getClass().getSimpleName() : "null"));
        }

        // 如果 PSI_ELEMENT 为 null，尝试从编辑器获取
        if (psiElement == null) {
            psiElement = getPsiElementFromEditor(e);
            if (LOG != null) {
                LOG.info("[SpringAnnotatedMethodAction] psiElement from editor: " + (psiElement != null ? psiElement.getClass().getSimpleName() : "null"));
            }
        }

        boolean visible = false;

        // 从 PSI 元素向上查找 PsiMethod
        PsiMethod psiMethod = findPsiMethod(psiElement);
        if (LOG != null) {
            LOG.info("[SpringAnnotatedMethodAction] psiMethod: " + (psiMethod != null ? psiMethod.getName() : "null"));
        }

        if (psiMethod != null) {
            boolean isRestController = isRestController(psiMethod.getContainingClass());
            boolean isFeignClient = isFeignClient(psiMethod.getContainingClass());
            boolean isRestfulMethod = isRestfulMethod(psiMethod);
            if (LOG != null) {
                LOG.info("[SpringAnnotatedMethodAction] isRestController: " + isRestController + ", isFeignClient: " + isFeignClient + ", isRestfulMethod: " + isRestfulMethod);
            }
            visible = (isRestController || isFeignClient || isRestfulMethod);
        }

        // 处理 Kotlin 函数
        if (psiElement instanceof KtNamedFunction) {
            KtNamedFunction ktNamedFunction = (KtNamedFunction) psiElement;
            PsiElement parentPsi = psiElement.getParent().getParent();
            if (parentPsi instanceof KtClassOrObject) {
                KtLightClass ktLightClass = LightClassUtilsKt.toLightClass(((KtClassOrObject) parentPsi));
                List<PsiMethod> psiMethods = LightClassUtilsKt.toLightMethods(ktNamedFunction);
                visible = (isRestController(ktLightClass) || isFeignClient(ktLightClass) || isRestfulMethod(psiMethods.get(0)));
            }
        }

        if (LOG != null) {
            LOG.info("[SpringAnnotatedMethodAction] visible: " + visible);
        }

        setActionPresentationVisible(e, visible);
    }

    /**
     * 从编辑器获取当前光标位置的 PSI 元素
     */
    private PsiElement getPsiElementFromEditor(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();

        if (editor == null || project == null) {
            return null;
        }

        // 通过 PsiDocumentManager 从编辑器文档获取 PSI 文件
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        PsiFile psiFile = psiDocumentManager.getPsiFile(editor.getDocument());

        if (LOG != null) {
            LOG.info("[SpringAnnotatedMethodAction] getPsiElementFromEditor - editor: true, psiFile: " + (psiFile != null));
        }

        if (psiFile == null) {
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        if (LOG != null && element != null) {
            LOG.info("[SpringAnnotatedMethodAction] getPsiElementFromEditor - element: " + element.getClass().getSimpleName() + ", text: " + element.getText());
        }
        return element;
    }

    /**
     * 从 PSI 元素向上查找 PsiMethod
     */
    private PsiMethod findPsiMethod(PsiElement element) {
        if (element == null) {
            return null;
        }
        if (element instanceof PsiMethod) {
            return (PsiMethod) element;
        }
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof PsiMethod) {
                return (PsiMethod) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private boolean isRestController(PsiClass containingClass) {
        if (containingClass == null || containingClass.getModifierList() == null) {
            return false;
        }
        PsiModifierList modifierList = containingClass.getModifierList();
        return modifierList.findAnnotation(SpringControllerAnnotation.REST_CONTROLLER.getQualifiedName()) != null ||
                modifierList.findAnnotation(SpringControllerAnnotation.CONTROLLER.getQualifiedName()) != null;
    }

    private boolean isFeignClient(PsiClass containingClass) {
        if (containingClass == null || containingClass.getModifierList() == null) {
            return false;
        }
        PsiModifierList modifierList = containingClass.getModifierList();
        return modifierList.findAnnotation(FeignClientAnnotation.FEIGN_CLIENT.getQualifiedName()) != null;
    }

    private boolean isRestfulMethod(PsiMethod psiMethod) {
        final PsiModifierList modifierList = psiMethod.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (SpringRequestMethodAnnotation annotation : SpringRequestMethodAnnotation.values()) {
            PsiAnnotation psiAnnotation = modifierList.findAnnotation(annotation.getQualifiedName());
            if (psiAnnotation != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前选中的 PsiMethod（从 PSI_ELEMENT 或编辑器光标位置）
     */
    protected PsiMethod getPsiMethod(AnActionEvent e) {
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (psiElement instanceof PsiMethod) {
            return (PsiMethod) psiElement;
        }

        // 如果 PSI_ELEMENT 为 null 或不是 PsiMethod，尝试从编辑器获取
        psiElement = getPsiElementFromEditor(e);
        if (psiElement != null) {
            return findPsiMethod(psiElement);
        }
        return null;
    }
}
