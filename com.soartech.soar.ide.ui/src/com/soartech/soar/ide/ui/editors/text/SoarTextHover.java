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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.IExpandedTclCode;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.ITclHelpModel;
import com.soartech.soar.ide.core.model.ITclProcedureHelp;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

/**
 * Provides text hover implementation.
 * 
 * @author aron
 *
 */
public class SoarTextHover implements ITextHover 
{
	private SoarEditor editor;
	
	public SoarTextHover(SoarEditor editor)
	{
		this.editor = editor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) 
	{
		IDocument doc = textViewer.getDocument();
		
		final String content = doc.get();
        final char[] charContent = content.toCharArray();
        String tclHover = checkForTclCommand(hoverRegion, charContent);
		if(tclHover != null)
		{
			return tclHover;
		}
        tclHover = checkForTclVariable(hoverRegion.getOffset(), content);
        if(tclHover != null)
        {
            return tclHover;
        }
		
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) 
	{
		return new Region(offset, 0);
	}

    private String checkForTclVariable(int offset, String content)
    {
        int start = offset;
        while(start >= 0)
        {
            char c = content.charAt(start);
            if(Character.isWhitespace(c))
            {
                start = -1;
            }
            else if(c == '$')
            {
                break;
            }
            --start;
        }
        if(start < 0)
        {
            return null;
        }
        
        int end = offset;
        while(end < content.length())
        {
            char c = content.charAt(end);
            if(Character.isWhitespace(c) || !Character.isJavaIdentifierPart(c))
            {
                break;
            }
            ++end;
        }
        
        if(end < start || end >= content.length())
        {
            return null;
        }
        
        String var = content.substring(start, end);
        try
        {
            ISoarFile file = editor.getSoarFileWorkingCopy();
            if(file == null)
            {
                return null;
            }
            ISoarFileAgentProxy proxy = file.getPrimaryAgentProxy();
            ISoarAgent agent = proxy != null ? proxy.getAgent() : null;
            
            if(agent == null)
            {
                return null;
            }
            
            IExpandedTclCode code = agent.expandTclString("::", var, 0);
            if(code.getError() != null)
            {
                return null;
            }
            String result = code.getResultString();
            
            return result;
        }
        catch (SoarModelException e)
        {
            SoarEditorUIPlugin.log(e);
        }
        return null;
    }
	/**
	 * Check the given region to see if it is a tcl command. If it is,
	 * return a string representing the 'help' for that command.
	 * 
	 * @param region The region to check.
	 * @param file The open file.
	 * @return The help string.
	 */
	private String checkForTclCommand(IRegion region, char[] source)
	{
		int offset = region.getOffset();
        		
		IRegion procRegion = getWhitespaceDelimitedRegion(source, offset);
		
		//get the name of the 'hovered over' procedure
		String procName = getStringFromRegion(source, procRegion);
		
		ISoarFile file = editor.getSoarFileWorkingCopy();
		if(file == null)
		{
			return null;
		}
        ISoarProject project = (ISoarProject) file.getParent();
        if(project == null)
        {
            return null;
        }
        
		try
        {
            ISoarFileAgentProxy proxy = file.getPrimaryAgentProxy();
            ISoarAgent agent = proxy != null ? proxy.getAgent() : null;
            
            ISoarModel soarModel = SoarCorePlugin.getDefault().getSoarModel();
            ITclHelpModel helpModel = soarModel.getTclHelpModel();
            
            //get the help object from the help model
            ITclProcedureHelp procHelp = helpModel.getHelp(procName, project, agent);
            
            if(procHelp != null)
            {
            	return procHelp.getHoverHelpText();
            }
        }
        catch (SoarModelException e)
        {
            SoarEditorUIPlugin.log(e);
        }
		
		return null;
	}
	
	/**
	 * Get the index of the beginning of a tcl command.
	 * 
	 * @param source The source text.
	 * @param offset The offset into the source to search from.
	 * @return The begin index.
	 */
	private int getTclCommandBeginIndex(char[] source, int offset)
	{
		int index = offset;
		
		while(index >= 0)
		{
			char c = source[index];
			
			if(Character.isWhitespace(c))
			{
				return index;
			}
			
			if(c == '[' || c == '{' || c == '\"')
			{
				return index;
			}
			
			index--;
		}
		
		return index;
	}
	
	/**
	 * Get the index of the end of a tcl command.
	 * 
	 * @param source The source text.
	 * @param offset The offset into the source to search from.
	 * @return The end index.
	 */
	private int getTclCommandEndIndex(char[] source, int offset)
	{
		int index = offset;
		
		while(index < source.length)
		{
			char c = source[index];
			
			if(Character.isWhitespace(c))
			{
				return index;
			}
			
			if(c == ']' || c == '}' || c == '\"')
			{
				return index;
			}
			
			index++;
		}
		
		return index;
	}
	
	/**
	 * Get the string represented by a char[] and region.
	 * 
	 * @param source The source text.
	 * @param region The region of text.
	 * @return The source string.
	 */
	private String getStringFromRegion(char[] source, IRegion region)
	{
		String str = "";
		
		int beginIndex = region.getOffset();
		int endIndex = beginIndex + region.getLength();
		
		for(int i = beginIndex; i < endIndex; i++)
		{
			str += source[i];
		}
		
		return str;
	}
	
	/**
	 * Get a region for a whitespace delimited string.
	 * 
	 * Any square brackets and quotes will also count as delimiters. 
	 * 
	 * @param source The source text.
	 * @param offset The offset around which to find the region.
	 * @return The new delimited region.
	 */
	private IRegion getWhitespaceDelimitedRegion(char[] source, int offset)
	{	
		int beginIndex = getTclCommandBeginIndex(source, offset);
		
		int endIndex = getTclCommandEndIndex(source, offset);
		
		beginIndex++;
		
		return new Region(beginIndex, endIndex - beginIndex);
	}
	
}
