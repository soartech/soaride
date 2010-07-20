package com.soartech.soar.ide.ui.views.explorer;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRowFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.views.itemdetail.SoarDatabaseItemContentProvider;

public class SoarExplorerDatabaseContentProvider implements ITreeContentProvider {
	
	String filter = "";
	String search = "";
	
	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof ISoarModel) {
			SoarDatabaseConnection conn = ((ISoarModel)element).getDatabase();
			Object[] ret = conn.selectAllFromTable(Table.AGENTS).toArray();
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
					for (ISoarDatabaseTreeItem item : row.getChildrenOfType(Table.PROBLEM_SPACES)) {
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
					ret.addAll(row.getJoinedRowsFromTable(Table.RULES));
					ret.addAll(row.getJoinedRowsFromTable(Table.OPERATORS));
					ret.addAll(row.getDirectedJoinedChildrenOfType(Table.PROBLEM_SPACES, false));
					ret = SoarDatabaseItemContentProvider.sortItems(ret);
				}
				if (table == Table.OPERATORS) {
					ret.addAll(row.getJoinedRowsFromTable(Table.RULES));
					ret.addAll(row.getJoinedRowsFromTable(Table.PROBLEM_SPACES));
				}
				if (table == Table.RULES) {
					return new Object[0];
				}
				if (table == Table.TAGS) {
					ret.addAll(row.getUndirectedJoinedRowsFromTable(Table.OPERATORS));
					ret.addAll(row.getUndirectedJoinedRowsFromTable(Table.RULES));
					ret.addAll(row.getUndirectedJoinedRowsFromTable(Table.PROBLEM_SPACES));
					ret = SoarDatabaseItemContentProvider.sortItems(ret);
				}
			}
			if (element instanceof SoarDatabaseRowFolder) {
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
			}
			ret = SoarDatabaseItemContentProvider.sortItems(ret);
			ret = filter(ret);
			return ret.toArray();
		}
		return new Object[0];
	}
	
	private ArrayList<ISoarDatabaseTreeItem> filter(ArrayList<ISoarDatabaseTreeItem> list) {
		if (filter.length() == 0 && search.length() == 0) return list;
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		for (ISoarDatabaseTreeItem item : list) {
			boolean add = true;
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow row = (SoarDatabaseRow) item;
				String name = row.getName();
				if (filter.length() != 0 && !name.contains(filter)) {
					add = false;
				}
				String text = row.getText();
				if (search.length() != 0 && row.getTable() == Table.RULES && !text.contains(search)) {
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
		if (element instanceof ISoarModel) {
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
		if (element instanceof ISoarDatabaseTreeItem) {
			Object[] children = getChildren(element);
			boolean ret = children.length > 0;
			return ret;
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
	
	public void setSearch(String search) {
		this.search = search.trim();
	}

}
