package com.soartech.soar.ide.ui.editors.datamap;

import java.util.ArrayList;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class SoarDatamapItemDuplicateGroup implements ISoarDatabaseTreeItem {

	ArrayList<SoarDatabaseRow> items;
	
	public SoarDatamapItemDuplicateGroup(ArrayList<SoarDatabaseRow> items) {
		this.items = items;
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
		// Not meaningful in this context.
		// Better to throw an exception and fix the problem than to
		// return a dummy value.
		throw new NotImplementedException();
	}

}
