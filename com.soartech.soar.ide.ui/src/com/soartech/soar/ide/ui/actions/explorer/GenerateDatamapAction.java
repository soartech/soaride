package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.sql.TraversalUtil;
import com.soartech.soar.ide.core.sql.Triple;

/**
 * Represents a path from the root node &lt;s&gt; that cannot extend any farther.
 * Either the last node has no child nodes, or the path loops into itself, or it
 * loops into another terminal path, or something like that.
 * @author miller
 *
 */
class TerminalPath {
	public ArrayList<Triple> path;
	public ArrayList<Triple> links = new ArrayList<Triple>();
	public TerminalPath(ArrayList<Triple> path, ArrayList<Triple> links) {
		this.path = path;
		this.links = links;
	}
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("" + path);
		if (links != null) {
			buff.append(", loops to: " + links);
		}
		return buff.toString();
	}
}

/**
 * Represents a single correction to be made to the existing datamap.
 * The user can choose which corrections to apply and which to ignore.
 * @author miller
 *
 */
class Correction {
	SoarDatabaseRow row;
	ArrayList<Triple> addition;
	ArrayList<Triple> links = new ArrayList<Triple>();
	
	// Assigned during apply()
	SoarDatabaseRow tail = null;

	/**
	 * Class constructor.
	 * @param row
	 * @param addition
	 * @param links
	 */
	public Correction(SoarDatabaseRow row, ArrayList<Triple> addition, ArrayList<Triple> links) {
		this.row = row;
		this.addition = addition;
		this.links = links;
	}

	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append(row.getPathName());
		for (Triple triple : addition) {
			buff.append("." + triple.attribute);
		}
		Triple last = addition.get(addition.size() - 1); 
		if (last.valueIsConstant()) {
			buff.append(" " + last.value);
		}
		if (links != null && links.size() > 0) {
			buff.append(", link with:");
			for (Triple triple : links) {
				buff.append(" " + triple);
			}
		}
		return buff.toString();
	}

	/**
	 * Applys this correction to its datamap.
	 */
	public void apply() {
		SoarDatabaseRow currentRow = row;
		for (int i = 0; i < addition.size(); ++i) {
			Triple triple = addition.get(i);
			if (triple.valueIsVariable()) {
				currentRow = createJoinedChildIfNotExists(currentRow, Table.DATAMAP_IDENTIFIERS, triple.attribute);
			} else if (triple.valueIsInteger()) {
				currentRow = createJoinedChildIfNotExists(currentRow, Table.DATAMAP_INTEGERS, triple.attribute);
				editMinMaxValues(currentRow, triple);
			} else if (triple.valueIsFloat()) {
				currentRow = createJoinedChildIfNotExists(currentRow, Table.DATAMAP_FLOATS, triple.attribute);
				editMinMaxValues(currentRow, triple);
			} else if (triple.valueIsString()) {
				if (triple.value.equals(Triple.STRING_VALUE)) {
					currentRow = createJoinedChildIfNotExists(currentRow, Table.DATAMAP_STRINGS, triple.attribute);
				} else {
					ArrayList<ISoarDatabaseTreeItem> enumerations = currentRow.getDirectedJoinedChildrenOfType(Table.DATAMAP_ENUMERATIONS, false, false);
					SoarDatabaseRow enumeration = null;
					for (ISoarDatabaseTreeItem enumItem : enumerations) {
						SoarDatabaseRow enumRow = (SoarDatabaseRow) enumItem;
						if (enumRow.getName() == triple.value) {
							enumeration = enumRow;
							break;
						}
					}

					if (enumeration == null) {
						enumeration = createJoinedChildIfNotExists(currentRow, Table.DATAMAP_ENUMERATIONS, triple.attribute);
					}
					ArrayList<SoarDatabaseRow> enumValues = enumeration.getChildrenOfType(Table.DATAMAP_ENUMERATION_VALUES);
					boolean hasValue = false;
					for (SoarDatabaseRow valueRow : enumValues) {
						if (valueRow.getName() == triple.value) {
							hasValue = true;
							break;
						}
					}
					if (!hasValue) {
						enumeration.createChild(Table.DATAMAP_ENUMERATION_VALUES, triple.value);
					}
					currentRow = enumeration;
				}
			}
			if (triple.comment != null) {
				currentRow.setComment(triple.comment);
			}
		}
		tail = currentRow;
	}
	
	/**
	 * Edits min_value and max_value to include the value of the triple.
	 * @param row
	 * @param triple
	 */
	private static void editMinMaxValues(SoarDatabaseRow row, Triple triple) {
		assert row.getTable() == Table.DATAMAP_FLOATS || row.getTable() == Table.DATAMAP_INTEGERS;
		Object minVal = row.getColumnValue("min_value");
		Object maxVal = row.getColumnValue("max_value");
		if (triple.valueIsFloat()) {
			Double minValue = (Double) minVal;
			Double maxValue = (Double) maxVal;
			double value = Double.parseDouble(triple.value);
			if (minValue == null) {
				minValue = value;
			}
			if (maxValue == null) {
				maxValue = value;
			}
			if (value < minValue) {
				minValue = value;
			}
			if (value > maxValue) {
				maxValue = value;
			}
			if (!minValue.equals(minVal)) {
				row.updateValue("min_value", "" + minValue);
			}
			if (!maxValue.equals(maxVal)) {
				row.updateValue("max_value", "" + maxValue);
			}
		}
		else if (triple.valueIsInteger()) {
			Integer minValue = (Integer) minVal;
			Integer maxValue = (Integer) maxVal;
			int value = Integer.parseInt(triple.value);
			if (minValue == null) {
				minValue = value;
			}
			if (maxValue == null) {
				maxValue = value;
			}
			if (value < minValue) {
				minValue = value;
			}
			if (value > maxValue) {
				maxValue = value;
			}
			if (!minValue.equals(minVal)) {
				row.updateValue("min_value", "" + minValue);
			}
			if (!maxValue.equals(maxVal)) {
				row.updateValue("max_value", "" + maxValue);
			}
		}
	}
	
	/**
	 * Once all corrections have been applied, this is called to link items in the corrections to each other where needed.
	 */
	public void applyLinks() {
		for (Triple link : links) {
			ArrayList<SoarDatabaseRow> rows = link.getDatamapRowsFromProblemSpace(row.getAncestorRow(Table.PROBLEM_SPACES));
			for (SoarDatabaseRow row : rows) {
				SoarDatabaseRow.joinRows(row, tail, row.getDatabaseConnection());
			}
		}
	}

	/**
	 * Looks for a child of the given row, of the given type. If none exists, creates a new row and returns that.
	 * @param currentRow
	 * @param table
	 * @param named
	 * @return
	 */
	private SoarDatabaseRow createJoinedChildIfNotExists(SoarDatabaseRow currentRow, Table table, String named) {
		ArrayList<ISoarDatabaseTreeItem> childItems = currentRow.getDirectedJoinedChildrenOfType(table, false, false);
		for (ISoarDatabaseTreeItem childItem : childItems) {
			SoarDatabaseRow childRow = (SoarDatabaseRow) childItem;
			if (childRow.getName().equals(named)) {
				return childRow;
			}
		}
		return currentRow.createJoinedChild(table, named);
	}
}

/**
 * Generates a datamap for the given problem space by examining the parsed rules that
 * belong to the problem space. Optionally prompts the user for confirmation before making
 * changes to the existing datamap.
 * @author miller
 *
 */
public class GenerateDatamapAction extends Action {
	
	SoarDatabaseRow problemSpace;
	//Shell shell;
	public boolean applyAll = false;
	ArrayList<SoarDatabaseRow> joinedRules;
	
	public GenerateDatamapAction(SoarDatabaseRow problemSpace, boolean applyAll) {
		super ("Generate Datamap");
		this.problemSpace = problemSpace;
		this.applyAll = applyAll;
		joinedRules = problemSpace.getJoinedRowsFromTable(Table.RULES);
		ArrayList<SoarDatabaseRow> joinedOperators = problemSpace.getJoinedRowsFromTable(Table.OPERATORS);
		for (SoarDatabaseRow operator : joinedOperators) {
			joinedRules.addAll(operator.getJoinedRowsFromTable(Table.RULES));
		}
	}
	
	public int getJoinedRulesSize() {
		return joinedRules.size();
	}
	
	public SoarDatabaseRow getProblemSpace() {
		return problemSpace;
	}
	
	@Override
	public void run() {
		this.run((IProgressMonitor)null);
	}
	
	public void run(IProgressMonitor monitor) {

		if (problemSpace == null) {
			return;
		}
		
		ArrayList<Triple> allTriples = new ArrayList<Triple>();
		
		//long getTriplesStart = new Date().getTime();
		TraversalUtil.resetLoggingTimes();
		for (int i = 0; i < joinedRules.size(); ++i) {
			ISoarDatabaseTreeItem item = joinedRules.get(i);
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow row = (SoarDatabaseRow) item;
			assert row.getTable() == Table.RULES;
			ArrayList<Triple> triples = TraversalUtil.getTriplesForRule(row);
			allTriples.addAll(triples);
			if (monitor != null) {
				monitor.worked(1);
			}
		}
		//long getTriplesEnd = new Date().getTime();
		//System.out.println("Got Triples: " + (getTriplesEnd - getTriplesStart));
		//TraversalUtil.printLoggingTimes();
		
		runWithProblemSpaceForTriples(problemSpace, allTriples, applyAll);
	}
	
	public static void runWithProblemSpaceForTriples(SoarDatabaseRow problemSpace, ArrayList<Triple> triples, boolean applyAll) {
		
		// Get all terminal paths.
		//long terminalPathsStart = new Date().getTime();
		ArrayList<TerminalPath> paths = terminalPathsForTriples(triples);
		//long terminalPathsEnd = new Date().getTime();
		//System.out.println("Get terminal paths: " + (terminalPathsEnd - terminalPathsStart));
		
		// TODO debug
		/*
		System.out.println("Got paths:");
		for (TerminalPath path : paths) {
			System.out.println(path);
		}
		*/
		
		// Compare each path with existing datamap.
		// Propose corrections where paths diverge with datamap.
		ArrayList<Correction> corrections = new ArrayList<Correction>();
		
		// The root node <s>
		SoarDatabaseRow root = (SoarDatabaseRow) problemSpace.getChildrenOfType(Table.DATAMAP_IDENTIFIERS).get(0);
		
		// Iterate over all paths to build corrections
		//long buildCorrectionsStart = new Date().getTime();
		for (TerminalPath terminalPath : paths) {
			ArrayList<Triple> path = terminalPath.path;
			ArrayList<SoarDatabaseRow> currentNodes = new ArrayList<SoarDatabaseRow>();
			currentNodes.add(root);
			
			// Walk down the path, keeping track of which datamap nodes
			// correspond to the current location on the path.
			for (int i = 0; i < path.size(); ++i) {
				ArrayList<SoarDatabaseRow> childNodes = new ArrayList<SoarDatabaseRow>();
				for (SoarDatabaseRow node : currentNodes) {
					Triple triple = path.get(i);
					ArrayList<ISoarDatabaseTreeItem> items = new ArrayList<ISoarDatabaseTreeItem>();
					if (triple.valueIsVariable()) {
						items.addAll(node.getDirectedJoinedChildrenOfType(Table.DATAMAP_IDENTIFIERS, false, false));
					} else if (triple.valueIsInteger()) {
						items.addAll(node.getDirectedJoinedChildrenOfType(Table.DATAMAP_INTEGERS, false, false));
					} else if (triple.valueIsFloat()) {
						items.addAll(node.getDirectedJoinedChildrenOfType(Table.DATAMAP_FLOATS, false, false));
					} else if (triple.valueIsString()) {
						// Only add enums if they have a value that's correct
						ArrayList<ISoarDatabaseTreeItem> enumItems = node.getDirectedJoinedChildrenOfType(Table.DATAMAP_ENUMERATIONS, false, false);
						for (ISoarDatabaseTreeItem enumItem : enumItems) {
							SoarDatabaseRow enumRow = (SoarDatabaseRow) enumItem;
							ArrayList<SoarDatabaseRow> enumValues = enumRow.getChildrenOfType(Table.DATAMAP_ENUMERATION_VALUES);
							for (SoarDatabaseRow enumValue : enumValues) {
								if (enumValue.getName().equals(triple.value)) {
									items.add(enumItem);
								}
							}
						}
					}
					items.addAll(node.getDirectedJoinedChildrenOfType(Table.DATAMAP_STRINGS, false, false));
					for (ISoarDatabaseTreeItem item : items) {
						assert item instanceof SoarDatabaseRow;
						SoarDatabaseRow childRow = (SoarDatabaseRow) item;
						if (childRow.getName().equals(triple.attribute)) {
							childNodes.add(childRow);
						}
					}
				}
				
				// if currentNodes.size == 0 and there's more triples,
				// propose adding triples and continue to next path
				if (childNodes.size() == 0) {
					
					// TODO debug
					// System.out.println("No more child nodes.");
					
					for (SoarDatabaseRow leafNode : currentNodes) {
						
						// TODO debug
						// System.out.println("Leaf: " + leafNode);
						
						ArrayList<Triple> addition = new ArrayList<Triple>();
						for (int j = i; j < path.size(); ++j) {
							addition.add(path.get(j));
						}
						if (addition.size() > 0) {
							Correction correction = new Correction(leafNode, addition, terminalPath.links);
							corrections.add(correction);
						}
					}
					break;
				}
				currentNodes = childNodes;
			}
		}
		//long buildCorrectionsEnd = new Date().getTime();
		//System.out.println("Built Corrections: " + (buildCorrectionsEnd - buildCorrectionsStart));
		
		// TODO debug
		/*
		System.out.println("Proposing corrections:");
		for (Correction c : corrections) {
			System.out.println(c);
		}
		*/
		
		Object[] result = corrections.toArray();
		if (!applyAll) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			ListSelectionDialog dialog = new ListSelectionDialog(shell, result, new ArrayContentProvider(), new LabelProvider(), "Select which corrections to apply to the existing datamap.");
			dialog.setTitle("Select corrections to apply.");
			dialog.setInitialSelections(corrections.toArray());
			dialog.open();
			result = dialog.getResult();
		}
		
		// TODO debug
		/*
		System.out.println("User selected correction:");
		for (Object obj : result) {
			System.out.println("" + obj);
		}
		*/
		
		// Apply corrections
		//long applyCorrectionsStart = new Date().getTime();
		for (Object obj : result) {
			Correction correction = (Correction) obj;
			correction.apply();
		}
		
		for (Object obj : result) {
			Correction correction = (Correction) obj;
			correction.applyLinks();
		}
		//long applyCorrectionsEnd = new Date().getTime();
		//System.out.println("Applied Corrections: " + (applyCorrectionsEnd - applyCorrectionsStart));
		
		// For each superstate attribute, and superstate.superstate attribute, etc.,
		// link that attribute with ancestor problem spaces' <s> node.
		ArrayList<SoarDatabaseRow> superstates = problemSpace.getDirectedJoinedParentsOfType(Table.PROBLEM_SPACES);
		for (SoarDatabaseRow operator : problemSpace.getDirectedJoinedParentsOfType(Table.OPERATORS)) {
			superstates.addAll(operator.getDirectedJoinedParentsOfType(Table.PROBLEM_SPACES));
		}
		while (superstates.size() > 0) {
			ArrayList<ISoarDatabaseTreeItem> superstateAtributes = root.getDirectedJoinedChildrenOfType(Table.DATAMAP_IDENTIFIERS, false, false);
			ArrayList<ISoarDatabaseTreeItem> nextSuperstateAttributes = new ArrayList<ISoarDatabaseTreeItem>();
			for (ISoarDatabaseTreeItem item : superstateAtributes) {
				SoarDatabaseRow attribute = (SoarDatabaseRow) item;
				if (attribute.getName().equals("superstate")) {
					for (SoarDatabaseRow superstate : superstates) {
						SoarDatabaseRow superstateRoot = (SoarDatabaseRow) superstate.getChildrenOfType(Table.DATAMAP_IDENTIFIERS).get(0);
						LinkDatamapRowsAction linkAction = new LinkDatamapRowsAction(attribute, superstateRoot);
						linkAction.run();
					}
					nextSuperstateAttributes.addAll(attribute.getDirectedJoinedChildrenOfType(Table.DATAMAP_IDENTIFIERS, false, false));
				}
			}
			ArrayList<SoarDatabaseRow> newSuperstates = new ArrayList<SoarDatabaseRow>();
			for (SoarDatabaseRow superstate : superstates) {
				newSuperstates.addAll(superstate.getDirectedJoinedParentsOfType(Table.PROBLEM_SPACES));
			}
			superstates = newSuperstates;
			superstateAtributes = nextSuperstateAttributes;
		}
	}
	
	private static ArrayList<TerminalPath> terminalPathsForTriples(ArrayList<Triple> triples) {
		ArrayList<TerminalPath> ret = new ArrayList<TerminalPath>();
		HashSet<String> usedVariables = new HashSet<String>();
		
		boolean grew = true;
		while (grew) {
			grew = false;
			ArrayList<Triple> nextTriples = new ArrayList<Triple>();
			
			// Iterate over all triples.
			for (Triple triple : triples) {
				
				// Keep track of whether any path from this triple was added to 'ret'.
				boolean added = false;
				
				// If the paths from this triple are naturally terminal --
				// if the triple has no child triples.
				boolean terminal = !triple.valueIsVariable() || triple.childTriples == null;
				
				// Iterate over each path from this triple.
				for (ArrayList<Triple> path : triple.getTriplePathsFromState()) {
					
					// Figure out if the path loops onto itself
					// e.g. (<s> ^attr <v1>),(<v1> ^attr <v2>),(<v2> ^attr <v3>),(<v3> ^attr <v1>)
					// In the example, this triple is the last one and its value loops back to the
					// previous reference to the variable <v1>.
					HashSet<String> pathVariables = new HashSet<String>();
					addVariablesToHashSet(path, pathVariables);
					boolean loops = triple.valueIsVariable() && pathVariables.contains(triple.value);
					
					// Also figure out if this path loops onto any path in 'ret'.
					// 'Loops' in this context is used lightly -- there may not be any
					// cyclical paths.
					ArrayList<Triple> links = new ArrayList<Triple>();
					boolean loopsIntoPath = false;
					for (TerminalPath retPath : ret) {
						Triple t = pathLoopsIntoPath(path, retPath.path);
						if (t != null) {
							links.add(t);
							loopsIntoPath = true;
							// break;
						}
					}
					
					if (terminal || loops || loopsIntoPath) {

						// Make sure the path isn't too long
						boolean tooLong = false;
						for (TerminalPath retPath : ret) {
							if (pathCollidesWithPath(path, retPath.path)) {
								tooLong = true;
								break;
							}
						}
						
						// Make sure the path isn't identical to a path that's already been proposed.
						boolean identical = false;
						for (TerminalPath retPath : ret) {
							if (path.equals(retPath.path) || pathsAreRedundant(path, retPath.path)) {
								identical = true;
								break;
							}
						}
						
						if (!tooLong && !identical) {
							grew = true;
							added = true;
							TerminalPath newPath = new TerminalPath(path, links);
							ret.add(newPath);
							addVariablesToHashSet(path, usedVariables);
							
							// TODO debug
							/*
							System.out.println("Added path: " + path);
							System.out.println("terminal: " + terminal);
							System.out.println("loops: " + loops);
							System.out.println("loopsIntoPath: " + loopsIntoPath + '\n');
							*/
						}
					}
				}
				if (!added) nextTriples.add(triple);
			}
			triples = nextTriples;
		}
		
		return ret;
	}

	private static boolean pathsAreRedundant(ArrayList<Triple> path, ArrayList<Triple> retPath) {
		int len = Math.min(path.size(), retPath.size());
		for (int i = 0; i < len; ++i) {
			if (!path.get(i).equals(retPath.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static void addVariablesToHashSet(ArrayList<Triple> triples, HashSet<String> variables) {
		for (Triple triple : triples) {
			variables.add(triple.variable);
		}
	}
	
	/**
	 * 
	 * @param path
	 * @param retPath
	 * @return True if the two paths diverge and than converge again.
	 */
	private static boolean pathCollidesWithPath(ArrayList<Triple> path, ArrayList<Triple> retPath) {
		int index = 0;
		for ( ; index < path.size() - 1; ++index) {
			if (index >= retPath.size()) {
				return false;
			}
			if (!path.get(index).equals(retPath.get(index))) {
				break;
			}
		}
		if (index == path.size() - 1) {
			return false;
		}
		
		HashSet<String> retPathVariables = new HashSet<String>();
		for (Triple t : retPath) {
			retPathVariables.add(t.variable);
		}
		
		for ( ; index < path.size() - 1; ++index) {
			/*
			if (index >= retPath.size()) {
				return false;
			}
			*/
			if (retPathVariables.contains(path.get(index).value)) {
				return true;
			}
		}
		
		return false;
	}
	
	private static Triple pathLoopsIntoPath(ArrayList<Triple> path, ArrayList<Triple> retPath) {
		Triple last = path.get(path.size() - 1);
		if (!last.valueIsVariable()) return null;
		for (int i = 0; i < retPath.size(); ++i) {
			Triple retTriple = retPath.get(i);			
			if (last.value.equals(retTriple.value)) {
				if (!retTriple.equals(last)) {
					return retTriple;
				}
			}
			
		}
		return null;
	}
}
