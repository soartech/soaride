package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal.TraversalUtil;
import com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal.Triple;
import com.soartech.soar.ide.ui.editors.database.SoarDatabaseDatamapEditor;

public class GenerateDatamapAction extends Action {


	SoarDatabaseRow problemSpace;
	Shell shell;
	public boolean applyAll = false;;
	
	public GenerateDatamapAction(SoarDatabaseRow problemSpace) {
		super ("Generate datamap");
		this.problemSpace = problemSpace;
	}

	@Override
	public void run() {
		// Generate the datamap.
		shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		if (problemSpace != null) {
			ArrayList<ISoarDatabaseTreeItem> joinedRules = problemSpace.getJoinedRowsFromTable(Table.RULES);
			ArrayList<ISoarDatabaseTreeItem> joinedOperators = problemSpace.getJoinedRowsFromTable(Table.OPERATORS);
			for (ISoarDatabaseTreeItem item : joinedOperators) {
				assert item instanceof SoarDatabaseRow;
				SoarDatabaseRow operator = (SoarDatabaseRow) item;
				joinedRules.addAll(operator.getJoinedRowsFromTable(Table.RULES));
			}
			
			for (ISoarDatabaseTreeItem item : joinedRules) {
				if (item instanceof SoarDatabaseRow) {
					assert ((SoarDatabaseRow) item).getTable() == Table.RULES;

					SoarDatabaseRow row = (SoarDatabaseRow) item;
					ArrayList<Triple> triples = TraversalUtil.getTriplesForRule(row);

					// Recursively visit existing datamap,
					// proposing corrections where conflicts arise.
					ArrayList<ISoarDatabaseTreeItem> childIdentifiers = problemSpace.getChildrenOfType(Table.DATAMAP_IDENTIFIERS);
					for (ISoarDatabaseTreeItem childIdentifier : childIdentifiers) {
						SoarDatabaseRow child = (SoarDatabaseRow) childIdentifier;
						HashSet<String> variableSet = new HashSet<String>();
						for (Triple t : triples) {
							if (t.hasState) {
								variableSet.add(t.variable);
							}
						}
						HashSet<SoarDatabaseRow> visitedNodes = new HashSet<SoarDatabaseRow>();
						HashSet<Triple> usedTriples = new HashSet<Triple>();
						visitDatamapNode(child, variableSet, triples, visitedNodes, usedTriples);
					}

					// Finally, find attributes that should be linked.
					// Find attributes whose values are variables with the same
					// name.
					// propose to link those attributes.
					proposeLinksWithTriples(triples);
				}
			}
			
			// Build structure from substates
			// In the future, make this recursive (or something)
			// to include arbitraarily deep substates
			ArrayList<ISoarDatabaseTreeItem> substates = problemSpace.getDirectedJoinedChildrenOfType(Table.PROBLEM_SPACES, false);
			for (ISoarDatabaseTreeItem item : substates) {
				assert item instanceof SoarDatabaseRow;
				SoarDatabaseRow susbtate = (SoarDatabaseRow) item;
				ArrayList<ISoarDatabaseTreeItem> substateRules = susbtate.getJoinedRowsFromTable(Table.RULES);
				ArrayList<ISoarDatabaseTreeItem> susbtateOperators = susbtate.getJoinedRowsFromTable(Table.OPERATORS);
				for (ISoarDatabaseTreeItem opItem : susbtateOperators) {
					assert opItem instanceof SoarDatabaseRow;
					SoarDatabaseRow operator = (SoarDatabaseRow) opItem;
					substateRules.addAll(operator.getJoinedRowsFromTable(Table.RULES));
				}
				
				for (ISoarDatabaseTreeItem ruleItem : substateRules) {
					if (ruleItem instanceof SoarDatabaseRow) {
						assert ((SoarDatabaseRow) ruleItem).getTable() == Table.RULES;
						SoarDatabaseRow substateRule = (SoarDatabaseRow) ruleItem;
						ArrayList<Triple> substateTriples = TraversalUtil.getTriplesForRule(substateRule);
						ArrayList<Triple> triples = TraversalUtil.getTriplesForSuperstate(substateTriples);
						
						// Recursively visit existing datamap,
						// proposing corrections where conflicts arise.
						ArrayList<ISoarDatabaseTreeItem> childIdentifiers = problemSpace.getChildrenOfType(Table.DATAMAP_IDENTIFIERS);
						for (ISoarDatabaseTreeItem childIdentifier : childIdentifiers) {
							SoarDatabaseRow child = (SoarDatabaseRow) childIdentifier;
							HashSet<String> variableSet = new HashSet<String>();
							for (Triple t : triples) {
								if (t.hasState) {
									variableSet.add(t.variable);
								}
							}
							HashSet<SoarDatabaseRow> visitedNodes = new HashSet<SoarDatabaseRow>();
							HashSet<Triple> usedTriples = new HashSet<Triple>();
							visitDatamapNode(child, variableSet, triples, visitedNodes, usedTriples);
						}

						// Finally, find attributes that should be linked.
						// Find attributes whose values are variables with the same
						// name.
						// propose to link those attributes.
						proposeLinksWithTriples(triples);
					}
				}
				
			}

			// reload datamaps
			IEditorReference[] references = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
			for (IEditorReference reference : references) {
				IEditorPart part = reference.getEditor(false);
				if (part != null && part instanceof SoarDatabaseDatamapEditor) {
					SoarDatabaseDatamapEditor editor = (SoarDatabaseDatamapEditor) part;
					editor.refreshTree();
				}
			}
		}
	}
	
	private void proposeLinksWithTriples(ArrayList<Triple> triples) {
		for (Triple firstTriple : triples) {
			if (firstTriple.valueIsVariable()) {
				for (Triple secondTriple : triples) {
					if (firstTriple != secondTriple) {
						// Here are two triples.
						// See if they have attributes with the same
						// name.
						if (firstTriple.value.equals(secondTriple.value)) {
							// Check each node from the first
							// against each node from the second.
							for (SoarDatabaseRow firstRow : firstTriple.nodes) {
								for (SoarDatabaseRow secondRow : secondTriple.nodes) {
									// If the two rows aren't
									// joined, propose joining them.
									assert firstRow.getTable() == Table.DATAMAP_IDENTIFIERS;
									assert secondRow.getTable() == Table.DATAMAP_IDENTIFIERS;
									if (!SoarDatabaseRow.rowsAreJoined(firstRow, secondRow, firstRow.getDatabaseConnection())) {
										// Propose joining rows.
										// Only propse these joins once
										String firstName = firstRow.getPathName();
										String secondName = secondRow.getPathName();
										if (firstName.compareTo(secondName) < 0) {
											boolean link = applyAll;
											if (!link) {
												String title = "Link Datamap Attributes?";
												String message = "Datamap atrributes appear to point to the same variable.\nLink these attributes?\n\n" + firstName + "\n" + secondName;
												String[] options = { "OK", "Apply All", "Cancel" };
												Image image = MessageDialog.getDefaultImage();
												MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, options, 0);
												int result = dialog.open();
												if (result == 1) applyAll = true;
												if (result == 0 || applyAll) link = true;
											}
											if (link) {
												SoarDatabaseRow.joinRows(firstRow, secondRow, firstRow.getDatabaseConnection());
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void visitDatamapNode(SoarDatabaseRow node, HashSet<String> variableSet, ArrayList<Triple> triples, HashSet<SoarDatabaseRow> visitedNodes, HashSet<Triple> usedTriples) {
		
		// This method is recursive, but each node should only be visited once.
		if (visitedNodes.contains(node)) {
			return;
		}

		for (Triple triple : triples) {
			//System.out.println(triple.toString());
			if (variableSet.contains(triple.variable)) {
				boolean matched = false;
				ArrayList<ISoarDatabaseTreeItem> nodeAttributes = node.getDirectedJoinedChildren(false);
				for (ISoarDatabaseTreeItem item : nodeAttributes) {
					SoarDatabaseRow attribute = (SoarDatabaseRow) item;
					String attributeName = attribute.getName();
					if (attributeName.equals(triple.attribute) && typesMatch(attribute, triple)) {
						matched = true;
						triple.nodes.add(attribute);

						// System.out.println("Matched: " + triple + ", " + attribute);

						// TODO
						// Check that the value of the triple is within the
						// bounds specified by the datamap.
						// If it's not, propose a correction to the user.
					}
				}
				if (!matched) {

					//System.out.println("No match: " + triple);

					if (!usedTriples.contains(triple)) {
						// Propose a new datamap attribute to match the given
						// triple.
						boolean apply = applyAll;
						if (!apply) {
							String title = "Generate Datamap Node?";
							String message = "No datamap node for triple in rule \"" + triple.rule + "\":\n" + triple + "\n\nGenerate node?";
							String[] options = { "OK", "Apply All", "Cancel" };
							Image image = MessageDialog.getDefaultImage();
							MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, options, 0);
							int result = dialog.open();
							if (result == 1) applyAll = true;
							if (result == 0 || applyAll) apply = true;
						}
						if (apply) {
							SoarDatabaseRow newNode = generateDatamapNodeForTriple(node, triple);
							// Remember that this triple was used
							usedTriples = new HashSet<Triple>(usedTriples);
							usedTriples.add(triple);
							triple.nodes.add(newNode);
						}
					}
					/* 
					else {
						System.out.println("Triple already used. Avoiding recursive node generation.");
					}
					*/
				}
			}
		}

		// Attributes may have changed -- query database again.
		ArrayList<ISoarDatabaseTreeItem> nodeAttributes = node.getDirectedJoinedChildren(false);
		for (ISoarDatabaseTreeItem item : nodeAttributes) {
			SoarDatabaseRow attribute = (SoarDatabaseRow) item;
			if (attribute.getTable() == Table.DATAMAP_IDENTIFIERS) {
				HashSet<String> nextVariableSet = new HashSet<String>();
				for (Triple triple : triples) {
					if (variableSet.contains(triple.variable) && triple.attribute.equals(attribute.getName()) && triple.valueIsVariable()) {
						nextVariableSet.add(triple.value);
					}
				}
				visitedNodes = new HashSet<SoarDatabaseRow>(visitedNodes);
				visitedNodes.add(node);
				visitDatamapNode(attribute, nextVariableSet, triples, visitedNodes, usedTriples);
			}
		}
	}

	private SoarDatabaseRow generateDatamapNodeForTriple(SoarDatabaseRow node, Triple triple) {
		Table childTable = null;
		if (triple.valueIsVariable()) {
			childTable = Table.DATAMAP_IDENTIFIERS;
		} else if (triple.valueIsString()) {
			childTable = Table.DATAMAP_STRINGS;
		}
		String name = triple.attribute;
		SoarDatabaseRow ret = node.createJoinedChild(childTable, name);
		return ret;
	}

	/*
	private boolean variableSetContainsMatchingVariable(HashSet<String> variables, String variable) {
		if (variables.contains(variable)) {
			return true;
		}
		Variable newVariable = new Variable(variable.name, null);
		boolean ret = variables.contains(newVariable);
		return ret;
	}
	*/

	private boolean typesMatch(SoarDatabaseRow attribute, Triple triple) {
		Table table = attribute.getTable();
		return (table == Table.DATAMAP_STRINGS) || (table == Table.DATAMAP_ENUMERATIONS) || (table == Table.DATAMAP_IDENTIFIERS && triple.valueIsVariable())
				|| (table == Table.DATAMAP_INTEGERS && triple.valueIsInteger()) || (table == Table.DATAMAP_FLOATS && triple.valueIsFloat());
	}
}
