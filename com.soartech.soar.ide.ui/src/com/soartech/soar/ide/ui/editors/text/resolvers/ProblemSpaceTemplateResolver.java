package com.soartech.soar.ide.ui.editors.text.resolvers;

import java.util.ArrayList;

import org.eclipse.jface.text.templates.SimpleTemplateVariableResolver;
import org.eclipse.jface.text.templates.TemplateContext;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.editors.database.SoarDatabaseTextEditorConfiguration;

public class ProblemSpaceTemplateResolver extends SimpleTemplateVariableResolver implements ISoarTemplateResolver {
	
	SoarDatabaseTextEditorConfiguration configuration;
	
	public ProblemSpaceTemplateResolver() {
		super("problemspace", "The current problem space, if available");
		setEvaluationString("problemspace");
	}
	
	public void setConfiguration(SoarDatabaseTextEditorConfiguration configuration) {
		this.configuration = configuration;
	}
	
	protected String resolve(TemplateContext context) {

		if (configuration != null) {
			SoarDatabaseRow row = configuration.getRow();
			ArrayList<ISoarDatabaseTreeItem> items = row.getJoinedRowsFromTable(Table.PROBLEM_SPACES);
			if (items.size() == 1) {
				SoarDatabaseRow problemSpace = (SoarDatabaseRow) items.get(0);
				return problemSpace.getName();
			}
			if (row.getTable() == Table.RULES) {
				ArrayList<ISoarDatabaseTreeItem> operators = row.getJoinedRowsFromTable(Table.OPERATORS);
				if (operators.size() == 1) {
					SoarDatabaseRow operator = (SoarDatabaseRow) operators.get(0);
					items = operator.getJoinedRowsFromTable(Table.PROBLEM_SPACES);
					if (items.size() == 1) {
						SoarDatabaseRow problemSpace = (SoarDatabaseRow) items.get(0);
						return problemSpace.getName();
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public boolean canResolve() {
		String resolve = resolve(null);
		return resolve != null;
	}
}
