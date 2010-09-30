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
import com.soartech.soar.ide.core.sql.datamap.Correction;
import com.soartech.soar.ide.core.sql.datamap.DatamapUtil;
import com.soartech.soar.ide.core.sql.datamap.TerminalPath;



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
		
		runWithProblemSpaceForTriples(problemSpace, allTriples, applyAll);
	}
	
	public static void runWithProblemSpaceForTriples(SoarDatabaseRow problemSpace, ArrayList<Triple> triples, boolean applyAll) {

		// The root node <s>
		SoarDatabaseRow root = (SoarDatabaseRow) problemSpace.getChildrenOfType(Table.DATAMAP_IDENTIFIERS).get(0);
		
		// Compare each path with existing datamap.
		// Propose corrections where paths diverge with datamap.
		ArrayList<Correction> corrections = DatamapUtil.getCorrections(problemSpace, triples, root);
		
		Object[] result = corrections.toArray();
		if (!applyAll) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			ListSelectionDialog dialog = new ListSelectionDialog(shell, result, new ArrayContentProvider(), new LabelProvider(), "Select which corrections to apply to the existing datamap.");
			dialog.setTitle("Select corrections to apply.");
			dialog.setInitialSelections(corrections.toArray());
			dialog.open();
			result = dialog.getResult();
		}
		// Apply corrections
		for (Object obj : result) {
			Correction correction = (Correction) obj;
			correction.apply();
		}
		
		for (Object obj : result) {
			Correction correction = (Correction) obj;
			correction.applyLinks();
		}
		
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
	
	

	
}
