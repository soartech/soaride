package com.soartech.soar.ide.ui.editors.database;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseDatamapContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof SoarDatabaseRow) {
			// should be true
			SoarDatabaseRow row = (SoarDatabaseRow) element;
			boolean includeFolders = false;
			boolean includeItemsInFolders = false;
			boolean includeJoinedItems = false;
			boolean includeDirectionalJoinedItems = true;
			boolean putDirectionalJoinedItemsInFolders = false;
			boolean includeDatamapNodes = true;
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.addAll(row.getChildren(includeFolders,
					includeItemsInFolders,
					includeJoinedItems,
					includeDirectionalJoinedItems,
					putDirectionalJoinedItemsInFolders,
					includeDatamapNodes));

			// If this is the root node (the problem space), don't show substates.
			if (row.getTable() == Table.PROBLEM_SPACES) {
				ArrayList<Object> temp = new ArrayList<Object>();
				for (Object item : ret) {
					if (item instanceof SoarDatabaseRow &&
							((SoarDatabaseRow)item).getTable() != Table.PROBLEM_SPACES) {
						temp.add(item);
					}
				}
				ret = temp;
			}
			
			// If this is root node (<s>), show supstate attributes (if they
			// exist)
			if (row.getTable() == Table.DATAMAP_IDENTIFIERS) {
				ArrayList<SoarDatabaseRow> parents = row.getParents();
				for (SoarDatabaseRow parent : parents) {
					if (parent.getTable() == Table.PROBLEM_SPACES) {
						// row is <s>
						// Get its parent rows
						ArrayList<SoarDatabaseRow> superstates = parent.getDirectedJoinedParentsOfType(Table.PROBLEM_SPACES);
						for (SoarDatabaseRow ss : superstates) {
							ret.add(new SoarDatabaseDatamapSuperstateAttribute(parent, ss));
						}
					}
				}
			}
			
			return ret.toArray();
		}
		
		if (element instanceof SoarDatabaseDatamapSuperstateAttribute) {
			return ((SoarDatabaseDatamapSuperstateAttribute)element).getChildren(this).toArray();
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
		return getChildren(element);
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
