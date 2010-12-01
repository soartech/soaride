package com.soartech.soar.ide.ui.actions.dragdrop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.TreeItem;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseExplorerDropAdapter extends ViewerDropAdapter {

	// Maps tables onto types of rows that can be dragged onto them,
	// resulting in an undirected join.
	HashMap<Table, HashSet<Table>> undirectedChildren;

	// Maps tables onto types of rown that can be dragged onto them,
	// resulting in a directed join.
	HashMap<Table, HashSet<Table>> directedChildren;
	
	// Maps tables that can be dragged onto each other in either direction,
	// resulting in an undirected join.
	//HashMap<Table, HashSet<Table>> undirectedJoins;

	SoarDatabaseRow target;
	
	public SoarDatabaseExplorerDropAdapter(Viewer viewer) {
		super(viewer);
		directedChildren = new HashMap<Table, HashSet<Table>>();

		
		undirectedChildren = new HashMap<Table, HashSet<Table>>();
		HashSet<Table> tagChildren = new HashSet<Table>();
		tagChildren.add(Table.RULES);
		tagChildren.add(Table.PROBLEM_SPACES);
		tagChildren.add(Table.OPERATORS);
		tagChildren.add(Table.TAGS);
		directedChildren.put(Table.TAGS, tagChildren);
		
		HashSet<Table> problemSpaceChildren = new HashSet<Table>();
		problemSpaceChildren.add(Table.OPERATORS);
		problemSpaceChildren.add(Table.RULES);
		problemSpaceChildren.add(Table.PROBLEM_SPACES);
		problemSpaceChildren.add(Table.TAGS);
		directedChildren.put(Table.PROBLEM_SPACES, problemSpaceChildren);
		HashSet<Table> operatorChildren = new HashSet<Table>();
		operatorChildren.add(Table.RULES);
		operatorChildren.add(Table.PROBLEM_SPACES);
		operatorChildren.add(Table.TAGS);
		directedChildren.put(Table.OPERATORS, operatorChildren);


		/*
		undirectedJoins = new HashMap<Table, HashSet<Table>>();
		HashSet<Table> tagJoins = new HashSet<Table>();
		tagJoins.add(Table.PROBLEM_SPACES);
		tagJoins.add(Table.OPERATORS);
		tagJoins.add(Table.RULES);
		undirectedJoins.put(Table.TAGS, tagJoins);
		*/
	}

	@Override
	public boolean performDrop(Object data) {
		ArrayList<SoarDatabaseRow> rows = getRowsFromDropData(data);
		boolean ret = false;
		for (SoarDatabaseRow row : rows) {
			HashSet<Table> set = undirectedChildren.get(target.getTable());
			if (set != null && set.contains(row.getTable())) {
				SoarDatabaseRow.joinRows(row, target, row.getDatabaseConnection());
				ret = true;
			}
			set = directedChildren.get(target.getTable());
			if (set != null && set.contains(row.getTable())) {
				SoarDatabaseRow.directedJoinRows(target, row, target.getDatabaseConnection());
				ret = true;
			}
			/*
			set = undirectedJoins.get(target.getTable());
			if (set != null && set.contains(row.getTable())) {
				SoarDatabaseRow.joinRows(row, target, row.getDatabaseConnection());
				ret = true;
			}
			set = undirectedJoins.get(row.getTable());
			if (set != null && set.contains(target.getTable())) {
				SoarDatabaseRow.joinRows(row, target, row.getDatabaseConnection());
				ret = true;
			}
			*/
		}
		return ret;
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		if (target instanceof SoarDatabaseRow) {
			SoarDatabaseRow row = (SoarDatabaseRow) target;
			this.target = row;
			return true;
		}
		return false;
	}
	
	private ArrayList<SoarDatabaseRow> getRowsFromDropData(Object data) {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		if (data instanceof StructuredSelection) {
			StructuredSelection ss = (StructuredSelection) data;
			for (Object obj : ss.toArray()) {
				if (obj instanceof TreeItem) {
					TreeItem item = (TreeItem) obj;
					Object itemData = item.getData();
					if (itemData instanceof SoarDatabaseRow) {
						SoarDatabaseRow row = (SoarDatabaseRow) itemData;
						ret.add(row);
					}
				}
			}
		}
		return ret;
	}

}
