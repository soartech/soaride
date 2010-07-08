package com.soartech.soar.ide.ui.editors.text.resolvers;

import java.util.ArrayList;

import org.eclipse.jface.text.templates.SimpleTemplateVariableResolver;
import org.eclipse.jface.text.templates.TemplateContext;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.editors.database.SoarDatabaseTextEditorConfiguration;

public class OperatorTemplateResolver extends SimpleTemplateVariableResolver implements ISoarTemplateResolver {
	SoarDatabaseTextEditorConfiguration configuration;
	public OperatorTemplateResolver() {
		super("operator", "The current operator, if available");
		setEvaluationString("operator");
	}
	
	public void setConfiguration(SoarDatabaseTextEditorConfiguration configuration) {
		this.configuration = configuration;
	}
	
	protected String resolve(TemplateContext context) {
		if (configuration != null) {
			SoarDatabaseRow row = configuration.getRow();
			if (row.getTable() == Table.OPERATORS) {
				return row.getName();
			}
			ArrayList<ISoarDatabaseTreeItem> items = row.getJoinedRowsFromTable(Table.OPERATORS);
			if (items.size() == 1) {
				SoarDatabaseRow operator = (SoarDatabaseRow) items.get(0);
				return operator.getName();
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
