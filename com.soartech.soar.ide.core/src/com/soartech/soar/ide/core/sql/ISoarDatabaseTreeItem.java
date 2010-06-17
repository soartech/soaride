package com.soartech.soar.ide.core.sql;

import java.util.ArrayList;

public interface ISoarDatabaseTreeItem {
	ArrayList<ISoarDatabaseTreeItem> getChildren(boolean includeFolders,
			boolean includeChildrenInFolders,
			boolean includeJoinedItems,
			boolean includeDirectionalJoinedItems,
			boolean putDirectionalJoinedItemsInFolders,
			boolean includeDatamapNodes);

	boolean hasChildren();
}
