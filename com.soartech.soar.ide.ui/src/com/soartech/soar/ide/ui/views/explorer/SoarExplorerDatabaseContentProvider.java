package com.soartech.soar.ide.ui.views.explorer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.sql.EditableColumn;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarExplorerDatabaseContentProvider implements ITreeContentProvider {

	private boolean includeFolders;
	private boolean includeItemsInFolders;
	private boolean includeJoinedItems;
	private boolean includeDirectionalJoinedItems;
	private boolean putDirectionalJoinedItemsInFolders;
	private boolean includeDatamapNodes;
	
	public SoarExplorerDatabaseContentProvider(boolean includeFolders,
			boolean includeItemsInFolders,
			boolean includeJoinedItems,
			boolean includeDirectionalJoinedItems,
			boolean putDirectionalJoinedItemsInFolders,
			boolean includeDatamapNodes) {
		this.includeFolders = includeFolders;
		this.includeItemsInFolders = includeItemsInFolders;
		this.includeJoinedItems = includeJoinedItems;
		this.includeDirectionalJoinedItems = includeDirectionalJoinedItems;
		this.putDirectionalJoinedItemsInFolders = putDirectionalJoinedItemsInFolders;
		this.includeDatamapNodes = includeDatamapNodes;
	}
	
	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof ISoarModel) {
			SoarDatabaseConnection conn = ((ISoarModel)element).getDatabase();
			Object[] ret = conn.selectAllFromTable(Table.AGENTS).toArray();
			return ret;
		}
		else if (element instanceof ISoarDatabaseTreeItem) {
			List<ISoarDatabaseTreeItem> ret = ((ISoarDatabaseTreeItem)element).getChildren(includeFolders, includeItemsInFolders, includeJoinedItems, includeDirectionalJoinedItems, putDirectionalJoinedItemsInFolders, includeDatamapNodes);
			return ret.toArray();
		}
		return new Object[]{};
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof ISoarModel) {
			return null;
		}
		else if (element instanceof SoarDatabaseRow) {
			ArrayList<SoarDatabaseRow> parents = ((SoarDatabaseRow)element).getParents();
			if (parents.size() > 0) {
				return parents.get(0);
			} else {
				return null;
			}
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof ISoarDatabaseTreeItem) {
			List<ISoarDatabaseTreeItem> children = ((ISoarDatabaseTreeItem)element).getChildren(includeFolders, includeItemsInFolders, includeJoinedItems, includeDirectionalJoinedItems, putDirectionalJoinedItemsInFolders, includeDatamapNodes);
			boolean ret = children.size() > 0;
			return ret;
		}
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		
	}

}
