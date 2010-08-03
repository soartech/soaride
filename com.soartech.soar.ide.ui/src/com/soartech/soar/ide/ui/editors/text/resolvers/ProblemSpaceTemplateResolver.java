package com.soartech.soar.ide.ui.editors.text.resolvers;

import java.util.ArrayList;

import org.eclipse.jface.text.templates.SimpleTemplateVariableResolver;
import org.eclipse.jface.text.templates.TemplateContext;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.editors.text.SoarDatabaseTextEditorConfiguration;

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
			Table rowTable = row.getTable();
			if (rowTable == Table.RULES) {
				ArrayList<SoarDatabaseRow> operators = row.getDirectedJoinedParentsOfType(Table.OPERATORS);
				if (operators.size() == 1) {
					SoarDatabaseRow operator = (SoarDatabaseRow) operators.get(0);
					ArrayList<SoarDatabaseRow> items = operator.getDirectedJoinedParentsOfType(Table.PROBLEM_SPACES);
					if (items.size() == 1) {
						SoarDatabaseRow problemSpace = (SoarDatabaseRow) items.get(0);
						return problemSpace.getName();
					}
				}
			} else if (rowTable == Table.OPERATORS) {
				ArrayList<SoarDatabaseRow> items = row.getDirectedJoinedParentsOfType(Table.PROBLEM_SPACES);
				if (items.size() == 1) {
					SoarDatabaseRow problemSpace = items.get(0);
					return problemSpace.getName();
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
