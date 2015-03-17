/**
 * 
 */
package com.soartech.soar.ide.ui.views.console;

import java.io.IOException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.console.TextConsoleViewer;

/**
 * @author aron
 *
 */
public class TestConsoleViewer extends TextConsoleViewer {

    //This class gives you access to setting the caret position
    //by getting the styled text widget and then using setCaretOffset
    
    /**
     * will always scroll with output if value is true.
     */
    private boolean fAutoScroll = true;

    private IDocumentListener fDocumentListener;
    
    TestIOConsole ioConsole;
    
    public TestConsoleViewer(Composite parent, TestIOConsole console) {
        super(parent, console);
        
        this.ioConsole = console;
    }

    public boolean isAutoScroll() {
        return fAutoScroll;
    }

    public void setAutoScroll(boolean scroll) {
        fAutoScroll = scroll;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.TextViewer#handleVerifyEvent(org.eclipse.swt.events.VerifyEvent)
     */
    @SuppressWarnings("unused")
    protected void handleVerifyEvent(VerifyEvent e) {
        IDocument doc = getDocument();
        String[] legalLineDelimiters = doc.getLegalLineDelimiters();
        String eventString = e.text;
        boolean isCarriageReturn = false;
        try {
            IConsoleDocumentPartitioner partitioner = (IConsoleDocumentPartitioner) doc.getDocumentPartitioner();
            if (!partitioner.isReadOnly(e.start)) {
                for (int i = 0; i < legalLineDelimiters.length; i++) {
                    if (e.text.equals(legalLineDelimiters[i])) {
                        isCarriageReturn = true;
                        break;
                    }
                }

                if (!isCarriageReturn) {
                    super.handleVerifyEvent(e);
                    return;
                }
                else //read the input into the system
                {
                    ITypedRegion region = partitioner.getPartition(e.start);
                    int length = region.getLength();
                    int offset = region.getOffset();
                    
                    try {
                        String inputString = doc.get(offset, length);
                        
                        //execute the input string
                        
                    } catch (BadLocationException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            
            int length = doc.getLength();
            if (e.start == length) {
                super.handleVerifyEvent(e);
            } else {
                try {
                    doc.replace(length, 0, eventString);
                } catch (BadLocationException e1) {
                }
                e.doit = false;
            }
        } finally {
            StyledText text = (StyledText) e.widget;
            text.setCaretOffset(text.getCharCount());
//            getTextWidget().setCaretOffset(getDocument().getLength());
        }
    }

    /**
     * makes the associated text widget uneditable.
     */
    public void setReadOnly() {
        ConsolePlugin.getStandardDisplay().asyncExec(new Runnable() {
            public void run() {
                StyledText text = getTextWidget();
                if (text != null && !text.isDisposed()) {
                    text.setEditable(false);
                }
            }
        });
    }

    /**
     * @return <code>false</code> if text is editable
     */
    public boolean isReadOnly() {
        return !getTextWidget().getEditable();
    }
   
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.ITextViewer#setDocument(org.eclipse.jface.text.IDocument)
     */
    public void setDocument(IDocument document) {
        IDocument oldDocument= getDocument();
        
        super.setDocument(document);
        
        if (oldDocument != null) {
            oldDocument.removeDocumentListener(getDocumentListener());
        }
        if (document != null) {
            document.addDocumentListener(getDocumentListener());
        }
    }
    
    private IDocumentListener getDocumentListener() {
        if (fDocumentListener == null) {
            fDocumentListener= new IDocumentListener() {
                public void documentAboutToBeChanged(DocumentEvent event) {
                }

                public void documentChanged(DocumentEvent event) {
                    if (fAutoScroll) {
                        revealEndOfDocument();
                    }
                }
            };
        }
        return fDocumentListener;
    }
}
