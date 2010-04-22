package com.soartech.soar.ide.ui.editors.database;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.part.EditorPart;

import com.soartech.soar.ide.core.sql.EditableColumn;
import com.soartech.soar.ide.core.sql.ISoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseEditorInput;
import com.soartech.soar.ide.core.sql.SoarDatabaseJoinFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;
import com.soartech.soar.ide.ui.views.explorer.SoarExplorerDatabaseContentProvider;

public class SoarDatabaseDatamapEditor extends EditorPart {

	public static final String ID = "com.soartech.soar.ide.ui.editors.database.SoarDatabaseDatamapEditor";

	private SoarDatabaseRow row;
    private TreeViewer tree;
	
	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
		if (input instanceof SoarDatabaseEditorInput) {
			row = ((SoarDatabaseEditorInput) input).getRow();
		} else {
			throw new PartInitException("Input not instance of SoarDatabaseEditorInput");
		}
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		tree = new TreeViewer(parent, SWT.NONE);
		
		tree.setContentProvider(new SoarExplorerDatabaseContentProvider(false, false, false, true, true));
		tree.setLabelProvider(SoarLabelProvider.createFullLabelProvider(null));
		tree.setInput(row);
		
		MenuManager manager = new MenuManager();
		manager.addMenuListener(new IMenuListener() {
			
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.removeAll();
				ISelection selection = tree.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection ss = (IStructuredSelection) selection;
					Object element = ss.getFirstElement();

					if (element instanceof SoarDatabaseRow) {
						final SoarDatabaseRow row = (SoarDatabaseRow) element;
						
						ArrayList<Table> childTables = row.getChildTables();
						
						Table table = row.getTable();
						final String tableName = table.shortName();
						final String rowName = row.getName();
						
						for (final Table t : childTables) {
							manager.add(new Action("Add New " + tableName) {
								@Override
								public void run() {
									
									Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
									String title = "New " + tableName;
									String message = "Enter Name:";
									String initialValue = "New " + tableName;
									InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
									dialog.open();
									String result = dialog.getValue();
									
									if (result != null && result.length() > 0) {
										row.createChild(t, result);
										refreshTree();
									}
								}
							});
						}
						
						manager.add(new Action("Rename " + rowName) {
							@Override
							public void run() {
								Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
								String title = "Rename " + tableName;
								String message = "Enter New Name:";
								String initialValue = row.getName();
								InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
								dialog.open();
								String result = dialog.getValue();
								
								if (result != null && result.length() > 0) {
									row.setName(result);
									refreshTree();
								}
							}
						});
						
						manager.add(new Action("Delete " + rowName) {
							@Override
							public void run() {
								row.deleteAllChildren(true);
								refreshTree();
							}
						});
						
						ArrayList<EditableColumn> editableColumns = row.getEditableColumns();
						for (final EditableColumn column : editableColumns) {
							manager.add(new Action("Edit " + column.getName()) {
								@Override
								public void run() {
									Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
									String title = "Edit " + column.getName();
									String message = "Enter New Value:";
									Object currentValue = row.getEditableColumnValue(column);
									String initialValue = null;
									if (currentValue == null) {
										initialValue = "";
									} else {
										initialValue = currentValue.toString();
									}
									InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
									dialog.open();
									String result = dialog.getValue();
									
									if (result != null) {
										EditableColumn.Type columnType = column.getType();
										Object parsedValue = null;
										switch (columnType) {
										case FLOAT:
											parsedValue = Float.parseFloat(result);
											break;
										case INTEGER:
											parsedValue = Integer.parseInt(result);
											break;
										case STRING:
											parsedValue = result;
											break;
										}

										if (parsedValue != null && result.length() > 0) {
											row.editColumnValue(column, parsedValue);
											refreshTree();
										}
									}
								}
							});
						}
					}
					
					else if (element instanceof SoarDatabaseJoinFolder) {
						final SoarDatabaseJoinFolder folder = ((SoarDatabaseJoinFolder)element); 
						final Table t = folder.getTable();
						manager.add(new Action("Add New " + t.shortName()) {
							@Override
							public void run() {
								Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
								String title = "New " + t.shortName();
								String message = "Enter Name:";
								String initialValue = "New " + t.shortName();
								InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
								dialog.open();
								String result = dialog.getValue();
								if (result != null && result.length() > 0) {
									SoarDatabaseRow parent = folder.getRow();
									SoarDatabaseRow newRow = parent.createJoinedChild(t, "New " + t.shortName());
									refreshTree();
								}
							}
						});
					}
					
				}
			}
		});
		
		Menu menu = manager.createContextMenu(tree.getTree());
		tree.getTree().setMenu(menu);
	}

	@Override
	public void setFocus() {
	}
	
	// Convenience method for refreshing tree
	private void refreshTree() {
		Object[] elements = tree.getExpandedElements();
		tree.refresh();
		tree.setExpandedElements(elements);
	}

}
