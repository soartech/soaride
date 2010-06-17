package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

public class AddRuleTemplateChildrenAction extends Action {
	
	SoarDatabaseRow parent;
	TreeViewer tree;
	
	public AddRuleTemplateChildrenAction(SoarDatabaseRow parent, TreeViewer tree) {
		super("Add rules from templates");
		this.parent = parent;
		this.tree = tree;
	}
	
	@Override
	public void run() {
		String parentName = parent.getName().replace(' ', '-');
		SoarDatabaseRow ps = getProblemSpace();
		String problemSpace = null;
		if (ps != null) {
			problemSpace = ps.getName().replace(' ', '-');
		} else {
			problemSpace = "problem-space";
		}
		ArrayList<String> templateNames = new ArrayList<String>();
		
		HashMap<String, String> templates = new HashMap<String, String>();
		
		Table parentTable = parent.getTable();
		if (parentTable == Table.OPERATORS) {
			String name = "propose*" + parentName;
			String template =
					  "sp {" + name + "\n"
					+ "   (state <s> ^name " + problemSpace + ")\n"
					+ "-->\n"
					+ "   (<s> ^operator <o> + =)\n"
					+ "   (<o> ^name " + parentName + ")\n" + "}";
			templateNames.add(name);
			templates.put(name, template);

			name = "apply*" + parentName;
			template =
					  "sp {" + name + "\n"
					+ "   (state <s> ^operator <o>)\n"
					+ "   (<o> ^name " + parentName + ")\n"
					+ "-->\n" 
					+ "}";
			templateNames.add(name);
			templates.put(name, template);
		}
		else if (parentTable == Table.PROBLEM_SPACES) {
			String name = "elaborate*top-state*top-state";
			String template =
					  "sp {" + name + "\n"
					+ "   (state <s> ^superstate <u>)\n"
					+ "   (<u> ^name nil)\n"
					+ "-->\n"
					+ "   (<s> ^top-state <s>)\n"
					+ "}";
			templateNames.add(name);
			templates.put(name, template);
			
			name = "elaborate*state*top-state";
			template =
					  "sp {" + name + "\n"
					+ "   (state <s> ^superstate.top-state <ts>)\n"
					+ "-->\n"
					+ "   (<s> ^top-state <ts>)\n"
					+ "}";
			templateNames.add(name);
			templates.put(name, template);
			
			name = "elaborate*state*name";
			template =
					  "sp {" + name + "\n"
					+ "   (state <s> ^superstate.operator.name <name>)\n"
					+ "-->\n"
					+ "   (<s> ^name <name>)\n"
					+ "}";
			templateNames.add(name);
			templates.put(name, template);
		}
		
		IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object obj) {
				if (obj instanceof ArrayList<?>) {
					return ((ArrayList<?>)obj).toArray();
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
		
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ListSelectionDialog dialog = new ListSelectionDialog(shell,
				templateNames,
				contentProvider,
				SoarLabelProvider.createFullLabelProvider(null),
				"Select substate");
		dialog.open();
		Object[] result = dialog.getResult();
		
		for (Object obj : result) {
			if (obj instanceof String) {
				String key = (String) obj;
				String ruleBody = templates.get(key);
				SoarDatabaseRow child = parent.getTopLevelRow().createChild(Table.RULES, key);
				SoarDatabaseRow.joinRows(parent, child, parent.getDatabaseConnection());
				child.save(ruleBody, null);
			}
		}
		
		if (result.length > 0) {
			tree.setExpandedState(parent, true);
		}
	}
	
	private SoarDatabaseRow getProblemSpace() {
		ISelection selection = tree.getSelection();
		if (selection instanceof TreeSelection) {
			TreeSelection ts = (TreeSelection) selection;
			TreePath[] paths = ts.getPathsFor(parent);
			for (TreePath path : paths) { // can't imagine why length would be more than 1
				Object segment = path.getSegment(path.getSegmentCount() - 2);
				if (segment instanceof SoarDatabaseRow) {
					SoarDatabaseRow ret = (SoarDatabaseRow) segment;
					if (ret.getTable() == Table.PROBLEM_SPACES) {
						return ret;
					}
				}
			}
		}
		return null;
	}
}
