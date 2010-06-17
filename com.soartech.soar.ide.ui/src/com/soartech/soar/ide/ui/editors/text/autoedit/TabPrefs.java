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
package com.soartech.soar.ide.ui.editors.text.autoedit;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.EditorsUI;

import com.soartech.soar.ide.ui.editors.text.SoarEditorPreferencePage;

/**
 * @author aron
 *
 */
public class TabPrefs implements IPropertyChangeListener
{	
    private boolean convertTabs;

    private int tabWidth;
    
	/**
     * Field representing an empty string.
     */
    public static final String EMPTY_STRING = "";

    public TabPrefs()
    {
    	IPreferenceStore prefs = EditorsUI.getPreferenceStore();
    	
    	prefs.setDefault(SoarEditorPreferencePage.TAB_WIDTH, 3);
    	
    	convertTabs = prefs.getBoolean(SoarEditorPreferencePage.CONVERT_TABS);
    	tabWidth = prefs.getInt(SoarEditorPreferencePage.TAB_WIDTH);
    	
    	prefs.addPropertyChangeListener(this);
    }
    
    /* (non-Javadoc)
     * @see com.soartech.hlsr.ui.editors.autoedit.IIndentPrefs#getUseSpaces()
     */
    public boolean getUseSpaces()
    {
        return convertTabs;
    }

    /* (non-Javadoc)
     * @see com.soartech.hlsr.ui.editors.autoedit.IIndentPrefs#getTabWidth()
     */
    public int getTabWidth()
    {
        return tabWidth;
    }
    
    /**
     * Naive implementation. Always redoes the indentation string based in the
     * spaces and tabs settings.
     *
     * @see com.soartech.hlsr.ui.editors.autoedit.ITabPrefs#getIndentationString()
     */
    public String getIndentationString()
    {
        if (getUseSpaces())
        {
            return createSpaceString(getTabWidth());
        }
        else
        {
            return "\t";
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.hlsr.ui.editors.autoedit.IIndentPrefs#convertToStd(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
     */
    public void convertToStd(IDocument document, DocumentCommand command)
    {
        try {
            command.text = convertTabsToSpaces(document, command.length, command.text, command.offset, getIndentationString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replaces tabs if needed by ident string or just a space depending of the
     * tab location
     *
     */
    private String convertTabsToSpaces(
        IDocument document, int length, String text, int offset,
        String indentString) throws BadLocationException
    {
        // only interresting if it contains a tab (also if it is a tab only)
        if (text.indexOf("\t") != -1) {
            // get some text infos

            if (text.equals("\t")) {
                //only a single tab?
                deleteWhitespaceAfter(document, offset);
                text = indentString;

            } else {
                // contains a char (pasted text)
                byte[] byteLine = text.getBytes();
                StringBuffer newText = new StringBuffer();
                for (int count = 0; count < byteLine.length; count++) {
                    if (byteLine[count] == '\t'){
                        newText.append(indentString);

                    } else { // if it is not a tab add the char
                        newText.append((char) byteLine[count]);
                    }
                }
                text = newText.toString();
            }
        }
        return text;
    }

    /**
     * When hitting TAB, delete the whitespace after the cursor in the line
     */
    private void deleteWhitespaceAfter(IDocument document, int offset)
        throws BadLocationException {
        if (offset < document.getLength() && !endsWithNewline(document, document.get(offset, 1))) {

            int lineLength = document.getLineInformationOfOffset(offset).getLength();
            int lineStart = document.getLineInformationOfOffset(offset).getOffset();
            String textAfter = document.get(offset, (lineStart + lineLength) - offset);

            if (textAfter.length() > 0
                && isWhitespace(textAfter)) {
                document.replace(offset, textAfter.length(), EMPTY_STRING);
            }
        }
    }


    private boolean isWhitespace(String s)
    {
        for (int i = s.length() - 1; i > -1 ; i--)
            if (!Character.isWhitespace(s.charAt(i)))
                return false;
        return true;
    }


    /**
     * True if text ends with a newline delimiter
     */
    public static boolean endsWithNewline(IDocument document, String text)
    {
        String[] newlines = document.getLegalLineDelimiters();
        boolean ends = false;
        for (int i = 0; i < newlines.length; i++) {
            String delimiter = newlines[i];
            if (text.indexOf(delimiter) != -1)
                ends = true;
        }
        return ends;
    }


    /**
     * Creates a string of spaces of the designated length.
     * @param width number of spaces you want to create a string of
     * @return the created string
     */
    public String createSpaceString(int width)
    {
        StringBuffer b = new StringBuffer(width);
        while (width-- > 0)
            b.append(' ');
        return b.toString();
    }
    
	public void propertyChange(PropertyChangeEvent event) 
	{
		IPreferenceStore prefs = EditorsUI.getPreferenceStore();
		
		String property = event.getProperty();
		
		if(property.equals(SoarEditorPreferencePage.CONVERT_TABS))
		{
			convertTabs = prefs.getBoolean(SoarEditorPreferencePage.CONVERT_TABS);
		}
		else if(property.equals(SoarEditorPreferencePage.TAB_WIDTH))
		{
			tabWidth = prefs.getInt(SoarEditorPreferencePage.TAB_WIDTH);
		}
		
	}
}
