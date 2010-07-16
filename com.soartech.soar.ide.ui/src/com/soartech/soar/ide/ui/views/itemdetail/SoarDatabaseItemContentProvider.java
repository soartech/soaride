package com.soartech.soar.ide.ui.views.itemdetail;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.sun.xml.internal.ws.developer.MemberSubmissionEndpointReference.Elements;

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
				ISoarDatabaseTreeItem item = (ISoarDatabaseTreeItem)element;
				ArrayList<ISoarDatabaseTreeItem> ret = item.getChildren(
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
	
	public static ArrayList<ISoarDatabaseTreeItem> sortItems(ArrayList<ISoarDatabaseTreeItem> items) {
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
		String[] terms = {"elaborate-state", "propose", "evaluate", "compare", "elaborate-operator", "apply"};
		
		for (String term : terms) {
			String[] termSegments = term.split("/");
			for (ISoarDatabaseTreeItem item : items) {
				if (item instanceof SoarDatabaseRow) {
					SoarDatabaseRow row = (SoarDatabaseRow) item;
					if (row.getTable() == Table.RULES) {
						boolean matchesTerm = false;
						for (String termSegment : termSegments) {
							if (row.getName().toLowerCase().contains(termSegment)) {
								matchesTerm = true;
							}
						}
						if (row.getTable() == Table.RULES && matchesTerm && !added.contains(row)) {
							ret.add(row);
							added.add(row);
						}
					}
				}
			}
		}
		
		// Add other table types
		Table[] tables = {Table.RULES, Table.OPERATORS, Table.PROBLEM_SPACES, Table.AGENTS};
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
			Object[] ar = (Object[]) element;
			if (ar.length > 0) {
				Object firstElement = ar[0];
				if (firstElement instanceof SoarDatabaseRow) {
					SoarDatabaseRow row = (SoarDatabaseRow) firstElement;
					if (row.isDatamapNode()) {
						ArrayList<ISoarDatabaseTreeItem> ret = row.getUndirectedJoinedRowsFromTable(row.getTable());
						ret.add(row);
						for (ISoarDatabaseTreeItem item : ret) {
							assert item instanceof SoarDatabaseRow;
							((SoarDatabaseRow) item).setTerminal(true);
						}
						return ret.toArray();
					}
				}
			}
			return ar;
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
