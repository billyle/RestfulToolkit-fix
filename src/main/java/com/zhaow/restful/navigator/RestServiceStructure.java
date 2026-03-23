package com.zhaow.restful.navigator;


import com.intellij.icons.AllIcons;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.zhaow.utils.PluginLogger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.CachingSimpleNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.util.OpenSourceUtil;
import com.zhaow.restful.common.KtFunctionHelper;
import com.zhaow.restful.common.PsiMethodHelper;
import com.zhaow.restful.common.ToolkitIcons;
import com.zhaow.restful.method.HttpMethod;
import com.zhaow.restful.navigation.action.RestServiceItem;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.util.*;
import java.util.List;
import javax.swing.SwingUtilities;

public class RestServiceStructure extends SimpleTreeStructure {
    private final PluginLogger LOG;
    private final Project myProject;
    private final RestServiceProjectsManager myProjectsManager;
    RestServiceDetail myRestServiceDetail;
    private SimpleTree myTree;
    private DefaultTreeModel myTreeModel;
    private RootNode myRoot = new RootNode();
    private DefaultMutableTreeNode myRootTreeNode;
    private int serviceCount = 0;
    private boolean pendingRefresh = false;

    // 模块节点列表
    private List<ModuleNode> moduleNodes = new ArrayList<>();

    public RestServiceStructure(Project project,
                                RestServiceProjectsManager projectsManager,
                                SimpleTree tree) {
        myProject = project;
        myProjectsManager = projectsManager;
        myTree = tree;
        myRestServiceDetail = project.getComponent(RestServiceDetail.class);
        this.LOG = new PluginLogger(RestServiceStructure.class, project);

        configureTree(tree);
    }

    public static <T extends BaseSimpleNode> List<T> getSelectedNodes(SimpleTree tree, Class<T> nodeClass) {
        final List<T> filtered = new ArrayList<>();
        for (SimpleNode node : getSelectedNodes(tree)) {
            if ((nodeClass != null) && (!nodeClass.isInstance(node))) {
                filtered.clear();
                break;
            }
            //noinspection unchecked
            filtered.add((T) node);
        }
        return filtered;
    }

    private static List<SimpleNode> getSelectedNodes(SimpleTree tree) {
        List<SimpleNode> nodes = new ArrayList<>();
        TreePath[] treePaths = tree.getSelectionPaths();
        if (treePaths != null) {
            for (TreePath treePath : treePaths) {
                nodes.add(tree.getNodeFor(treePath));
            }
        }
        return nodes;
    }

    private void configureTree(SimpleTree tree) {
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);

        myRootTreeNode = new DefaultMutableTreeNode(myRoot);
        myTreeModel = new DefaultTreeModel(myRootTreeNode);
        tree.setModel(myTreeModel);

        // 设置树的选择监听器
        tree.addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            LOG.info("[TreeSelection] Selection event, path: " + path);
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();
                LOG.info("[TreeSelection] userObject class: " + (userObject != null ? userObject.getClass().getSimpleName() : "null"));
                if (userObject instanceof BaseSimpleNode) {
                    LOG.info("[TreeSelection] Calling handleSelection in runReadAction");
                    // 需要在读操作中执行，因为会访问 PSI 和索引
                    ApplicationManager.getApplication().runReadAction(() -> {
                        try {
                            ((BaseSimpleNode) userObject).handleSelection(tree);
                        } catch (Exception ex) {
                            LOG.error("Error handling selection", ex);
                        }
                    });
                }
            }
        });

        // 设置右键菜单
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(java.awt.event.MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    tree.setSelectionPath(path);
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    Object userObject = node.getUserObject();
                    if (userObject instanceof BaseSimpleNode) {
                        ((BaseSimpleNode) userObject).showPopupMenu(tree, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    @NotNull
    @Override
    public RootNode getRootElement() {
        return myRoot;
    }

    public void update() {
        List<RestServiceProject> projects = RestServiceProjectsManager.getInstance(myProject).getServiceProjects();
        updateProjects(projects);
    }

    public void updateProjects(List<RestServiceProject> projects) {
        serviceCount = 0;
        moduleNodes.clear();

        // 按模块分组
        Map<String, ModuleData> modulesMap = new TreeMap<>();

        if (projects != null) {
            for (RestServiceProject project : projects) {
                String moduleName = project.getModuleName();
                ModuleData moduleData = modulesMap.computeIfAbsent(moduleName, k -> new ModuleData(moduleName));

                if (project.serviceItems != null) {
                    for (RestServiceItem item : project.serviceItems) {
                        serviceCount++;
                        String downstream = item.getDownstreamService();
                        if (downstream != null && !downstream.isEmpty()) {
                            // Feign 服务
                            moduleData.feignCount++;
                            moduleData.feignServices.computeIfAbsent(downstream, k -> new ArrayList<>()).add(item);
                        } else {
                            // Rest 服务
                            moduleData.restCount++;
                            moduleData.restServices.add(item);
                        }
                    }
                }
            }
        }

        // 创建模块节点
        for (ModuleData moduleData : modulesMap.values()) {
            ModuleNode moduleNode = new ModuleNode(myRoot, moduleData);
            moduleNodes.add(moduleNode);
        }

        // Refresh the tree UI
        SwingUtilities.invokeLater(() -> {
            if (myTree == null) {
                return;
            }

            Component parent = myTree.getParent();
            while (parent != null && !(parent instanceof ToolWindowEx)) {
                parent = parent.getParent();
            }

            if (parent instanceof ToolWindowEx) {
                ToolWindowEx toolWindow = (ToolWindowEx) parent;
                if (!toolWindow.isVisible()) {
                    pendingRefresh = true;
                    return;
                }
            }

            performTreeRefresh();
            pendingRefresh = false;
        });
    }

    private void performTreeRefresh() {
        if (myTree == null || myTreeModel == null) {
            return;
        }

        myTree.setRootVisible(true);
        myTree.setShowsRootHandles(true);

        rebuildTreeNodes();

        // 默认展开一级节点
        expandFirstLevel();
        myTree.revalidate();
        myTree.repaint();
    }

    private void rebuildTreeNodes() {
        myRootTreeNode.removeAllChildren();

        for (ModuleNode moduleNode : moduleNodes) {
            DefaultMutableTreeNode moduleTreeNode = new DefaultMutableTreeNode(moduleNode);

            // 添加 REST 类型节点
            if (moduleNode.restTypeNode != null && !moduleNode.restTypeNode.isEmpty()) {
                DefaultMutableTreeNode restTypeTreeNode = new DefaultMutableTreeNode(moduleNode.restTypeNode);
                for (ServiceNode serviceNode : moduleNode.restTypeNode.serviceNodes) {
                    restTypeTreeNode.add(new DefaultMutableTreeNode(serviceNode));
                }
                moduleTreeNode.add(restTypeTreeNode);
            }

            // 添加 Feign 类型节点
            if (moduleNode.feignTypeNode != null && !moduleNode.feignTypeNode.isEmpty()) {
                DefaultMutableTreeNode feignTypeTreeNode = new DefaultMutableTreeNode(moduleNode.feignTypeNode);
                for (FeignDownstreamNode downstreamNode : moduleNode.feignTypeNode.downstreamNodes) {
                    DefaultMutableTreeNode downstreamTreeNode = new DefaultMutableTreeNode(downstreamNode);
                    for (ServiceNode serviceNode : downstreamNode.serviceNodes) {
                        downstreamTreeNode.add(new DefaultMutableTreeNode(serviceNode));
                    }
                    feignTypeTreeNode.add(downstreamTreeNode);
                }
                moduleTreeNode.add(feignTypeTreeNode);
            }

            myRootTreeNode.add(moduleTreeNode);
        }

        myRootTreeNode.setUserObject(myRoot);
        myTreeModel.reload();
    }

    private void expandFirstLevel() {
        // 展开根节点
        myTree.expandPath(new TreePath(myRootTreeNode));
        // 展开一级子节点（模块节点）
        for (int i = 0; i < myRootTreeNode.getChildCount(); i++) {
            TreeNode child = myRootTreeNode.getChildAt(i);
            TreePath path = new TreePath(new Object[]{myRootTreeNode, child});
            myTree.expandPath(path);
        }
    }

    public void onToolWindowBecameVisible() {
        if (pendingRefresh) {
            SwingUtilities.invokeLater(() -> {
                performTreeRefresh();
                pendingRefresh = false;
            });
        }
    }

    public void updateFrom(SimpleNode node) {
        if (node == null) {
            return;
        }
        SwingUtilities.invokeLater(this::performTreeRefresh);
    }

    private void resetRestServiceDetail() {
        if (myRestServiceDetail == null) {
            return;
        }
        myRestServiceDetail.resetRequestTabbedPane();
        myRestServiceDetail.setMethodValue(HttpMethod.GET.name());
        myRestServiceDetail.setUrlValue("URL");
        myRestServiceDetail.initTab();
    }

    // ==================== 数据类 ====================

    private static class ModuleData {
        String moduleName;
        int restCount = 0;
        int feignCount = 0;
        List<RestServiceItem> restServices = new ArrayList<>();
        Map<String, List<RestServiceItem>> feignServices = new TreeMap<>();

        ModuleData(String moduleName) {
            this.moduleName = moduleName;
        }

        int totalCount() {
            return restCount + feignCount;
        }
    }

    // ==================== 节点类定义 ====================

    public abstract class BaseSimpleNode extends CachingSimpleNode {
        protected BaseSimpleNode(SimpleNode aParent) {
            super(aParent);
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return new SimpleNode[0];
        }

        public String getMenuId() {
            return null;
        }

        @Override
        public void handleSelection(SimpleTree tree) {
            resetRestServiceDetail();
        }

        @Override
        public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
        }

        public void showPopupMenu(SimpleTree tree, int x, int y) {
            JPopupMenu popup = new JPopupMenu();

            // 展开子节点
            JMenuItem expandItem = new JMenuItem("展开子节点", AllIcons.Actions.Expandall);
            expandItem.addActionListener(e -> expandChildren(tree));
            popup.add(expandItem);

            // 收起子节点
            JMenuItem collapseItem = new JMenuItem("收起子节点", AllIcons.Actions.Collapseall);
            collapseItem.addActionListener(e -> collapseChildren(tree));
            popup.add(collapseItem);

            popup.addSeparator();

            // 复制为 Markdown
            JMenuItem copyItem = new JMenuItem("复制为 Markdown", AllIcons.Actions.Copy);
            copyItem.addActionListener(e -> copyAsMarkdown());
            popup.add(copyItem);

            popup.show(tree, x, y);
        }

        protected void expandChildren(SimpleTree tree) {
            TreePath path = tree.getSelectionPath();
            if (path != null) {
                expandAllChildren(tree, path);
            }
        }

        protected void collapseChildren(SimpleTree tree) {
            TreePath path = tree.getSelectionPath();
            if (path != null) {
                collapseAllChildren(tree, path);
            }
        }

        private void expandAllChildren(JTree tree, TreePath parent) {
            TreeNode node = (TreeNode) parent.getLastPathComponent();
            for (int i = 0; i < node.getChildCount(); i++) {
                TreeNode child = node.getChildAt(i);
                TreePath path = parent.pathByAddingChild(child);
                tree.expandPath(path);
                expandAllChildren(tree, path);
            }
        }

        private void collapseAllChildren(JTree tree, TreePath parent) {
            TreeNode node = (TreeNode) parent.getLastPathComponent();
            for (int i = 0; i < node.getChildCount(); i++) {
                TreeNode child = node.getChildAt(i);
                TreePath path = parent.pathByAddingChild(child);
                collapseAllChildren(tree, path);
                tree.collapsePath(path);
            }
        }

        public void copyAsMarkdown() {
            StringBuilder sb = new StringBuilder();
            buildMarkdown(sb, 0);
            CopyPasteManager.getInstance().setContents(new StringSelection(sb.toString()));
        }

        protected void buildMarkdown(StringBuilder sb, int level) {
            String indent = "  ".repeat(Math.max(0, level));
            sb.append(indent).append("- ").append(getName()).append("\n");
        }
    }

    public class RootNode extends BaseSimpleNode {
        protected RootNode() {
            super(null);
            getTemplatePresentation().setIcon(AllIcons.Actions.ModuleDirectory);
            setIcon(AllIcons.Actions.ModuleDirectory);
        }

        @Override
        public String getName() {
            return "Found " + serviceCount + " services";
        }

        @Override
        public void showPopupMenu(SimpleTree tree, int x, int y) {
            JPopupMenu popup = new JPopupMenu();

            JMenuItem expandAllItem = new JMenuItem("展开所有", AllIcons.Actions.Expandall);
            expandAllItem.addActionListener(e -> {
                for (int i = 0; i < myRootTreeNode.getChildCount(); i++) {
                    TreeNode child = myRootTreeNode.getChildAt(i);
                    TreePath path = new TreePath(new Object[]{myRootTreeNode, child});
                    expandAllChildren(tree, path);
                }
            });
            popup.add(expandAllItem);

            JMenuItem collapseAllItem = new JMenuItem("收起所有", AllIcons.Actions.Collapseall);
            collapseAllItem.addActionListener(e -> {
                for (int i = 0; i < myRootTreeNode.getChildCount(); i++) {
                    tree.collapseRow(i + 1);
                }
            });
            popup.add(collapseAllItem);

            popup.addSeparator();

            JMenuItem copyItem = new JMenuItem("复制为 Markdown", AllIcons.Actions.Copy);
            copyItem.addActionListener(e -> copyAsMarkdown());
            popup.add(copyItem);

            popup.show(tree, x, y);
        }

        private void expandAllChildren(JTree tree, TreePath parent) {
            TreeNode node = (TreeNode) parent.getLastPathComponent();
            tree.expandPath(parent);
            for (int i = 0; i < node.getChildCount(); i++) {
                TreeNode child = node.getChildAt(i);
                TreePath path = parent.pathByAddingChild(child);
                expandAllChildren(tree, path);
            }
        }

        @Override
        protected void buildMarkdown(StringBuilder sb, int level) {
            sb.append("# ").append(getName()).append("\n\n");
            for (ModuleNode moduleNode : moduleNodes) {
                moduleNode.buildMarkdown(sb, 0);
            }
        }
    }

    // 模块节点
    public class ModuleNode extends BaseSimpleNode {
        private String moduleName;
        private int restCount = 0;
        private int feignCount = 0;
        ServiceTypeNode restTypeNode;
        ServiceTypeNode feignTypeNode;

        public ModuleNode(SimpleNode parent, ModuleData data) {
            super(parent);
            this.moduleName = data.moduleName;
            this.restCount = data.restCount;
            this.feignCount = data.feignCount;

            getTemplatePresentation().setIcon(ToolkitIcons.MODULE);
            setIcon(ToolkitIcons.MODULE);

            // 对服务排序
            data.restServices.sort(Comparator.comparing(item -> item.getUrl() != null ? item.getUrl() : ""));
            for (List<RestServiceItem> items : data.feignServices.values()) {
                items.sort(Comparator.comparing(item -> item.getUrl() != null ? item.getUrl() : ""));
            }

            // 创建 Rest 类型节点
            if (!data.restServices.isEmpty()) {
                restTypeNode = new ServiceTypeNode(this, "REST", data.restServices, false);
            }

            // 创建 Feign 类型节点
            if (!data.feignServices.isEmpty()) {
                feignTypeNode = new ServiceTypeNode(this, "Feign", null, true);
                for (Map.Entry<String, List<RestServiceItem>> entry : data.feignServices.entrySet()) {
                    FeignDownstreamNode downstreamNode = new FeignDownstreamNode(feignTypeNode, entry.getKey(), entry.getValue());
                    feignTypeNode.addDownstreamNode(downstreamNode);
                }
            }
        }

        @Override
        public String getName() {
            return moduleName + " (" + (restCount + feignCount) + ")";
        }

        @Override
        protected void buildMarkdown(StringBuilder sb, int level) {
            String indent = "  ".repeat(Math.max(0, level));
            sb.append(indent).append("- **").append(moduleName).append("** (").append(restCount + feignCount).append(")\n");

            if (restTypeNode != null) {
                restTypeNode.buildMarkdown(sb, level + 1);
            }
            if (feignTypeNode != null) {
                feignTypeNode.buildMarkdown(sb, level + 1);
            }
        }
    }

    // 服务类型节点 (Rest/Feign)
    public class ServiceTypeNode extends BaseSimpleNode {
        private String typeName;
        List<ServiceNode> serviceNodes = new ArrayList<>();
        List<FeignDownstreamNode> downstreamNodes = new ArrayList<>();
        private boolean isFeign;
        private int count = 0;

        public ServiceTypeNode(SimpleNode parent, String typeName, List<RestServiceItem> services, boolean isFeign) {
            super(parent);
            this.typeName = typeName;
            this.isFeign = isFeign;

            if (isFeign) {
                getTemplatePresentation().setIcon(ToolkitIcons.METHOD.FEIGN);
                setIcon(ToolkitIcons.METHOD.FEIGN);
            } else {
                getTemplatePresentation().setIcon(AllIcons.Nodes.ModuleGroup);
                setIcon(AllIcons.Nodes.ModuleGroup);
            }

            if (services != null) {
                this.count = services.size();
                for (RestServiceItem item : services) {
                    serviceNodes.add(new ServiceNode(this, item));
                }
            }
        }

        public void addDownstreamNode(FeignDownstreamNode downstreamNode) {
            downstreamNodes.add(downstreamNode);
            count += downstreamNode.serviceNodes.size();
        }

        public boolean isEmpty() {
            if (isFeign) {
                return downstreamNodes.isEmpty();
            }
            return serviceNodes.isEmpty();
        }

        @Override
        public String getName() {
            return typeName + " (" + count + ")";
        }

        @Override
        protected void buildMarkdown(StringBuilder sb, int level) {
            String indent = "  ".repeat(Math.max(0, level));
            sb.append(indent).append("- ").append(typeName).append(" (").append(count).append(")\n");

            if (!isFeign) {
                for (ServiceNode serviceNode : serviceNodes) {
                    serviceNode.buildMarkdown(sb, level + 1);
                }
            } else {
                for (FeignDownstreamNode downstreamNode : downstreamNodes) {
                    downstreamNode.buildMarkdown(sb, level + 1);
                }
            }
        }
    }

    // Feign 下游服务节点
    public class FeignDownstreamNode extends BaseSimpleNode {
        private String downstreamName;
        List<ServiceNode> serviceNodes = new ArrayList<>();

        public FeignDownstreamNode(SimpleNode parent, String downstreamName, List<RestServiceItem> services) {
            super(parent);
            this.downstreamName = downstreamName;

            getTemplatePresentation().setIcon(ToolkitIcons.METHOD.FEIGN);
            setIcon(ToolkitIcons.METHOD.FEIGN);

            if (services != null) {
                for (RestServiceItem item : services) {
                    serviceNodes.add(new ServiceNode(this, item));
                }
            }
        }

        @Override
        public String getName() {
            return downstreamName + " (" + serviceNodes.size() + ")";
        }

        @Override
        protected void buildMarkdown(StringBuilder sb, int level) {
            String indent = "  ".repeat(Math.max(0, level));
            sb.append(indent).append("- ").append(downstreamName).append(" (").append(serviceNodes.size()).append(")\n");

            for (ServiceNode serviceNode : serviceNodes) {
                serviceNode.buildMarkdown(sb, level + 1);
            }
        }
    }

    // 服务节点
    public class ServiceNode extends BaseSimpleNode {
        RestServiceItem myServiceItem;

        public ServiceNode(SimpleNode parent, RestServiceItem serviceItem) {
            super(parent);
            myServiceItem = serviceItem;

            String downstream = serviceItem.getDownstreamService();
            if (downstream != null && !downstream.isEmpty()) {
                getTemplatePresentation().setIcon(ToolkitIcons.METHOD.FEIGN);
                setIcon(ToolkitIcons.METHOD.FEIGN);
            } else {
                Icon icon = ToolkitIcons.METHOD.get(serviceItem.getMethod());
                if (icon != null) {
                    getTemplatePresentation().setIcon(icon);
                    setIcon(icon);
                }
            }
        }

        @Override
        public String getName() {
            return myServiceItem.getName();
        }

        @Override
        public String getMenuId() {
            return "Toolkit.NavigatorServiceMenu";
        }

        @Override
        public void handleSelection(SimpleTree tree) {
            LOG.info("[ServiceNode] handleSelection called for: " + myServiceItem.getName());
            showServiceDetail(myServiceItem);
        }

        private void showServiceDetail(RestServiceItem serviceItem) {
            LOG.info("[ServiceNode] showServiceDetail called, myRestServiceDetail is null: " + (myRestServiceDetail == null));
            if (myRestServiceDetail == null) {
                LOG.error("[ServiceNode] myRestServiceDetail is null, cannot show detail");
                return;
            }

            LOG.info("[ServiceNode] resetRequestTabbedPane called");
            myRestServiceDetail.resetRequestTabbedPane();

            String method = serviceItem.getMethod() != null ? String.valueOf(serviceItem.getMethod()) : HttpMethod.GET.name();
            LOG.info("[ServiceNode] Setting method: " + method);
            myRestServiceDetail.setMethodValue(method);

            String fullUrl = serviceItem.getFullUrl();
            LOG.info("[ServiceNode] Setting URL: " + fullUrl);
            myRestServiceDetail.setUrlValue(fullUrl);

            String requestParams = "";
            String requestBodyJson = "";
            PsiElement psiElement = serviceItem.getPsiElement();
            if (psiElement.getLanguage() == JavaLanguage.INSTANCE) {
                PsiMethodHelper psiMethodHelper = PsiMethodHelper.create(serviceItem.getPsiMethod()).withModule(serviceItem.getModule());
                requestParams = psiMethodHelper.buildParamString();
                requestBodyJson = psiMethodHelper.buildRequestBodyJson();

            } else if (psiElement.getLanguage() == KotlinLanguage.INSTANCE) {
                if (psiElement instanceof KtNamedFunction) {
                    KtNamedFunction ktNamedFunction = (KtNamedFunction) psiElement;
                    KtFunctionHelper ktFunctionHelper = (KtFunctionHelper) KtFunctionHelper.create(ktNamedFunction).withModule(serviceItem.getModule());
                    requestParams = ktFunctionHelper.buildParamString();
                    requestBodyJson = ktFunctionHelper.buildRequestBodyJson();
                }
            }

            // 只有在有请求参数时才添加请求参数标签页
            if (isNotBlank(requestParams)) {
                myRestServiceDetail.addRequestParamsTab(requestParams);
            }

            // 只有在有请求体时才添加请求体标签页
            if (isNotBlank(requestBodyJson)) {
                myRestServiceDetail.addRequestBodyTabPanel(requestBodyJson);
            }
        }

        @Override
        public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
            PsiElement psiElement = myServiceItem.getPsiElement();

            if (!psiElement.isValid()) {
                RestServicesNavigator.getInstance(myServiceItem.getModule().getProject()).scheduleStructureUpdate();
            }

            if (psiElement.getLanguage() == JavaLanguage.INSTANCE) {
                PsiMethod psiMethod = myServiceItem.getPsiMethod();
                OpenSourceUtil.navigate(psiMethod);

            } else if (psiElement.getLanguage() == KotlinLanguage.INSTANCE) {
                if (psiElement instanceof KtNamedFunction) {
                    KtNamedFunction ktNamedFunction = (KtNamedFunction) psiElement;
                    OpenSourceUtil.navigate(ktNamedFunction);
                }
            }
        }

        @Override
        public void showPopupMenu(SimpleTree tree, int x, int y) {
            JPopupMenu popup = new JPopupMenu();

            JMenuItem copyUrlItem = new JMenuItem("复制完整 URL", AllIcons.Actions.Copy);
            copyUrlItem.addActionListener(e -> {
                String url = myServiceItem.getFullUrl();
                CopyPasteManager.getInstance().setContents(new StringSelection(url));
            });
            popup.add(copyUrlItem);

            JMenuItem copyItem = new JMenuItem("复制为 Markdown", AllIcons.Actions.Copy);
            copyItem.addActionListener(e -> copyAsMarkdown());
            popup.add(copyItem);

            popup.show(tree, x, y);
        }

        @Override
        protected void buildMarkdown(StringBuilder sb, int level) {
            String indent = "  ".repeat(Math.max(0, level));
            String method = myServiceItem.getMethod() != null ? myServiceItem.getMethod().name() : "GET";
            sb.append(indent).append("- [").append(method).append("] ").append(myServiceItem.getUrl()).append("\n");
        }
    }

    private static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
