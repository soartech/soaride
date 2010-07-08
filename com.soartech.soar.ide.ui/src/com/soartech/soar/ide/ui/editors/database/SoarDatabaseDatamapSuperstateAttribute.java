package com.soartech.soar.ide.ui.editors.database;

import java.util.ArrayList;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseDatamapSuperstateAttribute implements ISoarDatabaseTreeItem {

	SoarDatabaseRow substate;
	SoarDatabaseRow superstate;
	
	public SoarDatabaseDatamapSuperstateAttribute(SoarDatabaseRow substate, SoarDatabaseRow superstate) {
		this.substate = substate;
		this.superstate = superstate;
	}

	public ArrayList<ISoarDatabaseTreeItem> getChildren(SoarDatabaseDatamapContentProvider contentProvider) {
		
		assert substate.getTable() == Table.PROBLEM_SPACES;
		
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		
		if (superstate == null) {
			return ret;
		}
		
		ArrayList<ISoarDatabaseTreeItem> superstateChildren = superstate.getChildrenOfType(Table.DATAMAP_IDENTIFIERS);
		for (ISoarDatabaseTreeItem item : superstateChildren) {
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow superstateChild = (SoarDatabaseRow) item;
				// this is superstate's <s>
				Object[] ar = contentProvider.getChildren(superstateChild);
				for (Object obj : ar) {
					if (obj instanceof ISoarDatabaseTreeItem) {
						ret.add((ISoarDatabaseTreeItem)obj);
					}
				}
			}
		}
		return ret;
	}

	public boolean hasChildren(SoarDatabaseDatamapContentProvider contentProvider) {
		return getChildren(contentProvider).size() > 0;
	}
	
	@Override
	public String toString() {
		if (superstate == null) {
			return "superstate (none)";
		}
		return "superstate (" + superstate.getName() + ")";
	}

	@Override
	public ArrayList<ISoarDatabaseTreeItem> getChildren(boolean includeFolders, boolean includeChildrenInFolders, boolean includeJoinedItems, boolean includeDirectionalJoinedItems,
			boolean putDirectionalJoinedItemsInFolders, boolean includeDatamapNodes) {
		return superstate.getChildren(includeFolders, includeChildrenInFolders, includeJoinedItems, includeDirectionalJoinedItems, putDirectionalJoinedItemsInFolders, includeDatamapNodes);
	}

	@Override
	public SoarDatabaseRow getRow() {
		return superstate;
	}

	@Override
	public boolean hasChildren() {
		return superstate.hasChildren();
	}
}
