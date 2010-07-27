/*
 *Copyright (c) 2009, Soar Technology, Inc.
 *All rights reserved.
 *
 *Redistribution and use in source and binary forms, with or without modification,   *are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  * Neither the name of Soar Technology, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY  *EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED   *WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.   *IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,   *INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT   *NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR   *PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,    *WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)   *ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE    *POSSIBILITY OF SUCH *DAMAGE. 
 *
 * 
 */
package com.soartech.soar.ide.ui.editors.text;

import java.util.ArrayList;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.SimpleTemplateVariableResolver;
import org.eclipse.jface.text.templates.TemplateContextType;

import com.soartech.soar.ide.ui.editors.text.resolvers.AgentTemplateResolver;
import com.soartech.soar.ide.ui.editors.text.resolvers.ISoarTemplateResolver;
import com.soartech.soar.ide.ui.editors.text.resolvers.OperatorTemplateResolver;
import com.soartech.soar.ide.ui.editors.text.resolvers.ProblemSpaceTemplateResolver;
import com.soartech.soar.ide.ui.editors.text.resolvers.RuleTemplateResolver;

public class SoarOperatorContext extends TemplateContextType {

	SoarDatabaseTextEditorConfiguration configuration;
	ArrayList<SimpleTemplateVariableResolver> resolvers = new ArrayList<SimpleTemplateVariableResolver>();
	
   public static final String SOAR_CONTEXT_TYPE = "com.soartech.soar.ide.ui.editors.text.SoarOperatorContext";
   
	public SoarOperatorContext() {
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
