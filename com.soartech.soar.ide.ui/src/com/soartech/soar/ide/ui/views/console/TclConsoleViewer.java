/**
 * 
 */
package com.soartech.soar.ide.ui.views.console;


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
import org.eclipse.ui.console.TextConsoleViewer;

import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelException;

/**
 * @author aron
 *
 */
public class TclConsoleViewer extends TextConsoleViewer {

    //This class gives you access to setting the caret position
    //by getting the styled text widget and then using setCaretOffset
    
    final static String NEWLINE = System.getProperty("line.separator");
    
    /**
     * will always scroll with output if value is true.
     */
    private boolean fAutoScroll = true;

    private IDocumentListener fDocumentListener;
    
    TclConsole tclConsole;
    
    public TclConsoleViewer(Composite parent, TclConsole console) {
        super(parent, console);
        
        this.tclConsole = console;
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
    protected void handleVerifyEvent(VerifyEvent verifyEvent) {
        IDocument doc = getDocument();
        String[] legalLineDelimiters = doc.getLegalLineDelimiters();
        String eventString = verifyEvent.text;
        boolean isCarriageReturn = false;
        try {
            IConsoleDocumentPartitioner partitioner = (IConsoleDocumentPartitioner) doc.getDocumentPartitioner();
            if (!partitioner.isReadOnly(verifyEvent.start)) {
                for (int i = 0; i < legalLineDelimiters.length; i++) {
                    if (verifyEvent.text.equals(legalLineDelimiters[i])) {
                        isCarriageReturn = true;
                        break;
                    }
                }

                if (!isCarriageReturn) {
                    super.handleVerifyEvent(verifyEvent);
                    return;
                }
                else //read the input into the system
                {
                    ITypedRegion region = partitioner.getPartition(verifyEvent.start);
                    int length = region.getLength();
                    int offset = region.getOffset();
                    
                    try {
                        String inputString = doc.get(offset, length);
                        
                        //get the first agent from the current project and execute the input string in its interpreter
                        ISoarProject project = this.tclConsole.getSoarProject();
                        if(project.getAgents().size() > 0) 
                        {
                            try {
                                String result = project.getAgents().get(0).executeString(inputString);
                                result = NEWLINE + result + NEWLINE;
                                doc.replace(doc.getLength(), 0, result);
                            } catch (BadLocationException e) {
                            }
                        }
                        
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    } catch (SoarModelException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            int length = doc.getLength();
            if (verifyEvent.start == length) {
                super.handleVerifyEvent(verifyEvent);
            } else {
                try {
                    doc.replace(length, 0, eventString);
                } catch (BadLocationException e1) {
                }
                verifyEvent.doit = false;
            }
        } finally {
            StyledText text = (StyledText) verifyEvent.widget;
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
