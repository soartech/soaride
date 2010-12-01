package com.soartech.soar.ide.ui.views.explorer;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRowFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseUtil;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.actions.explorer.ChildProblemSpaceWrapper;
import com.soartech.soar.ide.ui.views.itemdetail.SoarDatabaseItemContentProvider;

public class SoarExplorerContentProvider implements ITreeContentProvider {
	
	String filter = "";
	
	@SuppressWarnings("unchecked")
	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof SoarCorePlugin) {
			SoarDatabaseConnection conn = ((SoarCorePlugin)element).getDatabaseConnection();
			Object[] ret = conn.selectAllFromTable(Table.AGENTS, "order by name").toArray();
			return ret;
		}
		else if (element instanceof ISoarDatabaseTreeItem) {
			ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
			if (element instanceof SoarDatabaseRow) {
				SoarDatabaseRow row = (SoarDatabaseRow) element;
				Table table = row.getTable();
				if (table == Table.AGENTS) {	
					boolean includeFolders = true;
					boolean includeChildrenInFolders = false;
					boolean includeJoinedItems = true;
					boolean includeDirectionalJoinedItems = true;
					boolean putDirectionalJoinedItemsInFolders = true;
					boolean includeDatamapNodes = false;
					ArrayList<ISoarDatabaseTreeItem> ar = new ArrayList<ISoarDatabaseTreeItem>();
					for (ISoarDatabaseTreeItem item : row.getChildrenOfType(Table.PROBLEM_SPACES, "order by name")) {
						if (item instanceof SoarDatabaseRow) {
							SoarDatabaseRow ps = (SoarDatabaseRow) item;
							if (ps.getTable() == Table.PROBLEM_SPACES && ps.isRootProblemSpace()) {
								ar.add(ps);
							}
						}
					}
					ar.addAll(row.getChildren(
							includeFolders,
							includeChildrenInFolders,
							includeJoinedItems,
							includeDirectionalJoinedItems,
							putDirectionalJoinedItemsInFolders,
							includeDatamapNodes));
					return ar.toArray();
				}
				if (table == Table.PROBLEM_SPACES) {
					Table[] folderTables = new Table[] { Table.RULES };
					Table[] rawTables = new Table[] { Table.OPERATORS, Table.PROBLEM_SPACES, Table.TAGS };

					for (Table folderTable : folderTables) {
						SoarDatabaseRowFolder folder = new SoarDatabaseRowFolder(row, folderTable, true);
						if (folder.hasChildren()) {
							ret.add(folder);
						}
					}
					
					for (Table rawTable : rawTables) {
						ret.addAll((ArrayList<ISoarDatabaseTreeItem>) SoarDatabaseUtil.sortRowsByName(row.getJoinedRowsFromTable(rawTable)));
					}
				}
				if (table == Table.OPERATORS) {
					Table[] tables = new Table[] { Table.PROBLEM_SPACES, Table.RULES, Table.TAGS };
					for (Table folderTable : tables) {
						ret.addAll(SoarDatabaseUtil.sortRowsByName(row.getJoinedRowsFromTable(folderTable)));
					}
					ret = SoarDatabaseItemContentProvider.sortExplorerItems(ret);
				}
				if (table == Table.RULES) {
					return new Object[0];
				}
				if (table == Table.TAGS) {
					Table[] tables = new Table[] { Table.PROBLEM_SPACES, Table.OPERATORS, Table.RULES, Table.TAGS };
					for (Table folderTable : tables) {
						ret.addAll(SoarDatabaseUtil.sortRowsByName(row.getJoinedRowsFromTable(folderTable)));
					}
					ret = SoarDatabaseItemContentProvider.sortExplorerItems(ret);
				}
				ret = packageProblemSpaces(ret, row);
			}
			else if (element instanceof SoarDatabaseRowFolder) {
				SoarDatabaseRowFolder folder = (SoarDatabaseRowFolder) element;
				boolean includeFolders = true;
				boolean includeChildrenInFolders = false;
				boolean includeJoinedItems = true;
				boolean includeDirectionalJoinedItems = true;
				boolean putDirectionalJoinedItemsInFolders = true;
				boolean includeDatamapNodes = false;
				ret = folder.getChildren(
						includeFolders,
						includeChildrenInFolders,
						includeJoinedItems,
						includeDirectionalJoinedItems,
						putDirectionalJoinedItemsInFolders,
						includeDatamapNodes);
				SoarDatabaseUtil.sortRowsByName(ret);
			}
			//ret = SoarDatabaseItemContentProvider.sortExplorerItems(ret);
			ret = filterAndSearch(ret);
			return ret.toArray();
		}
		return new Object[0];
	}

	/**
	 * Replaces instances of SoarDatabaseRow of type PROBLEM_SPACE with
	 * ChildProblemSpaceWrapper objects.
	 * @param list
	 * @param parent
	 * @return
	 */
	private ArrayList<ISoarDatabaseTreeItem> packageProblemSpaces(ArrayList<ISoarDatabaseTreeItem> list, SoarDatabaseRow parent) {
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		
		for (ISoarDatabaseTreeItem item : list) {
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow row = (SoarDatabaseRow) item;
				if (row.getTable() == Table.PROBLEM_SPACES) {
					ChildProblemSpaceWrapper wrapper = new ChildProblemSpaceWrapper(parent, row);
					ret.add(wrapper);
				} else {
					ret.add(item);
				}
			} else {
				ret.add(item);
			}
		}
		
		return ret;
	}
	
	/**
	 * Filters the results by the user-entered filter string.
	 * @param list
	 * @return
	 */
	private ArrayList<ISoarDatabaseTreeItem> filterAndSearch(ArrayList<ISoarDatabaseTreeItem> list) {
		if (filter.length() == 0) return list;
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		for (ISoarDatabaseTreeItem item : list) {
			boolean add = true;
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow row = (SoarDatabaseRow) item;
				String name = row.getName();
				if (filter.length() != 0 && !name.contains(filter)) {
					add = false;
				}
			}
			if (add) {
				ret.add(item);
			}
		}
		return ret;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof SoarCorePlugin) {
			return null;
		}
		else if (element instanceof SoarDatabaseRow) {
			ArrayList<SoarDatabaseRow> parents = ((SoarDatabaseRow)element).getParents();
			if (parents.size() > 0) {
				return parents.get(0);
			} else {
				return null;
			}
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof SoarCorePlugin) {
			SoarDatabaseConnection conn = ((SoarCorePlugin)element).getDatabaseConnection();
			if (conn.selectAllFromTable(Table.AGENTS, null).size() > 0) return true;
		}
		else if (element instanceof ISoarDatabaseTreeItem) {
			if (element instanceof SoarDatabaseRow) {
				SoarDatabaseRow row = (SoarDatabaseRow) element;
				Table table = row.getTable();
				if (table == Table.AGENTS) {	
					boolean includeFolders = true;
					boolean includeChildrenInFolders = false;
					boolean includeJoinedItems = true;
					boolean includeDirectionalJoinedItems = true;
					boolean putDirectionalJoinedItemsInFolders = true;
					boolean includeDatamapNodes = false;
					if (row.hasChildrenOfType(Table.PROBLEM_SPACES)) return true;
					if (row.getChildren(
							includeFolders,
							includeChildrenInFolders,
							includeJoinedItems,
							includeDirectionalJoinedItems,
							putDirectionalJoinedItemsInFolders,
							includeDatamapNodes).size() > 0) return true;
				}
				if (table == Table.PROBLEM_SPACES) {
					Table[] tables = new Table[] { Table.PROBLEM_SPACES, Table.OPERATORS, Table.RULES, Table.TAGS };
					for (Table folderTable : tables) {
						if (row.getDirectedJoinedChildrenOfType(folderTable, false, false).size() > 0) return true;
					}
				}
				if (table == Table.OPERATORS) {
					Table[] tables = new Table[] { Table.PROBLEM_SPACES, Table.RULES, Table.TAGS };
					for (Table folderTable : tables) {
						if (row.getJoinedRowsFromTable(folderTable).size() > 0) return true;
					}
				}
				if (table == Table.RULES) {
					return false;
				}
				if (table == Table.TAGS) {
					Table[] tables = new Table[] { Table.PROBLEM_SPACES, Table.OPERATORS, Table.RULES, Table.TAGS };
					for (Table folderTable : tables) {
						if(row.getDirectedJoinedChildrenOfType(folderTable, false, false).size() > 0) return true;
					}
				}
			}
			else if (element instanceof SoarDatabaseRowFolder) {
				SoarDatabaseRowFolder folder = (SoarDatabaseRowFolder) element;
				boolean includeFolders = true;
				boolean includeChildrenInFolders = false;
				boolean includeJoinedItems = true;
				boolean includeDirectionalJoinedItems = true;
				boolean putDirectionalJoinedItemsInFolders = true;
				boolean includeDatamapNodes = false;
				if (folder.getChildren(
						includeFolders,
						includeChildrenInFolders,
						includeJoinedItems,
						includeDirectionalJoinedItems,
						putDirectionalJoinedItemsInFolders,
						includeDatamapNodes).size() > 0) return true;
			}
		}
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	
	public void setFilter(String filter) {
		this.filter = filter.trim();
	}
}
