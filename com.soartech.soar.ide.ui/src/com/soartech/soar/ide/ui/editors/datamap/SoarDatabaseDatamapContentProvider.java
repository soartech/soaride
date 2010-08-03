package com.soartech.soar.ide.ui.editors.datamap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseDatamapContentProvider implements ITreeContentProvider {

	private ArrayList<ISoarDatabaseTreeItem> getChildren(ISoarDatabaseTreeItem element, boolean includeLinkedChildren) {
		if (element instanceof SoarDatabaseRow) {
			// should usually be true
			SoarDatabaseRow row = (SoarDatabaseRow) element;
			boolean includeFolders = false;
			boolean includeItemsInFolders = false;
			boolean includeJoinedItems = false;
			boolean includeDirectionalJoinedItems = true;
			boolean putDirectionalJoinedItemsInFolders = false;
			boolean includeDatamapNodes = true;
			ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
			ret.addAll(row.getChildren(includeFolders,
					includeItemsInFolders,
					includeJoinedItems,
					includeDirectionalJoinedItems,
					putDirectionalJoinedItemsInFolders,
					includeDatamapNodes));
			
			// If this is the root node (the problem space), only show datamap nodes
			if (row.getTable() == Table.PROBLEM_SPACES) {
				ArrayList<ISoarDatabaseTreeItem> temp = new ArrayList<ISoarDatabaseTreeItem>();
				for (ISoarDatabaseTreeItem item : ret) {
					if (item instanceof SoarDatabaseRow &&
							((SoarDatabaseRow)item).getTable().isDatamapTable()) {
						temp.add(item);
					}
				}
				ret = temp;
			}
			
			if (includeLinkedChildren) {
				ArrayList<ISoarDatabaseTreeItem> linkedRows = row.getUndirectedJoinedRowsFromTable(row.getTable());
				for (ISoarDatabaseTreeItem item : linkedRows) {
					ret.addAll(getChildren(item, false));
				}
			}
			
			// Group duplicate children together
			/*
			ArrayList<ArrayList<ISoarDatabaseTreeItem>> duplicates = new ArrayList<ArrayList<ISoarDatabaseTreeItem>>();
			HashMap<String, ArrayList<SoarDatabaseRow>> pathsToNodes = new HashMap<String, ArrayList<SoarDatabaseRow>>();
			for (ISoarDatabaseTreeItem item : ret) {
				if (item instanceof SoarDatabaseRow) {
					SoarDatabaseRow node = (SoarDatabaseRow) item;
					String key = node.getPathName() + " " + node.getName();
					ArrayList<SoarDatabaseRow> list = pathsToNodes.get(key);
					if (list == null) list = new ArrayList<SoarDatabaseRow>();
					list.add(node);
					pathsToNodes.put(key, list);
				}
			}
			ArrayList<SoarDatamapItemDuplicateGroup> groups = new ArrayList<SoarDatamapItemDuplicateGroup>();
			for (String key : pathsToNodes.keySet()) {
				ArrayList<SoarDatabaseRow> list = pathsToNodes.get(key);
				if (list.size() > 1) {
					SoarDatamapItemDuplicateGroup group = new SoarDatamapItemDuplicateGroup(list);
					groups.add(group);
					for (SoarDatabaseRow groupRow : list) {
						ret.remove(groupRow);
					}
					ret.add(group);
				}
			}
			*/
			
			return ret;
		}
		
		return new ArrayList<ISoarDatabaseTreeItem>();
	}
	
	@Override
	public Object[] getChildren(Object element) {
		return getChildren((SoarDatabaseRow)element, true).toArray();
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
