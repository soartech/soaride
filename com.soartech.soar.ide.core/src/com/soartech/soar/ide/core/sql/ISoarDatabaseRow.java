package com.soartech.soar.ide.core.sql;

import java.util.List;


public interface ISoarDatabaseRow {
	List<ISoarDatabaseRow> getChildren(boolean includeFolders,
			boolean includeChildrenInFolders,
			boolean includeJoinedItems,
			boolean includeDirectionalJoinedItems,
			boolean includeDatamapNodes);

	boolean hasChildren();
}
