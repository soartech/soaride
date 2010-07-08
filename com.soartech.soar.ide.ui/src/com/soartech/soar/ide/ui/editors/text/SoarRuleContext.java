package com.soartech.soar.ide.ui.editors.text;

import java.util.ArrayList;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.SimpleTemplateVariableResolver;
import org.eclipse.jface.text.templates.TemplateContextType;

import com.soartech.soar.ide.ui.editors.database.SoarDatabaseTextEditorConfiguration;
import com.soartech.soar.ide.ui.editors.text.resolvers.AgentTemplateResolver;
import com.soartech.soar.ide.ui.editors.text.resolvers.ISoarTemplateResolver;
import com.soartech.soar.ide.ui.editors.text.resolvers.OperatorTemplateResolver;
import com.soartech.soar.ide.ui.editors.text.resolvers.ProblemSpaceTemplateResolver;
import com.soartech.soar.ide.ui.editors.text.resolvers.RuleTemplateResolver;

public class SoarRuleContext extends TemplateContextType {

	SoarDatabaseTextEditorConfiguration configuration;
	ArrayList<SimpleTemplateVariableResolver> resolvers = new ArrayList<SimpleTemplateVariableResolver>();
	
    public static final String SOAR_CONTEXT_TYPE = "com.soartech.soar.ide.ui.editors.text.SoarRuleContext";
   
    public SoarRuleContext() {
    	resolvers.add(new GlobalTemplateVariables.Cursor());
    	resolvers.add(new GlobalTemplateVariables.WordSelection());
    	resolvers.add(new GlobalTemplateVariables.LineSelection());
    	resolvers.add(new GlobalTemplateVariables.Dollar());
    	resolvers.add(new GlobalTemplateVariables.Date());
		resolvers.add(new GlobalTemplateVariables.Year());
		resolvers.add(new GlobalTemplateVariables.Time());
		resolvers.add(new GlobalTemplateVariables.User());

		resolvers.add(new ProblemSpaceTemplateResolver());
		resolvers.add(new OperatorTemplateResolver());
		resolvers.add(new AgentTemplateResolver());
		resolvers.add(new RuleTemplateResolver());

		for (SimpleTemplateVariableResolver resolver : resolvers) {
			addResolver((SimpleTemplateVariableResolver) resolver);
		}
	}

	public void setConfiguration(SoarDatabaseTextEditorConfiguration configuration) {
		removeAllResolvers();
		for (SimpleTemplateVariableResolver resolver : resolvers) {
			if (resolver instanceof ISoarTemplateResolver) {
				ISoarTemplateResolver soarResolver = (ISoarTemplateResolver) resolver;
				soarResolver.setConfiguration(configuration);
				if (soarResolver.canResolve()) {
					addResolver(resolver);
				}
			} else {
				addResolver(resolver);
			}
		}
   }
}