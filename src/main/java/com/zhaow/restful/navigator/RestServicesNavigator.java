package com.zhaow.restful.navigator;

import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.treeStructure.SimpleTree;
import com.zhaow.restful.common.ToolkitIcons;
import com.zhaow.utils.RestfulToolkitBundle;
import com.zhaow.utils.ToolkitUtil;
import com.zhaow.utils.PluginLogger;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.net.URL;

@State(name = "RestServicesNavigator", storages = { @Storage(StoragePathMacros.WORKSPACE_FILE) })
public class RestServicesNavigator implements PersistentStateComponent<RestServicesNavigatorState>, ProjectComponent {

	private final PluginLogger LOG;

	public static final String TOOL_WINDOW_ID = "RestServices";

	private static final URL SYNC_ICON_URL = RestServicesNavigator.class.getResource("/actions/refresh.png");

	protected final Project myProject;

	// private JTree myTree;
	protected RestServiceStructure myStructure;

	RestServicesNavigatorState myState = new RestServicesNavigatorState();

	private SimpleTree myTree;

	private ToolWindowEx myToolWindow;

	private RestServiceProjectsManager myProjectsManager;

	public RestServicesNavigator(Project myProject) {
		this.myProject = myProject;
		myProjectsManager = RestServiceProjectsManager.getInstance(myProject);
		this.LOG = new PluginLogger(RestServicesNavigator.class, myProject);
	}

	public static RestServicesNavigator getInstance(Project p) {
		return p.getComponent(RestServicesNavigator.class);
	}

	private void initTree() {
		LOG.info("Initializing SimpleTree");
		myTree = new SimpleTree() {

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				// Only show "nothing to display" message when project is NOT initialized
				if (!myProject.isInitialized()) {
					final JLabel myLabel = new JLabel(RestfulToolkitBundle.message("toolkit.navigator.nothing.to.display",
							ToolkitUtil.formatHtmlImage(SYNC_ICON_URL)));
					myLabel.setFont(getFont());
					myLabel.setBackground(getBackground());
					myLabel.setForeground(getForeground());
					Rectangle bounds = getBounds();
					Dimension size = myLabel.getPreferredSize();
					myLabel.setBounds(0, 0, size.width, size.height);

					int x = (bounds.width - size.width) / 2;
					Graphics g2 = g.create(bounds.x + x, bounds.y + 20, bounds.width, bounds.height);
					try {
						myLabel.paint(g2);
					}
					finally {
						g2.dispose();
					}
				}
			}
		};
		myTree.getEmptyText().clear();

		myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		LOG.info("SimpleTree initialized successfully");
	}

	@Override
	public void initComponent() {
		LOG.info("[RestServicesNavigator] initComponent called");
		LOG.info("[RestServicesNavigator] Current thread: " + Thread.currentThread().getName());
		LOG.info("[RestServicesNavigator] isDispatchThread: " + ApplicationManager.getApplication().isDispatchThread());
		LOG.info("[RestServicesNavigator] Project isOpen: " + myProject.isOpen() + ", isDisposed: " + myProject.isDisposed() + ", isInitialized: " + myProject.isInitialized());
		
		listenForProjectsChanges();
		// 注册工具窗口监听器以检测可见性变化
		// 注意：某些IDE版本可能不支持ToolWindowManagerListener，所以使用现有的stateChanged方法
		
		ToolkitUtil.runDumbAware(myProject, () -> {
			LOG.info("[RestServicesNavigator] runDumbAware callback executing");
			if (myProject.isDisposed()) {
				LOG.info("[RestServicesNavigator] Project is disposed, skipping init");
				return;
			}
			if (myProject.isOpen()) {
				LOG.info("[RestServicesNavigator] Project is open, initializing tool window");
				initToolWindow();
			} else {
				LOG.info("[RestServicesNavigator] Project is not open yet");
			}
		});
	}

	private void initToolWindow() {
		LOG.info("[RestServicesNavigator] initToolWindow called");
		LOG.info("[RestServicesNavigator] Current thread: " + Thread.currentThread().getName());
		
		// 确保树结构已初始化 before creating the panel
		if (myTree == null) {
			LOG.info("[RestServicesNavigator] myTree is null, initializing tree");
			initTree();
		}
		
		final ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(myProject);
		myToolWindow = (ToolWindowEx) manager.getToolWindow(TOOL_WINDOW_ID);
		LOG.info("[RestServicesNavigator] Existing tool window is null: " + (myToolWindow == null));
		
		// 即使工具窗口已存在，也要确保正确初始化
		if (myToolWindow == null) {
			LOG.info("[RestServicesNavigator] Tool window doesn't exist, registering new one");
			myToolWindow = (ToolWindowEx) manager.registerToolWindow(TOOL_WINDOW_ID, false, ToolWindowAnchor.RIGHT,
					myProject, true);
			myToolWindow.setIcon(ToolkitIcons.SERVICE);
			LOG.info("[RestServicesNavigator] Tool window registered");

			LOG.info("[RestServicesNavigator] Creating panel and content");
			JPanel panel = new RestServicesNavigatorPanel(myProject, myTree);
			final ContentFactory contentFactory = ServiceManager.getService(ContentFactory.class);
			final Content content = contentFactory.createContent(panel, "", false);
			ContentManager contentManager = myToolWindow.getContentManager();
			contentManager.addContent(content);
			contentManager.setSelectedContent(content, false);
			LOG.info("[RestServicesNavigator] Content added to tool window");
		} else {
			LOG.info("[RestServicesNavigator] Tool window already exists, ensuring it's properly configured");
		}
		
		// 确保数据模型已初始化
		if (myStructure == null) {
			LOG.info("[RestServicesNavigator] myStructure is null, initializing structure");
			initStructure();
		}
		
		// Schedule initial data update
		LOG.info("[RestServicesNavigator] Scheduling initial structure update");
		scheduleStructureUpdate();
		LOG.info("[RestServicesNavigator] initToolWindow completed");

		// final ToolWindowManagerAdapter listener = new ToolWindowManagerAdapter() {
		// boolean wasVisible = false;
		//
		// @Override
		// public void stateChanged() {
		// if (myToolWindow.isDisposed()) {
		// return;
		// }
		// boolean visible = myToolWindow.isVisible();
		// if (!visible || wasVisible) {
		// return;
		// }
		// scheduleStructureUpdate();
		// wasVisible = true;
		// }
		// };
		// manager.addToolWindowManagerListener(listener, myProject);

		// todo: extend toolWindows right click
		/*
		 * ActionManager actionManager = ActionManager.getInstance(); DefaultActionGroup
		 * group = new DefaultActionGroup();
		 * group.add(actionManager.getAction("Maven.GroupProjects"));
		 * group.add(actionManager.getAction("Maven.ShowIgnored"));
		 * group.add(actionManager.getAction("Maven.ShowBasicPhasesOnly"));
		 * group.add(actionManager.getAction("Maven.AlwaysShowArtifactId")); // 默认显示 app
		 * serviceName group.add(actionManager.getAction("Maven.ShowVersions")); //
		 * myToolWindow.setAdditionalGearActions(group);
		 */
	}

	boolean wasVisible = false;
	
	public void stateChanged() {
		if (myToolWindow == null) {
			return;
		}
		if (myToolWindow.isDisposed()) {
			return;
		}
		boolean visible = myToolWindow.isVisible();
		LOG.info("[RestServicesNavigator] Tool window visibility changed, now: " + visible + ", was: " + wasVisible);
		if (!visible || wasVisible) {
			// 如果窗口从不可见变为可见，且有待处理的刷新，则执行刷新
			if (visible && myStructure != null) {
				LOG.info("[RestServicesNavigator] Tool window became visible, checking for pending refresh");
				myStructure.onToolWindowBecameVisible();
			}
			wasVisible = visible;
			return;
		}
		scheduleStructureUpdate();
		wasVisible = true;
	}

	public void scheduleStructureUpdate() {
		LOG.info("[RestServicesNavigator] scheduleStructureUpdate called");
		LOG.info("[RestServicesNavigator] Current thread: " + Thread.currentThread().getName());
		LOG.info("[RestServicesNavigator] myStructure is null: " + (myStructure == null));
		LOG.info("[RestServicesNavigator] myToolWindow is null: " + (myToolWindow == null));
		
		if (myStructure == null) {
			LOG.warn("[RestServicesNavigator] myStructure is null, initializing structure first");
			initStructure();
		}
		if (myStructure != null) {
			LOG.info("[RestServicesNavigator] Scheduling structure request");
			scheduleStructureRequest(() -> myStructure.update());
		} else {
			LOG.error("[RestServicesNavigator] Failed to initialize structure, cannot update");
		}
	}

	private void scheduleStructureRequest(final Runnable r) {
		LOG.info("[RestServicesNavigator] scheduleStructureRequest called");
		LOG.info("[RestServicesNavigator] Current thread: " + Thread.currentThread().getName());
		LOG.info("[RestServicesNavigator] myToolWindow is null: " + (myToolWindow == null));
		
		if (myToolWindow == null) {
			LOG.warn("[RestServicesNavigator] myToolWindow is null, cannot schedule request");
			return;
		}
		
		LOG.info("[RestServicesNavigator] Calling runWhenProjectIsReady");
		// 检查项目是否已完全初始化
		if (!myProject.isInitialized() || myProject.isDisposed()) {
			LOG.warn("[RestServicesNavigator] Project not ready - initialized: " + myProject.isInitialized() + ", disposed: " + myProject.isDisposed());
			return;
		}
		
		// 检查工具窗口是否已创建
		if (myToolWindow == null) {
            LOG.warn("[RestServicesNavigator] Tool window is not ready yet");
            return;
        }
        
        // 确保在工具窗口可见时才更新
        if (!myToolWindow.isVisible()) {
            LOG.info("[RestServicesNavigator] Tool window not visible, will update when visible");
            // 注册监听器，当工具窗口变为可见时再更新
            return;
        }
        
		ToolkitUtil.runWhenProjectIsReady(myProject, () -> {
			LOG.info("[RestServicesNavigator] runWhenProjectIsReady callback executing");
			LOG.info("[RestServicesNavigator] myToolWindow.isVisible(): " + myToolWindow.isVisible());
			LOG.info("[RestServicesNavigator] Project initialized: " + myProject.isInitialized() + ", disposed: " + myProject.isDisposed());
			
			if (!myToolWindow.isVisible()) {
				LOG.info("[RestServicesNavigator] Tool window not visible, skipping update");
				return;
			}

			// Structure should already be initialized in initToolWindow
			// But check again just in case
			if (myStructure == null) {
				LOG.warn("[RestServicesNavigator] Structure is null, initializing...");
				initStructure();
			}

			LOG.info("[RestServicesNavigator] Running structure update runnable");
			r.run();
			LOG.info("[RestServicesNavigator] Structure update runnable completed");
			// fixme: compat
			// if (shouldCreate) {
			// TreeState.createFrom(myState.treeState).applyTo(myTree);
			// }
		});
	}

	private void initStructure() {
		LOG.info("[RestServicesNavigator] initStructure called");
		LOG.info("[RestServicesNavigator] Current thread: " + Thread.currentThread().getName());
		LOG.info("[RestServicesNavigator] myTree is null: " + (myTree == null));
		LOG.info("[RestServicesNavigator] myProjectsManager is null: " + (myProjectsManager == null));
		
		if (myTree == null) {
			LOG.warn("[RestServicesNavigator] myTree is null, cannot initialize structure");
			return;
		}
		
		LOG.info("[RestServicesNavigator] Creating RestServiceStructure");
		myStructure = new RestServiceStructure(myProject, myProjectsManager, myTree);
		LOG.info("[RestServicesNavigator] RestServiceStructure initialized successfully");
	}

	private void listenForProjectsChanges() {
		// todo :
	}

	@Nullable
	@Override
	public RestServicesNavigatorState getState() {
		// 修复线程安全问题：getState() 可能在非EDT线程中被调用
		// 只有在EDT线程中才尝试从UI组件读取状态，否则返回已保存的状态
		if (ApplicationManager.getApplication().isDispatchThread()) {
			if (myStructure != null && myTree != null) {
				try {
					myState.treeState = new Element("root");
					TreeState.createOn(myTree).writeExternal(myState.treeState);
				}
				catch (WriteExternalException e) {
					LOG.warn("WriteExternalException: " + e.getMessage());
				}
			}
		}
		return myState;
	}

	@Override
	public void loadState(RestServicesNavigatorState state) {
		myState = state;
		scheduleStructureUpdate();
	}

}