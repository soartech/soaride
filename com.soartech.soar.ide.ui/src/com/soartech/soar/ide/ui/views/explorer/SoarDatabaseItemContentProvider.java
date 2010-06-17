package com.soartech.soar.ide.ui.views.explorer;

import java.util.ArrayList;

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor.ThisReferenceFinder;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

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
			try {
				ArrayList<ISoarDatabaseTreeItem> ret = ((ISoarDatabaseTreeItem)element).getChildren(
						includeFolders,
						includeItemsInFolders,
						includeJoinedItems,
						includeDirectionalJoinedItems,
						putDirectionalJoinedItemsInFolders,
						includeDatamapNodes);
				return ret.toArray();
			} catch (AbstractMethodError e) {
				e.printStackTrace();
			}
		}
		return new Object[0];
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
			return (Object[]) element;
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
