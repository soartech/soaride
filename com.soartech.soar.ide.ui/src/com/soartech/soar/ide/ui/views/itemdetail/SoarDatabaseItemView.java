package com.soartech.soar.ide.ui.views.itemdetail;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.sql.ISoarDatabaseEventListener;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.editors.datamap.SoarDatabaseDatamapEditor;
import com.soartech.soar.ide.ui.views.explorer.SoarExplorerView;

public class SoarDatabaseItemView extends ViewPart implements ISoarDatabaseEventListener, ISelectionListener, IDoubleClickListener {
    public static final String ID = "com.soartech.soar.ide.ui.views.SoarDatabaseItemView";
    
	TreeViewer tree;
	
	@Override
	public void createPartControl(Composite parent) {
		tree = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        //tree.setUseHashlookup(true); // this significantly improves update performance
		tree.setContentProvider(new SoarDatabaseItemContentProvider());
		tree.setLabelProvider(new SoarDatabaseItemLabelProvider());
		ISoarModel input = SoarCorePlugin.getDefault().getSoarModel();
        tree.setInput(input);
        getSite().setSelectionProvider(tree);
        input.getDatabase().addListener(this);
        IWorkbenchPartSite site = getSite();
        IWorkbenchPage page = site.getPage();
        page.addPostSelectionListener(this);
        
        tree.addDoubleClickListener(this);
        /*
		tree.getControl().addKeyListener(new org.eclipse.swt.events.KeyListener() {

			@Override
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == java.awt.event.KeyEvent.VK_DELETE) {
					TreeSelection ts = (TreeSelection) tree.getSelection();
					for (TreePath tp : ts.getPaths()) {
						int segments = tp.getSegmentCount();
						if (segments > 1) {
							Object childObject = tp.getLastSegment();
							Object parentObject = tp.getSegment(segments - 2);
							if (childObject instanceof SoarDatabaseRow && parentObject instanceof SoarDatabaseRow) {
								SoarDatabaseRow child = (SoarDatabaseRow) childObject;
								SoarDatabaseRow parent = (SoarDatabaseRow) parentObject;
								Table childTable = child.getTable();
								Table parentTable = parent.getTable();
								if (childTable == parentTable && childTable.isDatamapTable()) {
									if (SoarDatabaseRow.rowsAreJoined(parent, child, parent.getDatabaseConnection())) {
										proposeUnlinkRows(parent, child);
									}
								}
							}
						}
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent event) {
				
			}
			
		});
		*/
	}

	@Override
	public void setFocus() {
		
	}

	@Override
	public void onEvent(SoarDatabaseEvent event, SoarDatabaseConnection db) {
		if (event.type == SoarDatabaseEvent.Type.DATABASE_PATH_CHANGED) {
			ISoarModel input = SoarCorePlugin.getDefault().getSoarModel();
	        tree.setInput(input);
		}
		
		tree.refresh();
		/*
		TreePath[] paths = tree.getExpandedTreePaths();
		Object input = tree.getInput();
		tree.setInput(null);
		tree.setInput(input);
		tree.setExpandedTreePaths(paths);
		*/
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		Class<?> clazz = part.getClass();
		if (clazz == SoarExplorerView.class || clazz == SoarDatabaseDatamapEditor.class) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				if (!ss.isEmpty()) {
					Object obj = ss.getFirstElement();
					if (obj != null) {
						tree.setInput(new Object[] { obj });
						tree.setExpandedState(obj, true);
					}
				} else {
					tree.setInput(new Object[] {});
				}
			}
		}
	}
	
	private void refreshTree() {
		Object[] elements = tree.getExpandedElements();
		TreePath[] treePaths = tree.getExpandedTreePaths();
		tree.refresh();
		tree.setExpandedElements(elements);
		tree.setExpandedTreePaths(treePaths);
	}

	@Override
	public void doubleClick(DoubleClickEvent event) {
		ISelection s = tree.getSelection();
		if (s instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) s;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseRow) {
				SoarDatabaseRow row = (SoarDatabaseRow) obj;
				Table table = row.getTable();
		        IWorkbenchPage page = SoarEditorUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
		        
				if (table == Table.RULES) {
					try {
						SoarUiModelTools.showRuleInEditor(page, row);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				} else if (table == Table.PROBLEM_SPACES) {
					try {
						SoarUiModelTools.showProblemSpaceInEditor(page, row);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void proposeUnlinkRows(SoarDatabaseRow first, SoarDatabaseRow second) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String title = "Unlink items?";
		org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
		String message = "Unlink \"" + first.getName() + "\" and \"" + second.getName() + "\"?";
		String[] labels = new String[] { "OK", "Cancel" };
		MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, labels, 0);
		int result = dialog.open();
		if (result == 1) {
			return;
		}
		SoarDatabaseRow.unjoinRows(first, second, first.getDatabaseConnection());
	}
}
