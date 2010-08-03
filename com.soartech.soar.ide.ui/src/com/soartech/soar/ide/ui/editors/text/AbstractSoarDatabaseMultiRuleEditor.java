package com.soartech.soar.ide.ui.editors.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.IEditorInput;

import com.soartech.soar.ide.core.SoarProblem;
import com.soartech.soar.ide.core.sql.ISoarDatabaseEventListener;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarEditorPluginImages;
import com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal.TraversalUtil;

public abstract class AbstractSoarDatabaseMultiRuleEditor extends AbstractSoarDatabaseTextEditor implements ISoarDatabaseTextEditor, ISoarDatabaseEventListener {
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
		if (input != null) {
			HashMap<String, SoarDatabaseRow> rulesByName = new HashMap<String, SoarDatabaseRow>();
			SoarDatabaseRow row = input.getRow();
			ArrayList<ISoarDatabaseTreeItem> rules = row.getJoinedRowsFromTable(Table.RULES);
			for (ISoarDatabaseTreeItem item : rules) {
				if (item instanceof SoarDatabaseRow) {
					SoarDatabaseRow rule = (SoarDatabaseRow) item;
					String ruleName = rule.getName();
					rulesByName.put(ruleName, rule);
				}
			}
			
			IDocument doc = getDocumentProvider().getDocument(input);
			ArrayList<String> rulesText = getRulesFromText(doc.get());
			for (String ruleText : rulesText) {
				String ruleName = getNameOfRules(ruleText);
				SoarDatabaseRow rule = rulesByName.get(ruleName);
				if (rule == null) {
					rule = row.getTopLevelRow().createChild(Table.RULES, ruleName);
					addRow(rule);
				}
				rule.save(ruleText, input);
			}
			
			input.clearProblems();
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
	
	private ArrayList<String> getRulesFromText(String doc) {
		ArrayList<String> ret = new ArrayList<String>();
		int braceDepth = 0;
		StringBuffer buff = new StringBuffer();
		boolean string = false;
		boolean comment = false;
		for (char c : doc.toCharArray()) {
			buff.append(c);
			if (comment && c == '\n') comment = false;
			if (!string && c == '#') comment = true;
			if (!comment && c == '|') string = !string;
			if (comment) continue;
			if (c == '{') ++braceDepth;
			if (c == '}') --braceDepth;
			if (c < 0) c = 0;
			if (c == '}' && braceDepth == 0) {
				ret.add(buff.toString().trim());
				buff = new StringBuffer();
			}
		}
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
		int nameEndIndex1 = rule.indexOf('(');
		int nameEndIndex2 = rule.indexOf("-->");
		int nameEndIndex3 = rule.lastIndexOf('}');
		if (nameEndIndex1 == -1)
			nameEndIndex1 = Integer.MAX_VALUE;
		if (nameEndIndex2 == -1)
			nameEndIndex2 = Integer.MAX_VALUE;
		if (nameEndIndex3 == -1)
			nameEndIndex3 = Integer.MAX_VALUE; // shouldn't happen with matching regex
		int nameEndIndex = Math.min(nameEndIndex1, Math.min(nameEndIndex2, nameEndIndex3));
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
