package com.soartech.soar.ide.ui.editors.text.resolvers;

import org.eclipse.jface.text.templates.SimpleTemplateVariableResolver;
import org.eclipse.jface.text.templates.TemplateContext;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.editors.database.SoarDatabaseTextEditorConfiguration;

public class RuleTemplateResolver extends SimpleTemplateVariableResolver implements ISoarTemplateResolver {

	SoarDatabaseTextEditorConfiguration configuration;

	public RuleTemplateResolver() {
		super("rule", "The current problem space, if available");
		setEvaluationString("rule");
	}

	public void setConfiguration(SoarDatabaseTextEditorConfiguration configuration) {
		this.configuration = configuration;
	}

	protected String resolve(TemplateContext context) {
		if (configuration != null) {
			SoarDatabaseRow row = configuration.getRow();
			if (row.getTable() == Table.RULES) {
				return row.getName();
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
