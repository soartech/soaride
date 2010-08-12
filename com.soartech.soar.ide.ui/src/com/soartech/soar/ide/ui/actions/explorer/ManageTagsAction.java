package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.sql.TraversalUtil;

public class ManageTagsAction extends Action {

	SoarDatabaseRow row;
	
	public ManageTagsAction(SoarDatabaseRow row) {
		super("Manage Tags");
		this.row = row;
	}
	
	@Override
	public void run() {
		super.run();
		SoarDatabaseRow agent = row.getTopLevelRow();
		ArrayList<SoarDatabaseRow> tags = agent.getChildrenOfType(Table.TAGS);
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ListSelectionDialog dialog = new ListSelectionDialog(shell, tags, new ArrayContentProvider(), new LabelProvider(), "Select tags for \"" + row.getName() + "\"");
		ArrayList<SoarDatabaseRow> parentTags = TraversalUtil.getTagsForRow(row);
		dialog.setInitialSelections(parentTags.toArray());
		dialog.open();
		Object[] result = dialog.getResult();
		HashSet<Object> selectedTags = new HashSet<Object>();
		for (Object obj : result) {
			selectedTags.add(obj);
		}
		for (SoarDatabaseRow tag : tags) {
			if (selectedTags.contains(tag)) {
				SoarDatabaseRow.joinRows(tag, row, tag.getDatabaseConnection());
			} else {
				SoarDatabaseRow.unjoinRows(tag, row, tag.getDatabaseConnection());
			}
		}
	}
}
