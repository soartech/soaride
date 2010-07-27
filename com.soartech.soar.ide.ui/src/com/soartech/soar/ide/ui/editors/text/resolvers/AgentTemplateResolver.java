package com.soartech.soar.ide.ui.editors.text.resolvers;

import org.eclipse.jface.text.templates.SimpleTemplateVariableResolver;
import org.eclipse.jface.text.templates.TemplateContext;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.editors.text.SoarDatabaseTextEditorConfiguration;

public class AgentTemplateResolver extends SimpleTemplateVariableResolver implements ISoarTemplateResolver {
	SoarDatabaseTextEditorConfiguration configuration;
	public AgentTemplateResolver() {
		super("agent", "The current agent");
		setEvaluationString("agent");
	}
	
	public void setConfiguration(SoarDatabaseTextEditorConfiguration configuration) {
		this.configuration = configuration;
	}
	
	protected String resolve(TemplateContext context) {
		if (configuration != null) {
			SoarDatabaseRow row = configuration.getRow();
			SoarDatabaseRow agent = row.getAncestorRow(Table.AGENTS);
			if (agent != null) {
				return agent.getName();
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
