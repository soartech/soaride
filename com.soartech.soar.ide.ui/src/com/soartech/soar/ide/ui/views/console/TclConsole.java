/**
 * 
 */
package com.soartech.soar.ide.ui.views.console;

import java.io.IOException;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.part.IPageBookViewPage;

import com.soartech.soar.ide.core.model.ISoarProject;

/**
 * @author aron
 *
 */
public class TclConsole extends IOConsole 
{
    
    IOConsoleInputStream inputStream;
    IOConsoleOutputStream outputStream;
    
    ISoarProject soarProject;
    
    private static final String ENTER_KEY = "\r\n";
    
    //This is to remember the caret position after the prompt 
//    private int caretAtPrompt;
    
    public TclConsole(String name, ISoarProject soarProject) 
    {
        super("TCL> " + name, null);
        
        this.soarProject = soarProject;
        
        inputStream = getInputStream();
        outputStream = newOutputStream();
        
//        outputStream.setColor(Display.getCurrent().getSystemColor(
//            SWT.COLOR_BLUE));
//        inputStream.setColor(Display.getCurrent().getSystemColor(
//            SWT.COLOR_RED));
        
        getDocument().addDocumentListener(new IDocumentListener() {

            @Override
            public void documentChanged(DocumentEvent event) {
                if (ENTER_KEY.equals(event.getText())) {
                    // Print the Prompt
                    writePromptToConsole();
                    //update the caret ?
                    
                }
            }

            @Override
            public void documentAboutToBeChanged(DocumentEvent event) {
                
            }
        });
    }
    
    @Override
    public IPageBookViewPage createPage(IConsoleView view) {
        return new TclConsolePage(this, view);
    }
    
    public class TclConsolePage extends TextConsolePage
    {
           public TclConsolePage(TextConsole console, IConsoleView view) {
            super(console, view);
        }

        @Override
        protected TextConsoleViewer createViewer(Composite parent) {
            return new TclConsoleViewer(parent, (TclConsole) this.getConsole());
        }
    }
    
    public ISoarProject getSoarProject() 
    {
        return soarProject;
    }

    public void writeToConsole(String msg) {
        outputStream.setActivateOnWrite(true);
        try {
            outputStream.write(msg);
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
    
    public void writePromptToConsole() {
        writeToConsole("TCL> ");
    }

}


