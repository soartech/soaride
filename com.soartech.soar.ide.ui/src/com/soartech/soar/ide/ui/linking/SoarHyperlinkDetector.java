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
package com.soartech.soar.ide.ui.linking;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import com.soartech.soar.ide.core.Logger;
import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ITclFileReference;
import com.soartech.soar.ide.core.model.ITclHelpModel;
import com.soartech.soar.ide.core.model.ITclProcedure;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.editors.text.SoarEditor;

/**
 * Hyperlink detector for the Soar Editor.
 * 
 * @author aron
 */
public class SoarHyperlinkDetector implements IHyperlinkDetector 
{
	private SoarEditor editor;
		
	public SoarHyperlinkDetector(SoarEditor editor)
	{
		this.editor = editor;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
			IRegion region, boolean canShowMultipleHyperlinks) 
	{	
		ISoarFile file = editor.getSoarFileWorkingCopy();
        if(file == null)
        {
            return null;
        }
        
        IHyperlink[] fileHyperlink = checkForFileReference(region, file);
        if(fileHyperlink != null)
        {
            return fileHyperlink;
        }
        
        IHyperlink[] tclCommandLink = checkForTclCommand(region, file);
        if(tclCommandLink != null)
        {
        	return tclCommandLink;
        }
        
        IHyperlink[] tclBuiltInLink = checkForTclBuiltInCommand(region, file);
        if(tclBuiltInLink != null)
        {
        	return tclBuiltInLink;
        }
        
        
		return null;
	}
	
	/**
	 * Get a region for a whitespace delimited string.
	 * 
	 * Any square brackets will also count as delimiters. 
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
	
	/**
	 * Check the given region to see if it is a tcl command. If it is,
	 * return a new hyperlink representing that command.
	 * 
	 * @param region The region to check.
	 * @param file The open file.
	 * @return The new hyperlink.
	 */
	private IHyperlink[] checkForTclCommand(IRegion region, ISoarFile file)
	{
		int offset = region.getOffset();
        		
		try {
			char[] source = file.getBuffer().getCharacters();
			if(source == null || source.length == 0)
            {
			    return null;
            }
            
			IRegion linkRegion = getWhitespaceDelimitedRegion(source, offset);
			
			String linkStr = getStringFromRegion(source, linkRegion);
			
            ISoarFileAgentProxy proxy = file.getPrimaryAgentProxy();
            ISoarAgent agent = proxy != null ? proxy.getAgent() : null;
            
			ITclProcedure procedure = agent != null ? agent.getProcedure(linkStr) : null;
			
			if(procedure != null)
			{
				return new IHyperlink[] {new SoarHyperlink(linkRegion, procedure)};
			}
			
		} catch (SoarModelException e) {
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
		int index = Math.min(offset, source.length - 1);
		
		while(index >= 0)
		{
			char c = source[index];
			
			if(Character.isWhitespace(c))
			{
				return index;
			}
			
			if(c == '[' || c == '{')
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
			
			if(c == ']' || c == '}')
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
		    if(i >= source.length)
		    {
		        Logger.log("[SoarHyperlinkDetector]:getStringFromRegion() beginIndex: " + beginIndex + " endIndex: " + endIndex);
		        Logger.log("[SoarHyperlinkDetector]:getStringFromRegion() ERROR: ArrayOutOfBounds - current string: " + str);
		        break;
		    }
		    
			str += source[i];
		}
		
		return str;
	}
	
	/**
	 * Check for help documentation for a built-in tcl command. 
	 * 
	 * @param region The region to check.
	 * @param file The source file.
	 * @return The new hyperlink.
	 */
	private IHyperlink[] checkForTclBuiltInCommand(IRegion region, ISoarFile file)
	{
		int offset = region.getOffset();
		
		try {
			char[] source = file.getBuffer().getCharacters();
			
			IRegion linkRegion = getWhitespaceDelimitedRegion(source, offset);
			
			String linkStr = getStringFromRegion(source, linkRegion);
			
			ISoarProject project = (ISoarProject) file.getParent();
			
			ITclHelpModel helpModel = project.getSoarModel().getTclHelpModel();
			
			//get the file path to the help
			URL baseURL = new URL(
				          Platform.getBundle(SoarCorePlugin.PLUGIN_ID).getEntry("/"),
				          "conf/tclHtmlHelp/");
			
			URL newURL = FileLocator.resolve(baseURL);
			
			String path = newURL.getFile();
            
            // On Windows we end up with "/c:/..." so we have to strip off the
            // leading slash :(
			if(path.length() > 2 && path.charAt(0) == '/' && path.charAt(2) == ':')
            {
			    path = path.substring(1);
            }
            
			//append the filename to the end of the path
			path += linkStr + ".html";
			
			//don't create a file hyperlink if the file does not exist 
			File f = new File(path);
			if(!f.exists())
			{
				return null;
			}
			
			if(helpModel.hasHelp(linkStr))
			{					
				return new IHyperlink[] { new SoarHtmlHyperlink(linkRegion, path)};
			}
			
		} catch (SoarModelException e) {
            SoarEditorUIPlugin.log(e);
		} catch (MalformedURLException e) {
			SoarEditorUIPlugin.log(e);
		} catch (IOException e) {
			SoarEditorUIPlugin.log(e);
		}
		
		return null;
	}
	
	/**
	 * Check the given region to see if it is a file reference. If it is,
	 * create a new hyperlink representing that file reference.
	 * 
	 * @param region The region to check.
	 * @param file The open file.
	 * @return The new hyperlink.
	 */
	private IHyperlink[] checkForFileReference(IRegion region, ISoarFile file)
	{
		try {
            for(ISoarFileAgentProxy proxy : file.getAgentProxies())
            {
    			for(ISoarElement element:proxy.getChildren())
    			{
    				if(element instanceof ITclFileReference)
    				{
    					ITclFileReference fileReference = (ITclFileReference) element;
    					
                        ISoarSourceRange sourceRange = fileReference.getSourceRange();
                        
                        if(region.getOffset() < sourceRange.getOffset() || 
                           region.getOffset() >= sourceRange.getEnd())
                        {
                            continue;
                        }
                        
                        // Ignore if it's a directory and not a file
                        if(fileReference.isDirectory())
                        {
                            continue;
                        }
                        
    					ISoarFile referencedFile = fileReference.getReferencedSoarFile();
    					
    					// Ignore if the file is not in the eclipse workspace
                        // TODO: For external files use the approach described here to
                        // open a read-only editor on the external file
                        // <http://wiki.eclipse.org/index.php/FAQ_How_do_I_open_an_editor_on_something_that_is_not_a_file%3F>
    					if(referencedFile == null)
    					{
    						continue;
    					}
                        
                        IRegion linkRegion = new Region(sourceRange.getOffset(), sourceRange.getLength());
                        return new IHyperlink[] {new SoarFileHyperlink(linkRegion, fileReference)};
    				}
    			}
            }
		} catch (SoarModelException e) {
			SoarEditorUIPlugin.log(e.getStatus());
		}
		
		return null;
	}
}
