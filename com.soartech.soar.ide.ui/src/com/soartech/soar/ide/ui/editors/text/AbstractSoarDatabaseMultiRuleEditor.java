package com.soartech.soar.ide.ui.editors.text;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.SoarProblem;
import com.soartech.soar.ide.core.sql.ISoarDatabaseEventListener;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public abstract class AbstractSoarDatabaseMultiRuleEditor extends AbstractSoarDatabaseTextEditor implements ISoarDatabaseTextEditor, ISoarDatabaseEventListener {
	
	/**
	 * Used to associate a set of text with its beginning offset.
	 */
	class StringWithOffset {
		String string;
		int offset;
		public StringWithOffset(String string, int offset) {
			this.string = string;
			this.offset = offset;
		}
	}
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
		if (input != null) {
			final HashMap<String, SoarDatabaseRow> rulesByName = new HashMap<String, SoarDatabaseRow>();
			final HashSet<SoarDatabaseRow> childRules = new HashSet<SoarDatabaseRow>();
			final SoarDatabaseRow row = input.getRow();
			ArrayList<SoarDatabaseRow> rules = row.getJoinedRowsFromTable(Table.RULES);
			for (SoarDatabaseRow rule : rules) {
				String ruleName = rule.getName();
				rulesByName.put(ruleName, rule);
			}
			
			IDocument doc = getDocumentProvider().getDocument(input);
			final ArrayList<StringWithOffset> rulesText = getRulesFromText(doc.get());
			row.getDatabaseConnection().pushSuppressEvents();
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			input.clearProblems();
			
			// Save the rules, parsing them and collecting errors as you go.
			try {
				new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask("Saving Rules for \"" + getInput().getName() + "\"", rulesText.size());
						for (StringWithOffset ruleText : rulesText) {
							String ruleName = getNameOfRules(ruleText.string);
							SoarDatabaseRow rule = rulesByName.get(ruleName);
							if (rule == null) {
								rule = row.getTopLevelRow().createChild(Table.RULES, ruleName);
								addRow(rule);
							}
							ArrayList<SoarProblem> problems = new ArrayList<SoarProblem>();
							rule.save(ruleText.string, problems, null);
							for (SoarProblem problem : problems) { 
							  problem.start += ruleText.offset;
							  input.addProblem(problem);
							}
							childRules.add(rule);
							monitor.worked(1);
						}
						monitor.done();
					}
				});
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// For rules that used to be in this multi-rule item but aren't anymore,
			// ask the user if they should be removed or deleted.
			for (SoarDatabaseRow rule : row.getJoinedRowsFromTable(Table.RULES)) {
				if (!childRules.contains(rule)) {
					// Unjoin and also directed unjoin, just to cover bases
					// with regard to tags, operators, etc
					SoarDatabaseRow.unjoinRows(row, rule, row.getDatabaseConnection());
					SoarDatabaseRow.directedUnjoinRows(row, rule, row.getDatabaseConnection());
					String ruleName = rule.getName();
					String rowName = row.getName();
					MessageDialog dialog = new MessageDialog(shell,
							"Delete rule \"" + ruleName + "\"?",
							null,
							"The rule \"" + ruleName + "\" no longer appears in \"" + rowName + "\" (it may have been renamed). Delete \"" + ruleName + "\" or just remove it from \"" + rowName + "\"?",
							MessageDialog.QUESTION,
							new String[] { "Remove", "Delete" },
							0);
					int result = dialog.open();
					if (result == 1) { // Delete
						rule.deleteAllChildren(true, null);
					}
				}
			}
			
			row.getDatabaseConnection().popSuppressEvents();
			row.getDatabaseConnection().fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
			
			// Add the found problems to the row.
			clearAnnotations();
			//SoarDatabaseRow row = input.getSoarDatabaseStorage().getRow();
			//row.save(doc, input);
			ArrayList<SoarProblem> problems = input.getProblems();
			for (SoarProblem problem : problems) {
				SoarDatabaseTextAnnotation annotation = new SoarDatabaseTextAnnotation();
				Position position = new Position(problem.start, problem.length);
				addAnnotation(annotation, position);
			}
			getVerticalRuler().update();
			/*
			if (!hasRule) {
				this.doRevertToSaved();
			}
			*/
		}
	}
	
	protected abstract void addRow(SoarDatabaseRow newRow);
	
	private ArrayList<StringWithOffset> getRulesFromText(String doc) {
		ArrayList<StringWithOffset> ret = new ArrayList<StringWithOffset>();
		int braceDepth = 0;
		StringBuffer buff = new StringBuffer();
		boolean string = false;
		boolean comment = false;
		int start = 0;
		for (int i = 0; i < doc.length(); ++i) {
			char c = doc.charAt(i);
			buff.append(c);
			if (comment && c == '\n') comment = false;
			if (!string && c == '#') comment = true;
			if (!comment && c == '|') string = !string;
			if (comment) continue;
			if (c == '{') ++braceDepth;
			if (c == '}') --braceDepth;
			if (c < 0) c = 0;
			if (c == '}' && braceDepth == 0) {
				ret.add(new StringWithOffset(buff.toString().trim(), start));
				buff = new StringBuffer();
				start = i + 1;
			}
		}
		ret.add(new StringWithOffset(buff.toString().trim(), start));
		return ret;
	}
	
	private String getBodyOfRule(String rule) {
		int start = rule.indexOf('{');
		int end = rule.lastIndexOf('}');
		if (start == -1 || end == -1 || end < start) return "";
		return rule.substring(start, end + 1);
	}
	
	private String getNameOfRules(String rule) {
		int nameBeginIndex = rule.indexOf('{') + 1;
		int nameEndIndex1 = rule.indexOf('(', nameBeginIndex);
		int nameEndIndex2 = rule.indexOf("-->", nameBeginIndex);
		int nameEndIndex3 = rule.lastIndexOf('}');
		int nameEndIndex4 = rule.indexOf('\n', nameBeginIndex);
		if (nameEndIndex1 == -1)
			nameEndIndex1 = Integer.MAX_VALUE;
		if (nameEndIndex2 == -1)
			nameEndIndex2 = Integer.MAX_VALUE;
		if (nameEndIndex3 == -1)
			nameEndIndex3 = Integer.MAX_VALUE;
		if (nameEndIndex4 == -1)
			nameEndIndex4 = Integer.MAX_VALUE;
		int nameEndIndex = Math.min(nameEndIndex1, Math.min(nameEndIndex2, Math.min(nameEndIndex3, nameEndIndex4)));
		// If the name end index is too much, make it the first newline, or the length of the string if there is no newline.
		if (nameEndIndex >= rule.length()) {
			nameEndIndex = rule.indexOf('\n', nameBeginIndex);
			if (nameEndIndex == -1) {
				nameEndIndex = rule.length() - 1;
			}
		}
		String ruleName = rule.substring(nameBeginIndex, nameEndIndex).trim();
		return ruleName;
	}
	
	private String replaceRuleBodiesWithBody(String oldRule, String newBody) {
		StringBuffer buff = new StringBuffer();
		int index = 0;
		boolean added = false;
		
		buildBuffer:
		while (true) {
			int startIndex = oldRule.indexOf('{', index);
			if (startIndex == -1) {
				buff.append(oldRule.substring(index));
				break buildBuffer;
			}
			int endIndex = 0;
			int braceDepth = 1;
			
			findBody:
			for ( ; endIndex < oldRule.length(); ++endIndex) {
				char c = oldRule.charAt(endIndex);
				if (c == '{') ++braceDepth;
				if (c == '}') --braceDepth;
				if (braceDepth == 0) {
					break findBody;
				}
			}
			buff.append(oldRule.subSequence(index, startIndex));
			buff.append(newBody);
			added = true;
			index = endIndex;
		}
		if (!added) {
			buff.append("sp " + newBody);
		}
		return buff.toString();
	}
	
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (this.input != null) {
			this.input.getRow().getDatabaseConnection().addListener(this);
		}
	}

	@Override
	public void onEvent(SoarDatabaseEvent event, SoarDatabaseConnection db) {
		if (event.type == Type.DATABASE_CHANGED) {
			
			/* This wasn't working beause if you delete a rule from an operator this won't fire
			SoarDatabaseRow row = this.input.getRow();
			ArrayList<ISoarDatabaseTreeItem> rules = TraversalUtil.getRelatedRules(row);
			if (event.row != null && rules.contains(event.row) || event.row == row) {
				this.doRevertToSaved();
			}
			*/
			
			// Fire every time
			doRevertToSaved();
		}
	}
}
