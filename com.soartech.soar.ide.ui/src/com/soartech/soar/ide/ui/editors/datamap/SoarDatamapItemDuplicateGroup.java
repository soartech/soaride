package com.soartech.soar.ide.ui.editors.datamap;

import java.util.ArrayList;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class SoarDatamapItemDuplicateGroup implements ISoarDatabaseTreeItem {

	ArrayList<SoarDatabaseRow> items;
	
	public SoarDatamapItemDuplicateGroup(ArrayList<ISoarDatabaseTreeItem> items) {
		this.items = new ArrayList<SoarDatabaseRow>();
		for (ISoarDatabaseTreeItem item : items) {
			if (item instanceof SoarDatabaseRow) {
				this.items.add((SoarDatabaseRow)item);
			}
			if (item instanceof SoarDatamapItemDuplicateGroup) {
				this.items.addAll(((SoarDatamapItemDuplicateGroup)item).getItems());
			}
		}
	}
	
	@Override
	public String toString() {
		return getRow().toString();
	}
	
	@Override
	public ArrayList<ISoarDatabaseTreeItem> getChildren(boolean includeFolders, boolean includeChildrenInFolders, boolean includeJoinedItems, boolean includeDirectionalJoinedItems,
			boolean putDirectionalJoinedItemsInFolders, boolean includeDatamapNodes) {
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		for (SoarDatabaseRow item : items) {
			ret.addAll(item.getChildren(includeFolders, includeChildrenInFolders, includeJoinedItems, includeDirectionalJoinedItems, putDirectionalJoinedItemsInFolders, includeDatamapNodes));
		}
		return ret;
	}

	@Override
	public boolean hasChildren() {
		for (SoarDatabaseRow item : items) {
			if (item.hasChildren()) return true;
		}
		return false;
	}

	@Override
	public SoarDatabaseRow getRow() {
		return items.get(0);
	}
	
	public ArrayList<SoarDatabaseRow> getItems() {
		return items;
	}
}
