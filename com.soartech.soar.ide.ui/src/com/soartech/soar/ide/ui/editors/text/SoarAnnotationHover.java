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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

/**
 * Provides the hover tooltip text for an annotation.
 * 
 * Implementation taken from:
 * org.eclipse.pde.internal.ui.editor.text.AnnotationHover
 * 
 * Register in the SourceViewerConfiguration to make it work.
 * 
 * @author aron@soartech.com
 */
public class SoarAnnotationHover implements IAnnotationHover
{
    private static final String separator = System.getProperty("line.separator");
    
    public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber)
    {
        String[] messages = getMessagesForLine(sourceViewer, lineNumber);

        if (messages.length == 0)
            return null;
        
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < messages.length; i++) {
            buffer.append(messages[i]);
            if (i < messages.length - 1)
            {
                buffer.append(separator); //$NON-NLS-1$
            }
        }
        return buffer.toString(); 
    }
    
    private String[] getMessagesForLine(ISourceViewer viewer, int line) 
    {
        IDocument document = viewer.getDocument();
        IAnnotationModel model = viewer.getAnnotationModel();

        if (model == null)
            return new String[0];

        ArrayList<String> messages = new ArrayList<String>();
        Map<Position, Serializable> messagesAtPosition = new HashMap<Position, Serializable>();
        
        Iterator<?> iter = model.getAnnotationIterator();
        while (iter.hasNext()) {
            Object object = iter.next();
            
            Position position = model.getPosition((Annotation) object);
            if(position == null || !compareRulerLine(position, document, line))
            {
                continue;
            }
            
            String message = null;
            if (object instanceof MarkerAnnotation) 
            {
                MarkerAnnotation annotation = (MarkerAnnotation) object;
                IMarker marker = annotation.getMarker();
                
                message = marker.getAttribute(IMarker.MESSAGE, (String) null);
            }
            else if(object instanceof SoarEditorAnnotation) 
            {
                SoarEditorAnnotation annotation = (SoarEditorAnnotation) object;
                message = annotation.getText();
            }
            
            if(message != null)
            {
                message = message.trim();
                if(message.length() > 0 &&
                   !isDuplicateMessage(messagesAtPosition, position, message))
                {
                    messages.add(message);
                }
            }
        }
        
        return (String[]) messages.toArray(new String[messages.size()]);
    }

    private boolean compareRulerLine(Position position, IDocument document, int line) 
    {

        try {
            if (position.getOffset() > -1 && position.getLength() > -1) {
                final int lineOfOffset = document.getLineOfOffset(position.getOffset());
                return lineOfOffset == line;
            }
        } catch (BadLocationException e) {
        }
        return false;
    } 

    @SuppressWarnings("unchecked")
    private boolean isDuplicateMessage(Map<Position, Serializable> messagesAtPosition, Position position, String message) {
        if (message == null)
            return false;
        
        if (messagesAtPosition.containsKey(position)) {
            Object value= messagesAtPosition.get(position);
            if (message == null || message.equals(value))
                return true;

            if (value instanceof List) {
                List<String> messages= (List<String>)value;
                if  (messages.contains(message))
                    return true;
                messages.add(message);
            } else {
                ArrayList<Object> messages= new ArrayList<Object>();
                messages.add(value);
                messages.add(message);
                messagesAtPosition.put(position, messages);
            }
        } else
            messagesAtPosition.put(position, message);
        return false;
    }

}
