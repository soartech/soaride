package com.soartech.soar.ide.ui.editors.datamap;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseUtil;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseDatamapContentProvider implements ITreeContentProvider {

	private ArrayList<ISoarDatabaseTreeItem> getChildren(ArrayList <? extends ISoarDatabaseTreeItem> elements, boolean includeLinkedChildren) {
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		for (ISoarDatabaseTreeItem element : elements) {
			if (element instanceof SoarDatabaseRow) {
				// should usually be true
				SoarDatabaseRow row = (SoarDatabaseRow) element;

				// If this is the root node (the problem space), only show
				// datamap nodes
				if (row.getTable() == Table.PROBLEM_SPACES) {
					ret.addAll(SoarDatabaseUtil.sortRowsByName(row.getChildrenOfType(Table.DATAMAP_IDENTIFIERS)));
				} else {
					Table[] tables = new Table[] { Table.DATAMAP_IDENTIFIERS, Table.DATAMAP_STRINGS, Table.DATAMAP_ENUMERATIONS, Table.DATAMAP_FLOATS, Table.DATAMAP_INTEGERS, };
					for (Table table : tables) {
						ret.addAll(SoarDatabaseUtil.sortRowsByName(row.getDirectedJoinedRowsFromTable(table)));
					}
				}

				if (includeLinkedChildren) {
					ArrayList<SoarDatabaseRow> linkedRows = row.getUndirectedJoinedRowsFromTable(row.getTable());
					ret.addAll(getChildren(linkedRows, false));
				}
			} else if (element instanceof SoarDatamapItemDuplicateGroup) {
				ret.addAll(getChildren(((SoarDatamapItemDuplicateGroup)element).getItems(), true));
			}
		}
		
		// Group duplicate children together

		/*
		HashMap<String, ArrayList<ISoarDatabaseTreeItem>> pathsToNodes = new HashMap<String, ArrayList<ISoarDatabaseTreeItem>>();
		for (ISoarDatabaseTreeItem item : ret) {
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow node = (SoarDatabaseRow) item;
				String key = node.getName() + " " + node.getTable();
				ArrayList<ISoarDatabaseTreeItem> list = pathsToNodes.get(key);
				if (list == null)
					list = new ArrayList<ISoarDatabaseTreeItem>();
				list.add(node);
				pathsToNodes.put(key, list);
			}
			else if (item instanceof SoarDatamapItemDuplicateGroup) {
				SoarDatamapItemDuplicateGroup group = (SoarDatamapItemDuplicateGroup) item;
				String key = group.getRow().getName() + " " + group.getRow().getTable();
				ArrayList<ISoarDatabaseTreeItem> list = pathsToNodes.get(key);
				if (list == null) list = new ArrayList<ISoarDatabaseTreeItem>();
				list.add(group);
				pathsToNodes.put(key, list);	
			}
		}
		ArrayList<SoarDatamapItemDuplicateGroup> groups = new ArrayList<SoarDatamapItemDuplicateGroup>();
		for (String key : pathsToNodes.keySet()) {
			ArrayList<ISoarDatabaseTreeItem> list = pathsToNodes.get(key);
			if (list.size() > 1) {
				SoarDatamapItemDuplicateGroup group = new SoarDatamapItemDuplicateGroup(list);
				groups.add(group);
			for (ISoarDatabaseTreeItem groupRow : list) {
					ret.remove(groupRow);
				}
				ret.add(group);
			}
		}
		*/

		SoarDatabaseUtil.sortRowsByName(ret);
		return ret;
	}

	@Override
	public Object[] getChildren(Object element) {
		ArrayList <ISoarDatabaseTreeItem> elements = new ArrayList<ISoarDatabaseTreeItem>();
		elements.add((ISoarDatabaseTreeItem)element);
		return getChildren(elements, true).toArray();
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
		return getChildren(element);
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
