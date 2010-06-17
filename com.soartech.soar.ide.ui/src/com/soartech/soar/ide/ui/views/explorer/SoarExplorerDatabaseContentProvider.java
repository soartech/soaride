package com.soartech.soar.ide.ui.views.explorer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.sql.EditableColumn;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRowFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarExplorerDatabaseContentProvider implements ITreeContentProvider {
	
	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof ISoarModel) {
			SoarDatabaseConnection conn = ((ISoarModel)element).getDatabase();
			Object[] ret = conn.selectAllFromTable(Table.AGENTS).toArray();
			return ret;
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
					return row.getChildren(
							includeFolders,
							includeChildrenInFolders,
							includeJoinedItems,
							includeDirectionalJoinedItems,
							putDirectionalJoinedItemsInFolders,
							includeDatamapNodes).toArray();
				}
				if (table == Table.PROBLEM_SPACES) {
					ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
					ret.addAll(row.getJoinedRowsFromTable(Table.RULES));
					ret.addAll(row.getJoinedRowsFromTable(Table.OPERATORS));
					ret.addAll(row.getDirectedJoinedChildrenOfType(Table.PROBLEM_SPACES, false));
					return ret.toArray();
				}
				if (table == Table.OPERATORS) {
					ArrayList<ISoarDatabaseTreeItem> ret = row.getJoinedRowsFromTable(Table.RULES);
					return ret.toArray();
				}
				if (table == Table.RULES) {
					return new Object[0];
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
				return folder.getChildren(
						includeFolders,
						includeChildrenInFolders,
						includeJoinedItems,
						includeDirectionalJoinedItems,
						putDirectionalJoinedItemsInFolders,
						includeDatamapNodes).toArray();
			}
		}
		return new Object[0];
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

}
