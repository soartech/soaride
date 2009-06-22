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
package com.soartech.soar.ide.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.soartech.soar.ide.ui.editors.text.SoarEditor;
import com.soartech.soar.ide.ui.editors.text.autoedit.TabPrefs;

/**
 * Format a soar document to give it consistent indentation.
 * 
 * @author aron
 *
 */
public class FormatSoarDocumentAction implements IEditorActionDelegate
{   
    private TabPrefs prefs;
    
    public TabPrefs getIndentPrefs()
    {
        if (this.prefs == null)
        {
            this.prefs = new TabPrefs();
        }
        return this.prefs;
    }
    
    private SoarEditor editor;
    private ISelection selection;
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) 
    {
        if(targetEditor instanceof SoarEditor)
        {
            this.editor = (SoarEditor) targetEditor;
        }
        else
        {
            this.editor = null;
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) 
    {
        this.selection = selection;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) 
    {
        if(!isSelectionValid() || editor == null)
        {
            return;
        }
        
        //do nothing if this is a tcl file
        String inputName = editor.getEditorInput().getName();
        if(inputName.endsWith(".tcl"))
        {
            return;
        }
                
        IDocumentProvider provider = editor.getDocumentProvider();
        if(provider == null)
        {
            return;
        }
        IDocument document = provider.getDocument(editor.getEditorInput());
                        
        ITextSelection sel = (ITextSelection) selection;
        
        int startLine = 0;
        int endLine = 0;
        
        if(sel.getLength() == 0)
        {
            startLine = 0;
            endLine = document.getNumberOfLines() - 1;
        }
        else
        {
            startLine = sel.getStartLine();
            endLine = sel.getEndLine();
        }
        
        try {
            formatSoarDocument(document, startLine, endLine);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Format the document from the given start line to the given end line.
     * 
     * @param document The document.
     * @param startLine The start line.
     * @param endLine The end line.
     * @throws BadLocationException
     */
    private void formatSoarDocument(IDocument document, int startLine, int endLine) throws BadLocationException
    {
        //create a new document to do the work on line by line
        IDocument newDoc = new Document(document.get());
        
        //format each selected line in the new document 
        for(int i = startLine; i <= endLine; i++)
        {
            IRegion region = newDoc.getLineInformation(i);
            String formattedLine = formatLine(newDoc, i);
            
            newDoc.replace(region.getOffset(), region.getLength(), formattedLine);
        }
        
        int startOffset = document.getLineOffset(startLine);
        int endOffset = document.getLineOffset(endLine) + document.getLineInformation(endLine).getLength();
        
        int newDocStartOffset = newDoc.getLineOffset(startLine);
        int newDocEndOffset = newDoc.getLineOffset(endLine) + newDoc.getLineInformation(endLine).getLength();
        
        //get the formatted block of text from the new document
        String formattedBlock = newDoc.get(newDocStartOffset, newDocEndOffset - newDocStartOffset);
        
        //replace the text in the editor's document
        document.replace(startOffset, endOffset - startOffset, formattedBlock);
    }
    
    /**
     * Format the line of the given document to proper soar indenting.
     * 
     * @param document The document.
     * @param lineNumber The line to format.
     * @return The newly formatted line.
     * @throws BadLocationException
     */
    private String formatLine(IDocument document, int lineNumber) throws BadLocationException
    {
        IRegion region = document.getLineInformation(lineNumber);
        String line = document.get(region.getOffset(), region.getLength());
        
        String prevLine = null;
        int prevIndent = -1;
        try
        {
            IRegion prevRegion = document.getLineInformation(lineNumber - 1);
            prevLine = document.get(prevRegion.getOffset(), prevRegion.getLength());
            prevIndent = 0;
            while(prevIndent < prevLine.length() && 
                  Character.isWhitespace(prevLine.charAt(prevIndent)))
            {
                ++prevIndent;
            }
            if(prevIndent == prevLine.length())
            {
                prevLine = null;
            }
            else if(prevLine.charAt(prevIndent) == '-')
            {
                prevIndent++;
            }
        }
        catch(BadLocationException e)
        {
        }
        
        String trimmed = line.trim();
        
        //remove the tabs from the line if preferences are set that way
        if(getIndentPrefs().getUseSpaces())
        {
            trimmed = replaceTabsWithSpaces(trimmed);
        }
        
        String formatted = null;
        
        if(trimmed.startsWith("sp"))
        {
            formatted = trimmed;
        }
        else if(trimmed.startsWith("("))
        {
            formatted = addIndent(trimmed, getIndentPrefs().getIndentationString());
        }
        else if(trimmed.startsWith("-("))
        {
            String indent = getIndentPrefs().getIndentationString();
            formatted = addIndent(trimmed, indent.length() - 1);
        }
        else if(trimmed.startsWith("["))
        {
            formatted = addIndent(trimmed, getIndentPrefs().getIndentationString());
        }
        else if(trimmed.startsWith("^") && prevLine != null)
        {
            formatted = addIndent(trimmed, prevLine.indexOf('^'));
        }
        else if(trimmed.startsWith("-^") && prevLine != null)
        {
            formatted = addIndent(trimmed, prevLine.indexOf('^') - 1);
        }
        else if(trimmed.startsWith("#"))
        {
            if(prevLine != null)
            {
                formatted = addIndent(trimmed, prevIndent);
            }
            else
            {
                formatted = trimmed;
            }
        }
        else if(trimmed.startsWith("-{"))
        {
            formatted = addIndent(trimmed, 
                    prevLine != null ? prevIndent - 1  : 
                                       getIndentPrefs().getTabWidth() - 2);
        }
        else if(trimmed.startsWith("-->"))
        {
            formatted = trimmed;
        }
        else if(trimmed.startsWith("}"))
        {
            formatted = trimmed;
        }
        else
        {
            formatted = replaceTabsWithSpaces(line);
        }

        return formatted;
    }
    
    /**
     * Remove the tabs from the given line, inserting spaces instead.
     * 
     * @param line The line of text.
     * @return The line of text with tabs replaced with spaces.
     */
    private String replaceTabsWithSpaces(String line)
    {
        int tabWidth = getIndentPrefs().getTabWidth();
        
        String spaces = "";
        for(int i = 0; i < tabWidth; i++)
        {
            spaces += " ";
        }
        
        return line.replace("\t", spaces);
    }

    /**
     * Add the specified amount of indent as spaces to the beginning of
     * the text string.
     * 
     * @param text The text string.
     * @param length The amount of indent to add.
     * @return The newly indented string.
     */
    private String addIndent(String text, int length)
    {
        String indent = "";
        for(int i = 0; i < length; i++)
        {
            indent += " ";
        }
        text = indent + text;
        
        return text;
    }
    
    /**
     * Add the specified amount of indent as spaces to the beginning of
     * the text string.
     * 
     * @param text The text string.
     * @param length The amount of indent to add.
     * @return The newly indented string.
     */
    private String addIndent(String text, String indent)
    {
        text = indent + text;
        
        return text;
    }
    
    /**
     * Is the given selection valid for this action?
     * 
     * @return whether we can take this action on the selection
     */
    private boolean isSelectionValid() 
    {  
        if(selection instanceof ITextSelection)
        {            
            ITextSelection s = (ITextSelection) selection;
            if(s != null && !s.isEmpty())
            {
                return true;
            }
        }
        
        return false;
    }
}
