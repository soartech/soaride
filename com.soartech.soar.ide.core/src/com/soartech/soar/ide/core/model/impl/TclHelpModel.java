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
package com.soartech.soar.ide.core.model.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.ITclHelpModel;
import com.soartech.soar.ide.core.model.ITclProcedure;
import com.soartech.soar.ide.core.model.ITclProcedureHelp;
import com.soartech.soar.ide.core.model.SoarModelException;

/**
 * The tcl help model implementation.
 * 
 * @author aron
 *
 */
public class TclHelpModel implements ITclHelpModel 
{
	private Map<String,Element> manpages = new HashMap<String,Element>();
	
	private Map<String,ITclProcedureHelp> cachedBuiltInHelp = 
		new HashMap<String, ITclProcedureHelp>(); 
	
	/**
	 * Constructor.
	 *
	 */
	public TclHelpModel()
	{		
		if(!initBuiltInHelp("tclHelp.xml"))
		{
			SoarCorePlugin.log(new Status(IStatus.WARNING, SoarCorePlugin.PLUGIN_ID, 0, 
					"Tcl BuiltIn Help NOT Initialized", null));
		}
		
		if(!initBuiltInHelp("soarHelp.xml"))
		{
			SoarCorePlugin.log(new Status(IStatus.WARNING, SoarCorePlugin.PLUGIN_ID, 0, 
					"Soar BuiltIn Help NOT Initialized", null));
		}
	}
	
	/* (non-Javadoc)
	 * @see com.soartech.soar.ide.core.model.ITclHelpModel#getHelp(java.lang.String, com.soartech.soar.ide.core.model.ISoarProject, com.soartech.soar.ide.core.model.ISoarAgent)
	 */
	public ITclProcedureHelp getHelp(String name, ISoarProject project, ISoarAgent agent) 
	{
		if(project == null)
		{
			return getBuiltInHelp(name);
		}
		
        try
        {
            ITclProcedure procedure = null;
            if(agent != null)
            {
                procedure = agent.getProcedure(name);
            }
            else
            {
                for(ISoarAgent projectAgent : project.getAgents())
                {
                    procedure = projectAgent.getProcedure(name);
                    if(procedure != null)
                    {
                        break;
                    }
                }
            }
            if(procedure != null)
            {
                return procedure.getHelp();
            }
        }
        catch (SoarModelException e)
        {
            SoarCorePlugin.log(e);
        }
		
		return getBuiltInHelp(name);
	}
	
	/* (non-Javadoc)
	 * @see com.soartech.soar.ide.core.model.ITclHelpModel#hasHelp(java.lang.String)
	 */
	public boolean hasHelp(String procName)
	{
		return manpages.containsKey(procName);
	}
	
	/**
	 * Get a help object for a built-in procedure.
	 * 
	 * @param procName The procedure name.
	 * @return The new help object.
	 */
	private ITclProcedureHelp getBuiltInHelp(String procName)
	{
		if(cachedBuiltInHelp.containsKey(procName))
		{
			return cachedBuiltInHelp.get(procName);
		}
		
		//look up the corresponding manpage element
		Element elem = manpages.get(procName);
		
		if(elem == null)
		{
			return null;
		}
		
		//get the help string
		String helpString = getHelpString(elem);
		
		//create the help object
		ITclProcedureHelp help = new TclProcedureHelp(helpString, procName, true);
		
		//cache the help object
		cachedBuiltInHelp.put(procName, help);
		
		return help;
	}
	
	/**
	 * Get the help string to be displayed in the hover.
	 * 
	 * This method is for built-in help objects.
	 * 
	 * @param parentElem The jdom element to get help for.
	 * @return The help string.
	 */
	private String getHelpString(Element parentElem)
	{
		Element namesectionElem = parentElem.getChild("namesection");
		
		Element nameElem = namesectionElem.getChild("name");
		String name = nameElem.getText();
		
		Element descElem = namesectionElem.getChild("desc");
		String desc = descElem.getText();
		
		Element synopsisElem = parentElem.getChild("synopsis");
		Element syntaxElem = synopsisElem.getChild("syntax");
		
		XMLOutputter outputter = new XMLOutputter();
		String syntax = outputter.outputString(syntaxElem);
		
		syntax = removeTags(syntax);
		
		String helpString = "Name:\n" + name + " - " + desc + "\n\n" + "Synopsis:\n" + syntax;
		
		return helpString;
	}
	
	/**
	 * Utility method to remove extraneous formatting tags from the help string.
	 * 
	 * @param source The source string.
	 * @return The re-formatted string.
	 */
	private String removeTags(String source)
	{	
		int beginIndex = source.indexOf("<");
		int endIndex = source.indexOf(">");
		
		while(beginIndex >= 0)
		{
			CharSequence cs = source.subSequence(beginIndex, endIndex + 1);
		
			source = source.replace(cs, "");
			
			beginIndex = source.indexOf("<");
			endIndex = source.indexOf(">");
		}
		
		return source;
	}
	
    private InputStream getBuiltInHelpInputStream(String filename) throws IOException
    {
        URL baseURL = new URL(
                  Platform.getBundle(SoarCorePlugin.PLUGIN_ID).getEntry("/"),
                  "conf/" + filename);
        
        try
        {
            return new BufferedInputStream(baseURL.openStream());
        }
        catch (IOException e)
        {
        }
        URL newURL = FileLocator.resolve(baseURL);
        
        return new BufferedInputStream(newURL.openStream());
    }
	/**
	 * Initialize the built-in help.
	 * 
	 * @param filename The neame of the help file.
	 * @return True if built-in help was initialized, false otherwise.
	 */
	@SuppressWarnings("unchecked")
	private boolean initBuiltInHelp(String filename)
	{		
        InputStream stream = null;
        try
        {
            stream = getBuiltInHelpInputStream(filename);
            SAXBuilder builder = new SAXBuilder();
            
            Document doc = builder.build(stream);
            Element root = doc.getRootElement();
            
            List<Element> manpageList = root.getChildren();
            
            for(Element e:manpageList)
            {
                Attribute attr = e.getAttribute("title");
                if(attr != null)
                {
                    if(!manpages.containsKey(attr.getValue()))
                    {
                        manpages.put(attr.getValue(), e);
                    }
                }
            }
        }
        catch (IOException e)
        {
            SoarCorePlugin.log(e);
            return false;
        }
        catch (JDOMException e)
        {
            SoarCorePlugin.log(e);
            return false;
        }
        finally
        {
            if(stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException e)
                {
                }
            }
        }
        
		return true;
	}

}
