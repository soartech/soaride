package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.JoinType;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

public class AddSubstateAction extends Action {

	SoarDatabaseRow superstate;
	boolean selectExisting;
	TreeViewer tree;
	JoinType joinType;
	
	public AddSubstateAction(SoarDatabaseRow superstate, boolean selectExisting, TreeViewer tree, JoinType joinType) {
		super(superstate.getTable() == Table.OPERATORS ? "Add " + joinType.englishName() : joinType.shortEnglishName());
		this.superstate = superstate;
		this.tree = tree;
		this.selectExisting = selectExisting;
		this.joinType = joinType;
	}

	@Override
	public void run() {
		if (selectExisting) {
			runExisting();
		} else {
			runNew();
		}
	}
	
	private void runNew() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String title = "New Substate";
		String message = "Enter Name:";
		String initialValue = (joinType == JoinType.OPERATOR_NO__CHANGE_IMPASSE ? Table.OPERATORS.soarName() + "-name" : Table.PROBLEM_SPACES.soarName() + "-name");

		InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
		dialog.open();
		String result = dialog.getValue();
		
		if (result != null && result.length() > 0) {
			if (joinType == JoinType.OPERATOR_NO__CHANGE_IMPASSE) {
				// In this case, create a child operator and then create a child problem space under that operator.
				// OR, if a child operator with the given name already exists, just create a child problem space under that operator.
				SoarDatabaseRow child = null;
				if (superstate.getTable() == Table.OPERATORS) {
					child = superstate;
				} else {
					ArrayList<ISoarDatabaseTreeItem> children = superstate.getDirectedJoinedChildrenOfTypeNamed(Table.OPERATORS, result, false, false);
					if (children.size() > 0) {
						child = (SoarDatabaseRow) children.get(0);
					} else {
						child = superstate.getTopLevelRow().createChild(Table.OPERATORS, result);
						SoarDatabaseRow.directedJoinRows(superstate, child, superstate.getDatabaseConnection());
					}
				}
				
				// Add substate to operator.
				SoarDatabaseRow substate = superstate.getTopLevelRow().createChild(Table.PROBLEM_SPACES, result);
				SoarDatabaseRow.directedJoinRows(child, substate, superstate.getDatabaseConnection());
				SoarDatabaseRow.setDirectedJoinType(child, substate, joinType);
			} else {
				SoarDatabaseRow child = superstate.getTopLevelRow().createChild(Table.PROBLEM_SPACES, result);
				SoarDatabaseRow.directedJoinRows(superstate, child, superstate.getDatabaseConnection());
				SoarDatabaseRow.setDirectedJoinType(superstate, child, joinType);
			}
		}
		
		tree.setExpandedState(superstate, true);
	}
	
	private void runExisting() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object obj) {
				if (obj instanceof SoarDatabaseRow) {
					SoarDatabaseRow row = (SoarDatabaseRow) obj;
					SoarDatabaseRow topLevelRow = row.getTopLevelRow();
					ArrayList<SoarDatabaseRow> children = topLevelRow.getChildrenOfType(Table.PROBLEM_SPACES);
					ArrayList<SoarDatabaseRow> filteredChildren = new ArrayList<SoarDatabaseRow>();
					for (SoarDatabaseRow child : children) {
						filteredChildren.add((SoarDatabaseRow) child);
					}
					return filteredChildren.toArray();
				}
				return new Object[0];
			}

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
			}
			
		};

		ListDialog dialog = new ListDialog(shell);
		dialog.setContentProvider(contentProvider);
		dialog.setLabelProvider(SoarLabelProvider.createFullLabelProvider());
		dialog.setTitle("Select substate");
		dialog.setInput(superstate);
		dialog.open();
		Object[] result = dialog.getResult();
		if (result[0] instanceof SoarDatabaseRow) {
			SoarDatabaseRow selectedRow = (SoarDatabaseRow) result[0];
			if (selectedRow.getTable() == Table.PROBLEM_SPACES) { // Should be true
				SoarDatabaseRow.directedJoinRows(superstate, selectedRow, superstate.getDatabaseConnection());
				SoarDatabaseRow.setDirectedJoinType(superstate, selectedRow, joinType);
			}
		}
	}
}
