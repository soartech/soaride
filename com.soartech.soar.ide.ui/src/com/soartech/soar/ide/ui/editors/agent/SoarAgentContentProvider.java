package com.soartech.soar.ide.ui.editors.agent;

import java.net.URLClassLoader;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.model.WorkbenchContentProvider;

import com.google.common.collect.ObjectArrays;
import com.soartech.soar.ide.core.model.ISoarAgent;

public class SoarAgentContentProvider extends WorkbenchContentProvider {
	
	final ISoarAgent agent;
	
	public SoarAgentContentProvider(ISoarAgent agent)
	{
		super();
		this.agent = agent;
	}

	public Object[] getElements(Object inputElement) {
		Object[] ret = super.getElements(inputElement);
		if(inputElement instanceof IProject)
		{
			final URLClassLoader urlClassLoader = (URLClassLoader)agent.getClassLoader();
			if(urlClassLoader != null)
			{
				final Object[] urls = urlClassLoader.getURLs();
				ret = ObjectArrays.concat(ret, urls, Object.class);
			}
		}
		
		return ret;
	}
}
