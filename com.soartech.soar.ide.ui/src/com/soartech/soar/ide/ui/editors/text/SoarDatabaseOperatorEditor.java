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

public class SoarDatabaseOperatorEditor extends AbstractSoarDatabaseTextEditor implements ISoarDatabaseTextEditor, ISoarDatabaseEventListener {

	public static final String ID = "com.soartech.soar.ide.ui.editors.database.SoarDatabaseOperatorEditor";
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
		if (input != null) {
			HashMap<String, SoarDatabaseRow> rulesByName = new HashMap<String, SoarDatabaseRow>();
			SoarDatabaseRow operator = input.getRow();
			ArrayList<ISoarDatabaseTreeItem> rules = operator.getJoinedRowsFromTable(Table.RULES);
			for (ISoarDatabaseTreeItem item : rules) {
				if (item instanceof SoarDatabaseRow) {
					SoarDatabaseRow rule = (SoarDatabaseRow) item;
					String ruleName = rule.getName();
					rulesByName.put(ruleName, rule);
				}
			}
			
			// replace only text inside braces
			IDocument doc = getDocumentProvider().getDocument(input);
			Pattern regex = Pattern.compile("sp \\{([^\\}]*)\\}");
			Matcher matcher = regex.matcher(doc.get());
			boolean hasRule = false;
			while (matcher.find()) {
				hasRule = true;
				String match = matcher.group();
				String matchBody = matcher.group(1);
				int nameBeginIndex = match.indexOf('{') + 1;
				int nameEndIndex1 = match.indexOf('(');
				int nameEndIndex2 = match.indexOf("-->");
				int nameEndIndex3 = match.indexOf('}');
				if (nameEndIndex1 == -1) nameEndIndex1 = Integer.MAX_VALUE;
				if (nameEndIndex2 == -1) nameEndIndex2 = Integer.MAX_VALUE;
				if (nameEndIndex3 == -1) nameEndIndex3 = Integer.MAX_VALUE; // shouldn't happen with matching regex
				int nameEndIndex = Math.min(nameEndIndex1, Math.min(nameEndIndex2, nameEndIndex3));
				String ruleName = match.substring(nameBeginIndex, nameEndIndex).trim();
				
				// TODO debug
				//System.out.println("Match:\n" + ruleName);
				//System.out.println("match body: " + matchBody);

				SoarDatabaseRow rule = rulesByName.get(ruleName);
				if (rule == null) {
					rule = operator.getTopLevelRow().createChild(Table.RULES, ruleName);
					SoarDatabaseRow.joinRows(operator, rule, operator.getDatabaseConnection());
				}
				String ruleText = rule.getText();
				StringBuffer newRuleText = new StringBuffer();
				Matcher ruleMatcher = regex.matcher(ruleText);
				int lastIndex = 0;
				boolean matched = false;
				while (ruleMatcher.find()) {
					matched = true;
					int matchStart = ruleMatcher.start();
					newRuleText.append(ruleText.substring(lastIndex, matchStart));
					newRuleText.append("sp {");
					newRuleText.append(matchBody);
					newRuleText.append("}");
					lastIndex = ruleMatcher.end();
				}
				newRuleText.append(ruleText.substring(lastIndex));

				if (!matched) {
					newRuleText.append("sp {");
					newRuleText.append(matchBody);
					newRuleText.append("}");
				}

				// TODO debug
				// System.out.println("New Rule Body:\n" +
				// newRuleText.toString());

				rule.save(newRuleText.toString(), input);
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
			if (!hasRule) {
				this.doRevertToSaved();
			}
		}
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
			ArrayList<ISoarDatabaseTreeItem> rules = this.input.getRow().getJoinedRowsFromTable(Table.RULES);
			if (event.row != null && rules.contains(event.row) || event.row == this.input.getRow()) {
				this.doRevertToSaved();
			}
		}
	}
}
