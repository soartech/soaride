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
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;

import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarBufferChangedListener;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarOpenable;
import com.soartech.soar.ide.core.model.SoarModelException;

/**
 * Class that adapts an IDocument, as used by a text editor, to an ISoarBuffer
 * used by the Soar model framework. This allows an ISoarFile working copy to
 * use the editor's document directly as its buffer.
 * 
 * <p>This class is based heavily on org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter.
 * 
 * @author ray
 */
public class SoarBufferDocumentAdapter implements ISoarBuffer,
        IDocumentListener
{
    private ISoarFile ownerFile;
    private IDocument document;
    private List<ISoarBufferChangedListener> listeners = new ArrayList<ISoarBufferChangedListener>();

    private DocumentSetCommand setCommand = new DocumentSetCommand();
    private DocumentReplaceCommand replaceCommand = new DocumentReplaceCommand();
    /**
     * Construct a new buffer adapter for the given working copy
     * 
     * @param ownerFile The working copy Soar file that owns this buffer
     */
    public SoarBufferDocumentAdapter(ISoarFile ownerFile, IDocument document)
    {
        this.ownerFile = ownerFile;
        this.document = document;
        
        this.document.addPrenotifiedDocumentListener(this);
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#addListener(com.soartech.soar.ide.core.model.ISoarBufferChangedListener)
     */
    public void addListener(ISoarBufferChangedListener listener)
    {
        if(!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#close()
     */
    public void close()
    {
        if(isClosed())
        {
            return;
        }
        
        IDocument d = document;
        document = null;
        d.removePrenotifiedDocumentListener(this);
        
        for(ISoarBufferChangedListener listener : getListeners())
        {
            listener.onBufferClosed(this);
        }
        listeners.clear();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getCharacters()
     */
    public char[] getCharacters()
    {
        String content = getContents();
        return content != null ? content.toCharArray() : new char[0];
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getContents()
     */
    public String getContents()
    {
        return isClosed() ? "" : document.get();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getLength()
     */
    public int getLength()
    {
        return isClosed() ? 0 : document.getLength();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getOwner()
     */
    public ISoarOpenable getOwner()
    {
        return ownerFile;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getText(int, int)
     */
    public String getText(int offset, int length)
    {
        try
        {
            // With a working copy there is a brief time between a user edit 
            // and the next reconciling operation that the buffer/document 
            // will be out of synch with the model in which case, requests for
            // source code through source references could result in bad source
            // ranges. Since this window is relatively brief while the user is
            // typing, we just hide the error by fixing the offsets.
            int docLength = getLength();
            if(offset >= docLength)
            {
                return "";
            }
            int newLength = Math.min(length, docLength - offset);
            return isClosed() ? "" : document.get(offset, newLength);
        }
        catch (BadLocationException e)
        {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getUnderlyingResource()
     */
    public IResource getUnderlyingResource()
    {
        return ownerFile.getFile();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#isClosed()
     */
    public boolean isClosed()
    {
        return document == null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#removeListener(com.soartech.soar.ide.core.model.ISoarBufferChangedListener)
     */
    public void removeListener(ISoarBufferChangedListener listener)
    {
        listeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#replace(int, int, char[])
     */
    public void replace(int position, int length, char[] text)
    {
        replace(position, length, new String(text));
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#replace(int, int, java.lang.String)
     */
    public void replace(int position, int length, String text)
    {
        replaceCommand.replace(position, length, text);
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#save(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void save(IProgressMonitor monitor) throws SoarModelException
    {
        // TODO: What do we do here
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#setContents(char[])
     */
    public void setContents(char[] contents)
    {
        int oldLength = getLength();
        if(contents == null || contents.length == 0)
        {
            if(oldLength != 0)
            {
                setCommand.set("");
            }
        }
        else
        {
            setCommand.set(new String(contents));
        }

    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
     */
    public void documentAboutToBeChanged(DocumentEvent event)
    {
        // Nothing to do here
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
     */
    public void documentChanged(DocumentEvent event)
    {
        // TODO: Fire buffer changed event
        //System.out.println("Document changed: '" + event.getText() + "'");
    }

    /**
     *  Executes a document set content call in the UI thread.
     */
    protected class DocumentSetCommand implements Runnable {

        private String fContents;

        public void run() {
            if (!isClosed())
                document.set(fContents);
        }

        public void set(String contents) {
            fContents= contents;
            Display.getDefault().syncExec(this);
        }
    }

    /**
     * Executes a document replace call in the UI thread.
     */
    protected class DocumentReplaceCommand implements Runnable {

        private int fOffset;
        private int fLength;
        private String fText;

        public void run() {
            try {
                if (!isClosed())
                    document.replace(fOffset, fLength, fText);
            } catch (BadLocationException x) {
                // ignore
            }
        }

        public void replace(int offset, int length, String text) {
            fOffset= offset;
            fLength= length;
            fText= text;
            Display.getDefault().syncExec(this);
        }
    }
    
    private List<ISoarBufferChangedListener> getListeners()
    {
        return new ArrayList<ISoarBufferChangedListener>(listeners);
    }
    
//    private void fireChangedEvent(int offset, int length, String text)
//    {
//        SoarBufferChangedEvent e = new SoarBufferChangedEvent(this, offset, length, text);
//        
//        for(ISoarBufferChangedListener listener : getListeners())
//        {
//            listener.onBufferChanged(e);
//        }
//    }

}
