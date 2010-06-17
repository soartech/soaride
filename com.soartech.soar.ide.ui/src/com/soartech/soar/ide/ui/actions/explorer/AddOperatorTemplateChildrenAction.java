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

public class AddOperatorTemplateChildrenAction extends Action {

	SoarDatabaseRow parent;
	TreeViewer tree;
	
	private class Rule {
		String name;
		String body;
		public Rule(String name, String body) {
			this.name = name;
			this.body = body;
		}
	}
	
	private class Operator {
		String name;
		ArrayList<Rule> rules;
		public Operator(String name, ArrayList<Rule> rules) {
			this.name = name;
			this.rules = rules;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	public AddOperatorTemplateChildrenAction(SoarDatabaseRow parent, TreeViewer tree) {
		super("Add operators from templates");
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
				
		// Maps operator names on lists of rules for that operator.
		// Each rule is a mapping of 
		HashMap<String, Operator> operatorMap = new HashMap<String, Operator>();
		ArrayList<String> operatorNames = new ArrayList<String>();
		
		Table parentTable = parent.getTable();
		if (parentTable == Table.PROBLEM_SPACES) {
			ArrayList<Rule> rules = new ArrayList<Rule>();
			String operatorName = "initialize-" + parentName;
			String name = "propose*" + operatorName;
			String body =
					  "sp {" + name + "\n"
					+ "   (state <s> ^superstate nil\n"
					+ "             -^name)\n"
					+ "-->\n"
					+ "   (<s> ^operator <o> +)\n"
					+ "   (<o> ^name initialize-" + parentName + ")\n"
					+ "}";
			rules.add(new Rule(name, body));
			
			name = "apply*" + operatorName;
			body =
					  "sp {" + name + "\n"
					+ "   (state <s> ^operator <o>)\n"
					+ "   (<o> ^name initialize-" + parentName + ")\n"
					+ "-->\n"
					+ "   (<s> ^name " + parentName + ")\n"
					+ "}";
			rules.add(new Rule(name, body));
			operatorNames.add(operatorName);
			operatorMap.put(operatorName, new Operator(operatorName, rules));
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
				operatorNames,
				contentProvider,
				SoarLabelProvider.createFullLabelProvider(null),
				"Select opreators");
		dialog.open();
		Object[] result = dialog.getResult();
		
		for (Object obj : result) {
			if (obj instanceof String) {
				String key = (String) obj;
				Operator operator = operatorMap.get(key);
				SoarDatabaseRow topLevelRow = parent.getTopLevelRow();
				SoarDatabaseRow childOperator = topLevelRow.createChild(Table.OPERATORS, key);
				SoarDatabaseRow.joinRows(parent, childOperator, parent.getDatabaseConnection());
				for (Rule rule : operator.rules) {
					SoarDatabaseRow childRule = topLevelRow.createChild(Table.RULES, rule.name);
					SoarDatabaseRow.joinRows(childOperator, childRule, childOperator.getDatabaseConnection());
					childRule.save(rule.body, null);
				}
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
