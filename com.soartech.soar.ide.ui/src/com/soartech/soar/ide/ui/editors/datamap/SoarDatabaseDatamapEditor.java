package com.soartech.soar.ide.ui.editors.datamap;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.part.EditorPart;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.sql.EditableColumn;
import com.soartech.soar.ide.core.sql.ISoarDatabaseEventListener;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseEditorInput;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent;
import com.soartech.soar.ide.core.sql.SoarDatabaseJoinFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.actions.dragdrop.SoarDatabaseDatamapDragAdapter;
import com.soartech.soar.ide.ui.actions.dragdrop.SoarDatabaseDatamapDropAdapter;
import com.soartech.soar.ide.ui.views.search.SoarDatabaseSearchResultsView;

public class SoarDatabaseDatamapEditor extends EditorPart implements ISoarDatabaseEventListener {

	public static final String ID = "com.soartech.soar.ide.ui.editors.datamap.SoarDatabaseDatamapEditor";

	private SoarDatabaseRow problemSpaceRow;
	private IStructuredSelection selectedRows;
	private TreeViewer tree;
	private Composite parent;

	@Override
	public void doSave(IProgressMonitor monitor) {
		
	}

	@Override
	public void doSaveAs() {
		
	}
	
	public SoarDatabaseRow getProblemSpace() {
		return problemSpaceRow;
	}
	
	public TreeViewer getTree() {
		return tree;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
		if (input instanceof SoarDatabaseEditorInput) {
			problemSpaceRow = ((SoarDatabaseEditorInput) input).getRow();
			String description = input.getName();
			setPartName(description);
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
		this.parent = parent;
		tree = new TreeViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tree.getTree().setHeaderVisible(true);
		tree.getTree().setLinesVisible(true);
        
		TreeColumn leftColumn = new TreeColumn(tree.getTree(), SWT.NONE);
		
		leftColumn.setWidth(500);
		leftColumn.setResizable(true);
		leftColumn.setText("Datamap Nodes");
		
		TreeColumn rightColumn = new TreeColumn(tree.getTree(), SWT.NONE);
		rightColumn.setWidth(200);
		rightColumn.setResizable(true);
		rightColumn.setText("Comments");
				
        getSite().setSelectionProvider(tree);

		tree.setContentProvider(new SoarDatabaseDatamapContentProvider());
		tree.setLabelProvider(new SoarDatabaseDatamapLabelProvider());
		tree.setInput(problemSpaceRow);
		
		tree.getControl().addKeyListener(new org.eclipse.swt.events.KeyListener() {
			
			@Override
			public void keyReleased(org.eclipse.swt.events.KeyEvent event) {
				
			}
			
			@Override
			public void keyPressed(org.eclipse.swt.events.KeyEvent event) {
				if (event.keyCode == KeyEvent.VK_DELETE) {
					deleteSelectedItem(true);
				}
			}
		});
		
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					selectedRows = (IStructuredSelection) selection;
				}
			}
		});

		MenuManager manager = new MenuManager();
		manager.addMenuListener(new IMenuListener() {

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.removeAll();
				ISelection selection = tree.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection ss = (IStructuredSelection) selection;
					Object element = ss.getFirstElement();

					if (element instanceof ISoarDatabaseTreeItem) {
						final SoarDatabaseRow row = ((ISoarDatabaseTreeItem) element).getRow();

						ArrayList<Table> childTables = row.getChildTables();

						Table table = row.getTable();
						final String tableName = table.shortName();
						final String rowName = row.getName();

						for (final Table t : childTables) {
							final String childTableName = t.englishName();
							manager.add(new Action("Add new " + childTableName) {
										@Override
										public void run() {

											Shell shell = PlatformUI
													.getWorkbench()
													.getActiveWorkbenchWindow()
													.getShell();
											String title = "New " + childTableName;
											String message = "Enter Name:";
											String initialValue = t.soarName() + "-name";
											InputDialog dialog = new InputDialog(
													shell, title, message,
													initialValue, null);
											dialog.open();
											String result = dialog.getValue();

											if (result != null
													&& result.length() > 0) {
												row.createChild(t, result);
												refreshTree();
											}
										}
									});
						}
						
						if (table == Table.DATAMAP_ENUMERATIONS) {
							final ArrayList<SoarDatabaseRow> enumValues = row.getChildrenOfType(Table.DATAMAP_ENUMERATION_VALUES);
							if (enumValues.size() > 0) {
								manager.add(new Action("Remove Enumeration Values") {
									@Override
									public void run() {
										Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
										ListSelectionDialog dialog = new ListSelectionDialog(
											shell,
											enumValues,
											new ArrayContentProvider(),
											new LabelProvider(),
											"Select Enumeration Values to Remove"
											);
										dialog.open();
										Object[] result = dialog.getResult();
										for (Object obj : result) {
											SoarDatabaseRow row = (SoarDatabaseRow) obj;
											row.deleteAllChildren(true, null);
										}
									}
								});
							}
						}
						
						if (table != Table.DATAMAP_ENUMERATION_VALUES) {
							manager.add(new Action("Search for rules that test \"" + row.getName() + "\"") {
								@Override
								public void run() {
									SoarDatabaseSearchResultsView.searchForRulesWithDatamapAttribute(row);
								};
							});
						}

						manager.add(new Action("Rename \"" + rowName + "\"") {
							@Override
							public void run() {
								Shell shell = PlatformUI.getWorkbench()
										.getActiveWorkbenchWindow().getShell();
								String title = "Rename \"" + tableName + "\"";
								String message = "Enter New Name:";
								String initialValue = row.getName();
								InputDialog dialog = new InputDialog(shell,
										title, message, initialValue, null);
								dialog.open();
								String result = dialog.getValue();

								if (result != null && result.length() > 0) {
									row.setName(result);
									refreshTree();
								}
							}
						});

						manager.add(new Action("Delete \"" + rowName + "\"") {
							@Override
							public void run() {
								deleteSelectedItem(true);
							}
						});
						
						manager.add(new Action("Edit Comment") {
							@Override
							public void run() {
								Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
								InputDialog dialog = new InputDialog(shell, "Edit Comment", null, row.getComment(), null);
								dialog.open();
								String comment = dialog.getValue();
								row.setComment(comment);
							}
						});

						ArrayList<EditableColumn> editableColumns = row.getEditableColumns();
						for (final EditableColumn column : editableColumns) {
							manager.add(new Action("Edit \"" + column.getName() + "\"") {
								@Override
								public void run() {
									Shell shell = PlatformUI.getWorkbench()
											.getActiveWorkbenchWindow()
											.getShell();
									String title = "Edit \"" + column.getName() + "\"";
									String message = "Enter New Value:";
									Object currentValue = row
											.getEditableColumnValue(column);
									String initialValue = null;
									if (currentValue == null) {
										initialValue = "";
									} else {
										initialValue = currentValue.toString();
									}
									InputDialog dialog = new InputDialog(shell,
											title, message, initialValue, null);
									dialog.open();
									String result = dialog.getValue();

									if (result != null) {
										EditableColumn.Type columnType = column
												.getType();
										Object parsedValue = null;
										switch (columnType) {
										case FLOAT:
											parsedValue = Float
													.parseFloat(result);
											break;
										case INTEGER:
											parsedValue = Integer
													.parseInt(result);
											break;
										case STRING:
											parsedValue = result;
											break;
										}

										if (parsedValue != null
												&& result.length() > 0) {
											row.editColumnValue(column,
													parsedValue);
											refreshTree();
										}
									}
								}
							});
						}

						final ArrayList<ISoarDatabaseTreeItem> folders = row.getDirectedJoinedChildren(true);
						if (folders.size() > 0) {
							MenuManager sub = new MenuManager("Add New Attribute");

							manager.add(sub);
							
							for (ISoarDatabaseTreeItem item : folders) {
								SoarDatabaseJoinFolder folder = (SoarDatabaseJoinFolder) item;
								final Table folderTable = folder.getTable();
								int spaceIndex = folderTable.englishName().indexOf(' ') + 1;
								final String folderTableName = folderTable.englishName().substring(spaceIndex, spaceIndex + 1).toUpperCase() + folderTable.englishName().substring(spaceIndex + 1); 
								sub.add(new Action(folderTableName) {
									@Override
									public void run() {
										Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
										String title = "New " + folderTableName;
										String message = "Enter Name:";
										String initialValue = folderTable.soarName() + "-name";
										InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
										dialog.open();
										String resultString = dialog.getValue();
										if (resultString != null && resultString.length() > 0) {
											row.createJoinedChild(folderTable, resultString);
											refreshTree();
											tree.setExpandedState(row, true);
										}
									}
								});
							}
						}

						if (row.getUndirectedJoinedRowsFromTable(row.getTable()).size() > 0) {
							manager.add(new Action("Remove Links") {
								@Override
								public void run() {

									Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
									String title = "Remove Links?";
									org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
									String message = "Remove all links from \"" + row.getName() + "\"?";
									String[] labels = new String[] { "OK", "Cancel" };
									MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, labels, 0);
									int result = dialog.open();
									if (result == 1) {
										return;
									}

									for (ISoarDatabaseTreeItem item : row.getUndirectedJoinedRowsFromTable(row.getTable())) {
										SoarDatabaseRow.unjoinRows(row, (SoarDatabaseRow) item, row.getDatabaseConnection());
									}
									refreshTree();
									tree.setExpandedState(row, true);
								};
							});
						}
					}
				}
			}
		});

		Menu menu = manager.createContextMenu(tree.getTree());
		tree.getTree().setMenu(menu);
		
		tree.addDragSupport(DND.DROP_MOVE | DND.DROP_LINK, new Transfer[] {LocalSelectionTransfer.getTransfer()}, new SoarDatabaseDatamapDragAdapter());
        tree.addDropSupport(DND.DROP_MOVE | DND.DROP_LINK, new Transfer[] {LocalSelectionTransfer.getTransfer()}, new SoarDatabaseDatamapDropAdapter(tree));
        SoarCorePlugin.getDefault().getDatabaseConnection().addListener(this);
	}

	@Override
	public void setFocus() {
	}
	
	private void deleteSelectedItem(boolean confirmFirst) {

		if (selectedRows == null || selectedRows.size() == 0) {
			return;
		}

		// Make sure that isn't the root node
		for (Iterator<?> it = selectedRows.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (!(obj instanceof SoarDatabaseRow))
				continue;
			SoarDatabaseRow selectedRow = (SoarDatabaseRow) obj;
			if (!selectedRow.exists()) continue;
			ArrayList<SoarDatabaseRow> parents = selectedRow.getParents();
			for (SoarDatabaseRow row : parents) {
				if (row.getTable() == Table.PROBLEM_SPACES) {
					// This is a root node
					// don't allow deletion
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					String title = "Cannot delete root node";
					org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
					String message = "Cannot delete root node.";
					String[] labels = new String[] { "OK" };
					MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.ERROR, labels, 0);
					dialog.open();
					return;
				}
			}

			if (confirmFirst) {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				String title = "Delete item?";
				org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
				String message = "Are you sure you want to delete \"" + selectedRow.getName() + "\"?\nThis action cannot be undone.";
				String[] labels = new String[] { "OK", "Cancel" };
				MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, labels, 0);
				int result = dialog.open();
				if (result == 1) {
					return;
				}
			}

			// Delete the row
			SoarDatabaseRow rowToDelete = selectedRow;

			// TODO
			// select the next item
			/*
			 * ISelection sel = tree.getSelection(); if (sel instanceof
			 * StructuredSelection) { StructuredSelection ss =
			 * (StructuredSelection) sel; tree. }
			 */

			boolean eventsWereSupressed = rowToDelete.getDatabaseConnection().getSupressEvents();
			rowToDelete.getDatabaseConnection().setSupressEvents(true);
			rowToDelete.deleteAllChildren(true, null);
			rowToDelete.getDatabaseConnection().setSupressEvents(eventsWereSupressed);
		}
		selectedRows = null;
		refreshTree();
	}

	// Convenience method for refreshing tree
	public void refreshTree() {

		Runnable runnable = new Runnable() {
		  
		  @Override public void run() {

		try {
			Object[] elements = tree.getExpandedElements();
			TreePath[] paths = tree.getExpandedTreePaths();
			tree.setInput(problemSpaceRow);
			tree.setExpandedTreePaths(paths);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		  } };
		  
		  Display.findDisplay(Thread.currentThread()).asyncExec(runnable);
		 
	}

	@Override
	public void onEvent(SoarDatabaseEvent event, SoarDatabaseConnection db) {
		
		if (tree.getTree().isDisposed()) {
	        SoarCorePlugin.getDefault().getDatabaseConnection().removeListener(this);
	        return;
		}

		refreshTree();
	}
}
