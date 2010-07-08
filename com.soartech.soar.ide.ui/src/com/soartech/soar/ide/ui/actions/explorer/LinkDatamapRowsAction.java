package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class LinkDatamapRowsAction extends Action {

	SoarDatabaseRow first = null;
	SoarDatabaseRow second = null;
	
	public LinkDatamapRowsAction(SoarDatabaseRow first, SoarDatabaseRow second) {
		if (first.getTable() == second.getTable() && first.getTable().isDatamapTable()) {
			this.first = first;
			this.second = second;
		}
	}
	
	@Override
	public void run() {
		
		if (first == null || second == null) {
			return;
		}
		
		SoarDatabaseRow.joinRows(first,
				second,
				first.getDatabaseConnection());
		
		// copy children relationships so that linked attributes share the same substructure
		// Actually, don't do this -- make the illusion that the items are shared on the
		// content provider side instead.
		/*
		ArrayList<ISoarDatabaseTreeItem> firstChildren = first.getDirectedJoinedChildren(false);
		ArrayList<ISoarDatabaseTreeItem> secondChildren = second.getDirectedJoinedChildren(false);
		
		for (ISoarDatabaseTreeItem item : firstChildren) {
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow child = (SoarDatabaseRow) item;
				if (!secondChildren.contains(child)) {
					SoarDatabaseRow.directedJoinRows(second, child, second.getDatabaseConnection());
				}
			}
		}
		
		for (ISoarDatabaseTreeItem item : secondChildren) {
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow child = (SoarDatabaseRow) item;
				if (!firstChildren.contains(child)) {
					SoarDatabaseRow.directedJoinRows(first, child, first.getDatabaseConnection());
				}
			}
		}
		*/
	}
}
