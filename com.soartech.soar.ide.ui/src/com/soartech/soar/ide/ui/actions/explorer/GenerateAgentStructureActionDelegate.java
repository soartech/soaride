package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.dialogs.ListDialog;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.sql.TraversalUtil;
import com.soartech.soar.ide.core.sql.Triple;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

public class GenerateAgentStructureActionDelegate implements IWorkbenchWindowActionDelegate {
	
	boolean applyAll;
	Shell shell;
	StructuredSelection ss;
	SoarDatabaseRow row;
	public boolean forceApplyAll = false;
	
	@Override
	public void run(IAction action) {
		SoarDatabaseRow agent = SoarUiModelTools.selectAgent();
		if (agent != null) {
			runWithAgent(agent, null);
		}
	}
	
	/**
	 * 
	 * @param agent
	 * @param monitor
	 * @return List of error messages
	 */
	public ArrayList<String> runWithAgent(SoarDatabaseRow agent, IProgressMonitor monitor) {
		assert agent.getTable() == Table.AGENTS;
		ArrayList<String> errors = new ArrayList<String>();
		HashMap<String, SoarDatabaseRow> problemSpaces = TraversalUtil.getProblemSpacesMap(agent);
		HashMap<String, SoarDatabaseRow> operators = TraversalUtil.getOperatorsMap(agent);
		ArrayList<ISoarDatabaseTreeItem> rules = TraversalUtil.getRelatedRules(agent);
		
		if (monitor != null) {
			monitor.beginTask("Generating Project Structure", rules.size());
		}

		applyAll = forceApplyAll;

		System.out.println("Running Generate Agent Structure Action");
		
		for (int i = 0; i < rules.size(); ++i) {
			if (monitor != null) {
				monitor.worked(1);
			}
			ISoarDatabaseTreeItem ruleItem = rules.get(i);
			assert ruleItem instanceof SoarDatabaseRow;
			SoarDatabaseRow rule = (SoarDatabaseRow) ruleItem;
			assert rule.getTable() == Table.RULES;

			if (monitor != null) {
				monitor.subTask("Rule: " + rule.getName());
			}
			
			ArrayList<Triple> triples = TraversalUtil.getTriplesForRule(rule);

			// Find all triples which match <s>.operator.name and whose value is a constant.
			// The collection of all these constants are the names of related operators.
			// For each operator name, add this rule to the operator.
			// Either find an existing operator of the appropriate name, or create one from scratch.
			// Even better is to find an operator of the right name that is already joined to this rule.
			// If there is more than one operator with that name (shouldn't happen), signal an error.
			// Confirm with the user before making any changes to the database.
			ArrayList<String> relatedOperatorNames = new ArrayList<String>();
			for (Triple triple : triples) {
				if (triple.isOperatorName() && triple.valueIsConstant()) {
					relatedOperatorNames.add(triple.value);
				}
			}
			for (String operatorName : relatedOperatorNames) {
				SoarDatabaseRow operator = operators.get(operatorName);
				if (operator != null) {
					// Operator with the right name exists
					proposeDirectedJoin(rule, operator);
				} else {
					// Operator with the right name doesn't exist
					SoarDatabaseRow newRow = proposeCreateAndDirectedJoin(rule, Table.OPERATORS, operatorName);
					if (newRow != null) {
						operators.put(operatorName, newRow);
					}
				}
			}
			// update operators
			operators = TraversalUtil.getOperatorsMap(agent);

			// Find all triples which match <s>.name and whose value is a constant.
			// The collection of all these constants are the names of related states.
			// If this rule has no related operators: for each state name, add this rule to the state.
			// Either find an existing state of the appropriate name, or create one from scratch.
			// Even better is to find a state of the right name that is already joined to this rule.
			// If there is more than one state with that name (shouldn't happen), signal an error.
			// Confirm with the user before making any changes to the database.
			ArrayList<String> relatedStateNames = new ArrayList<String>();
			for (Triple triple : triples) {
				if (triple.isStateName() && triple.valueIsConstant()) {
					relatedStateNames.add(triple.value);
				}
			}
			if (relatedOperatorNames.size() == 0) {
				for (String stateName : relatedStateNames) {
					SoarDatabaseRow state = problemSpaces.get(stateName);
					if (state != null) {
						// Operator with the right name exists
						proposeDirectedJoin(rule, state);
					} else {
						// Operator with the right name doesn't
						// exist
						SoarDatabaseRow newRow = proposeCreateAndDirectedJoin(rule, Table.PROBLEM_SPACES, stateName);
						if (newRow != null) {
							problemSpaces.put(stateName, newRow);
						}
					}
				}
			} else {
				for (String relatedOperatorName : relatedOperatorNames) {
					SoarDatabaseRow relatedOperator = operators.get(relatedOperatorName);
					if (relatedOperator != null) {
						for (String stateName : relatedStateNames) {
							SoarDatabaseRow state = problemSpaces.get(stateName);
							if (state != null) {
								// Operator with the right name exists
								proposeDirectedJoin(relatedOperator, state);
							} else {
								// Operator with the right name doesn't exist
								SoarDatabaseRow newRow = proposeCreateAndDirectedJoin(relatedOperator, Table.PROBLEM_SPACES, stateName);
								if (newRow != null) {
									problemSpaces.put(stateName, newRow);
								}
							}
						}
					}
				}
			}
			// update problem spaces
			problemSpaces = TraversalUtil.getProblemSpacesMap(agent);

			// Find all triples which match <s>.superstate.operator.name and have a constant as their value.
			// The value of each of those triples are the related superstate operators.
			// For each related state (see above), for each superstate operator, propose a join.
			// Confirm with the user before making any changes to the database.
			// Keep track of these superstate operators, and later propose to add them to superstates.
			ArrayList<String> superstateOperators = new ArrayList<String>();
			for (Triple triple : triples) {
				if (triple.isSuperstateOperatorName() && triple.valueIsConstant()) {
					superstateOperators.add(triple.value);
				}
			}
			ArrayList<SoarDatabaseRow> superstateOperatorRows =  new ArrayList<SoarDatabaseRow>();
			for (String relatedStateName : relatedStateNames) {
				SoarDatabaseRow relatedState = problemSpaces.get(relatedStateName);
				if (relatedState != null) {
					for (String superstateOperatorName : superstateOperators) {
						SoarDatabaseRow superstateOperator = operators.get(superstateOperatorName);
						if (superstateOperator != null) {
							superstateOperatorRows.add(superstateOperator);
							proposeDirectedJoin(relatedState, superstateOperator);
						} else {
							SoarDatabaseRow newOperator = proposeCreateAndDirectedJoin(relatedState, Table.OPERATORS, superstateOperatorName);
							if (newOperator != null) {
								superstateOperatorRows.add(newOperator);
							}
						}
					}
				}
			}
			
			// Find all triples which match <s>.superstate.name and have a constant as their value.
			// The value of each of those triples are the related superstates.
			// For each related state (see above), for each superstate, if the states are not joined, propose a join.
			// UNLESS there are superstate operators, in which case don't propose a join, but keep track of superstates still.
			// Confirm with the user before making any changes to the database.
			// Keep track of superstate rows to attatch them to superstate operators.
			boolean hasSuperstateOperator = superstateOperatorRows.size() > 0;
			ArrayList<String> relatedSuperstateNames = new ArrayList<String>();
			for (Triple triple : triples) {
				if (triple.isSuperstateName() && triple.valueIsConstant()) {
					relatedSuperstateNames.add(triple.value);
				}
			}
			for (String relatedStateName : relatedStateNames) {
				SoarDatabaseRow relatedState = problemSpaces.get(relatedStateName);
				if (relatedState != null) {
					for (String relatedSuperstateName : relatedSuperstateNames) {
						SoarDatabaseRow superstate = problemSpaces.get(relatedSuperstateName);
						if (superstate != null) {
							if (!hasSuperstateOperator) {
								proposeDirectedJoin(relatedState, superstate);
							} else {
								for (SoarDatabaseRow operator : superstateOperatorRows) {
									proposeDirectedJoin(operator, superstate);
								}
							}
						} else {
							if (!hasSuperstateOperator) {
								proposeCreateAndDirectedJoin(relatedState, Table.PROBLEM_SPACES, relatedSuperstateName);
							} else {
								for (SoarDatabaseRow operator : superstateOperatorRows) {
									proposeCreateAndDirectedJoin(operator, Table.PROBLEM_SPACES, relatedSuperstateName);
								}						
							}
						}
					}
				}
			}
		}
		
		// Find problem spaces and operators that have the same name.
		// Make those problem spaces children of those operators.
		for (String problemSpaceName : problemSpaces.keySet()) {
			for (String operatorName : operators.keySet()) {
				if (problemSpaceName.equals(operatorName)) {
					SoarDatabaseRow problemSpace = problemSpaces.get(problemSpaceName);
					SoarDatabaseRow operator = operators.get(operatorName);
					proposeDirectedJoin(problemSpace, operator);
				}
			}
		}
		
		if (monitor != null) {
			monitor.done();
		}
		
		return errors;
	}
	
	private void proposeJoin(SoarDatabaseRow first, SoarDatabaseRow second) {
		if (SoarDatabaseRow.rowsAreJoined(first, second, first.getDatabaseConnection())) {
			return;
		}
		boolean join = applyAll;
		if (!join) {
			String title = "Join project elements?";
			String message = "Join the " + first.getTable().englishName() + " \"" + first.getName() + "\" to the " + second.getTable().englishName() + " \"" + second.getName() + "\"?";
			String[] options = { "OK", "Apply All", "Cancel" };
			Image image = MessageDialog.getDefaultImage();
			MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, options, 0);
			int result = dialog.open();
			if (result == 1) {
				applyAll = true;
			}
			if (result == 0 || applyAll) {
				join = true;
			}
		}
		if (join) {
			SoarDatabaseRow.joinRows(first, second, first.getDatabaseConnection());
		}
		return;
	}
	
	/**
	 * 
	 * @param first Child row
	 * @param second Parent row
	 */
	private void proposeDirectedJoin(SoarDatabaseRow first, SoarDatabaseRow second) {
		if (SoarDatabaseRow.rowsAreDirectedJoined(second, first, first.getDatabaseConnection())) {
			return;
		}
		boolean join = applyAll;
		if (!join) {
			String title = "Join project elements?";
			String message = "Join the " + first.getTable().englishName() + " \"" + first.getName() + "\" to the " + second.getTable().englishName() + " \"" + second.getName() + "\"?";
			String[] options = { "OK", "Apply All", "Cancel" };
			Image image = MessageDialog.getDefaultImage();
			MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, options, 0);
			int result = dialog.open();
			if (result == 1) {
				applyAll = true;
			}
			if (result == 0 || applyAll) {
				join = true;
			}
		}
		if (join) {
			SoarDatabaseRow.directedJoinRows(second, first, first.getDatabaseConnection());
		}
	}
	
	private SoarDatabaseRow proposeCreateAndJoin(SoarDatabaseRow first, Table type, String second) {
		
		boolean join = applyAll;
		if (!join) {
			String title = "Create and join project elements?";
			String message = "Create a " + type.englishName() + " \"" + second + "\" and join the " + first.getTable().englishName() + " \"" + first.getName() + "\" to it?";
			String[] options = { "OK", "Apply All", "Cancel" };
			Image image = MessageDialog.getDefaultImage();
			MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, options, 0);
			int result = dialog.open();
			if (result == 1) {
				applyAll = true;
			}
			if (result == 0 || applyAll)
				join = true;
		}
		if (join) {
			SoarDatabaseRow agent = first.getTopLevelRow();
			SoarDatabaseRow newRow = agent.createChild(type, second);
			SoarDatabaseRow.joinRows(first, newRow, first.getDatabaseConnection());
			return newRow;
		}
		return null;
	}
	
	/**
	 * 
	 * @param first The existing child row.
	 * @param type The type of parent row to create.
	 * @param second The name of the parent row to create.
	 */
	private SoarDatabaseRow proposeCreateAndDirectedJoin(SoarDatabaseRow first, Table type, String second) {
		boolean join = applyAll;
		if (!join) {
		String title = "Create and join project elements?";
		String message = "Create a " + type.englishName() + " \"" + second + "\" and join the " + first.getTable().englishName() + " \"" + first.getName() + "\" to it?";
		String[] options = { "OK", "Apply All", "Cancel" };
		Image image = MessageDialog.getDefaultImage();
		MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, options, 0);
		int result = dialog.open();
		if (result == 1) applyAll = true;
		if (result == 0 || applyAll) join = true;
		}
		if (join) {
			SoarDatabaseRow agent = first.getTopLevelRow();
			if (agent.getTable() == Table.AGENTS) {
				SoarDatabaseRow newRow = agent.createChild(type, second);
				SoarDatabaseRow.directedJoinRows(newRow, first, first.getDatabaseConnection());
				return newRow;
			}
			else {
				System.out.println("Top level row not of type agent");
				//first.getTopLevelRow();
			}
		}
		return null;
	}
	
	/*
	 * 
	 */
	
	private void proposeAddRuleToState(SoarDatabaseRow rule, String stateName) {
		SoarDatabaseRow state = getRowOfTypeNamed(Table.PROBLEM_SPACES, stateName);
		if (!SoarDatabaseRow.rowsAreJoined(rule, state, rule.getDatabaseConnection())) {
			proposeDirectedJoin(rule, state);
		}
	}
	
	private void proposeAddStateToSuperstate(SoarDatabaseRow rule, HashSet<String> stateNames, String superstateName) {
		
		// First, find the states currently joined to the operator.
		ArrayList<SoarDatabaseRow> states = rule.getJoinedRowsFromTable(Table.PROBLEM_SPACES);
		
		// If there are none, don't propose a join.
		if (states.size() == 0) return;
		
		// If there is one whose name is in stateNames, propose that join.
		// If there is more than one, choose from those states.
		ArrayList<SoarDatabaseRow> problemSpaces = new ArrayList<SoarDatabaseRow>();
		for (SoarDatabaseRow state : states) {
			assert state.getTable() == Table.PROBLEM_SPACES;
			String stateName = state.getName();
			if (stateNames.contains(stateName)) {
				problemSpaces.add(state);
			}
		}
		
		if (problemSpaces.size() == 1) {
			SoarDatabaseRow state = problemSpaces.get(0);
			SoarDatabaseRow superstate = getRowOfTypeNamed(Table.PROBLEM_SPACES, superstateName);
			proposeDirectedJoin(state, superstate);
		} else if (problemSpaces.size() > 1) {
			SoarDatabaseRow problemSpace = getRowFromList(problemSpaces, Table.PROBLEM_SPACES);
			SoarDatabaseRow superstate = getRowOfTypeNamed(Table.PROBLEM_SPACES, superstateName);
			proposeDirectedJoin(problemSpace, superstate);
		} else {
			SoarDatabaseRow problemSpace = getRowFromList(states, Table.PROBLEM_SPACES);
			SoarDatabaseRow superstate = getRowOfTypeNamed(Table.PROBLEM_SPACES, superstateName);
			proposeDirectedJoin(problemSpace, superstate);
		}
	}
	
	private SoarDatabaseRow getRowFromList(ArrayList<?> list, Table type) {
		String title = "Select " + type.englishName();
		String message = null;
		Object[] selectedItems = openListDialog(list, title, message);
		if (selectedItems.length > 0) {
			return (SoarDatabaseRow) selectedItems[0];
		}
		return null;
	}
	
	private void proposeAddRuleToOperator(SoarDatabaseRow rule, String operatorName) {
		SoarDatabaseRow operator = getRowOfTypeNamed(Table.OPERATORS, operatorName);
		proposeDirectedJoin(rule, operator);
	}
		
	private SoarDatabaseRow getRowOfTypeNamed(Table type, String name) {
		ArrayList<SoarDatabaseRow> rowsOfType = row.getJoinedRowsFromTable(type);
		ArrayList<SoarDatabaseRow> rowsWithName = new ArrayList<SoarDatabaseRow>();
		for (SoarDatabaseRow row : rowsOfType) {
			assert row.getTable() == type;
			if (row.getName().equals(name)) {
				rowsWithName.add(row);
			}
		}
		if (rowsWithName.size() == 1) {
			return rowsWithName.get(0);
		}
		
		Object[] selectedItems = openListDialog(rowsWithName, "Select " + type.englishName(), null);
		if (selectedItems.length > 0) {
			return (SoarDatabaseRow) selectedItems[0];
		}
		
		return null;
	}
	
	private Object[] openListDialog(ArrayList<?> items, String title, String message) {
		ListDialog dialog = new ListDialog(shell);
		dialog.setContentProvider(new ArrayContentProvider());
		dialog.setLabelProvider(SoarLabelProvider.createFullLabelProvider());
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setInput(items);
		dialog.open();
		Object[] result = dialog.getResult();
		return result;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof StructuredSelection) {
			this.ss = (StructuredSelection) selection;
		}
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow arg0) {
	}
}
