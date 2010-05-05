package com.soartech.soar.ide.core.sql;

import java.util.List;


public interface ISoarDatabaseTreeItem {
	List<ISoarDatabaseTreeItem> getChildren(boolean includeFolders,
			boolean includeChildrenInFolders,
			boolean includeJoinedItems,
			boolean includeDirectionalJoinedItems,
			boolean putDirectionalJoinedItemsInFolders,
			boolean includeDatamapNodes);

	boolean hasChildren();
}
