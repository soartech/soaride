package com.soartech.soar.ide.ui.views.explorer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
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
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

public class SoarDatabaseItemView extends ViewPart implements ISoarDatabaseEventListener, ISelectionListener, IDoubleClickListener {
    public static final String ID = "com.soartech.soar.ide.ui.views.SoarDatabaseItemView";

	TreeViewer tree;
	
	@Override
	public void createPartControl(Composite parent) {
		tree = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        tree.setUseHashlookup(true); // this significantly improves update performance
		tree.setContentProvider(new SoarDatabaseItemContentProvider());
		tree.setLabelProvider(SoarLabelProvider.createFullLabelProvider(null));
		ISoarModel input = SoarCorePlugin.getDefault().getSoarModel();
        tree.setInput(input);
        getSite().setSelectionProvider(tree);
        SoarCorePlugin.getDefault().getSoarModel().getDatabase().addListener(this);
        getSite().getPage().addPostSelectionListener(this);
        tree.addDoubleClickListener(this);
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
		
		refreshTree();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		Class<?> clazz = part.getClass();
		if (clazz == SoarExplorerView.class) {
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
}
