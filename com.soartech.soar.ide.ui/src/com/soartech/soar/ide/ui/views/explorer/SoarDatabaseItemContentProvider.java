package com.soartech.soar.ide.ui.views.explorer;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseJoinFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRowFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseItemContentProvider implements ITreeContentProvider {

	static final boolean includeFolders = true;
	static final boolean includeItemsInFolders = false;
	static final boolean includeJoinedItems = true;
	static final boolean includeDirectionalJoinedItems = true;
	static final boolean putDirectionalJoinedItemsInFolders = true;
	static final boolean includeDatamapNodes = false;
	
	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof ISoarModel) {
			SoarDatabaseConnection conn = ((ISoarModel)element).getDatabase();
			Object[] ret = conn.selectAllFromTable(Table.AGENTS).toArray();
			return ret;
		}
		if (element instanceof ISoarDatabaseTreeItem) {
			if (element instanceof SoarDatabaseRow) {
				SoarDatabaseRow row = (SoarDatabaseRow) element;
				if (row.isTerminal()) {
					return new Object[0];
				}
				if (row.isDatamapNode()) {
					ArrayList<ISoarDatabaseTreeItem> ret = row.getUndirectedJoinedRowsFromTable(row.getTable());
					for (ISoarDatabaseTreeItem item : ret) {
						assert item instanceof SoarDatabaseRow;
						((SoarDatabaseRow)item).setTerminal(true);
					}
					return ret.toArray();
				}
			}
			try {
				ArrayList<ISoarDatabaseTreeItem> ret = ((ISoarDatabaseTreeItem)element).getChildren(
						includeFolders,
						includeItemsInFolders,
						includeJoinedItems,
						includeDirectionalJoinedItems,
						putDirectionalJoinedItemsInFolders,
						includeDatamapNodes);
				ret = sortItems(ret);
				return ret.toArray();
			} catch (AbstractMethodError e) {
				e.printStackTrace();
			}
		}
		return new Object[0];
	}
	
	private ArrayList<ISoarDatabaseTreeItem> sortItems(ArrayList<ISoarDatabaseTreeItem> items) {
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		HashSet<ISoarDatabaseTreeItem> added = new HashSet<ISoarDatabaseTreeItem>();
		
		/* Order like this:
		 * Rules:
		 *   Propose
		 *   Evaluate / Select
		 *   Apply
		 *   Others
		 * Operators
		 * Problem Spaces
		 * Agents
		 * Others 
		 */
		
		// Add rules
		String[] terms = {"propose", "evaluate/select", "apply"};
		
		for (String term : terms) {
			String[] termSegments = term.split("/");
			for (ISoarDatabaseTreeItem item : items) {
				if (item instanceof SoarDatabaseRow) {
					SoarDatabaseRow row = (SoarDatabaseRow) item;
					boolean matchesTerm = false;
					for (String termSegment : termSegments) {
						if (row.getName().toLowerCase().contains(termSegment)) {
							matchesTerm = true;
						}
					}
					if (row.getTable() == Table.RULES
							&& matchesTerm
							&& !added.contains(row)) {
						ret.add(row);
						added.add(row);
					}
				}
			}
		}
		
		// Add other table types
		Table[] tables = {Table.OPERATORS, Table.PROBLEM_SPACES, Table.AGENTS};
		for (Table table : tables) {
			for (ISoarDatabaseTreeItem item : items) {
				if (item instanceof SoarDatabaseRow) {
					SoarDatabaseRow row = (SoarDatabaseRow) item;
					if (row.getTable() == table
							&& !added.contains(row)) {
						ret.add(row);
						added.add(row);
					}
				}
			}
		}
		
		// Add all others
		for (ISoarDatabaseTreeItem item : items) {
			if(!added.contains(item)) {
				ret.add(item);
				added.add(item);
			}
		}
		
		return ret;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	@Override
	public Object[] getElements(Object element) {
		if (element.getClass().isArray()) {
			return (Object[]) element;
		}
		return getChildren(element);
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
