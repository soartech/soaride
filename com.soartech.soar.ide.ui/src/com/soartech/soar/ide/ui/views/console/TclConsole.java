/**
 * 
 */
package com.soartech.soar.ide.ui.views.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IFlushableStreamMonitor;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.IConsoleColorProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.graphics.Color;
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
public class TclConsole extends IOConsole {

//    private IProcess process = null;
//    private IStreamsProxy fStreamsProxy;
//    private ConsoleHistory history;
    
    private List fStreamListeners = new ArrayList();
    
//    private IConsoleColorProvider fColorProvider;
    
    IOConsoleInputStream inputStream;
    IOConsoleOutputStream outputStream;
    
    ISoarProject soarProject;
    
    private static final String ENTER_KEY = "\r\n";
    
    //This is to remember the caret position after the prompt 
//    private int caretAtPrompt;
    
    public TclConsole(String name, ISoarProject soarProject) 
    {
        super(name, null);
        
        this.soarProject = soarProject;
        
        inputStream = getInputStream();
        outputStream = newOutputStream();
        
//        fStreamsProxy = process.getStreamsProxy();
        
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


    /**
     * This class listens to a specified IO stream
     */
    private class StreamListener implements IStreamListener {

        private IOConsoleOutputStream fStream;

        private IStreamMonitor fStreamMonitor;

        private String fStreamId;

        private boolean fFlushed = false;

        private boolean fListenerRemoved = false;

        public StreamListener(String streamIdentifier, IStreamMonitor monitor, IOConsoleOutputStream stream) {
            this.fStreamId = streamIdentifier;
            this.fStreamMonitor = monitor;
            this.fStream = stream;
            fStreamMonitor.addListener(this);  
            //fix to bug 121454. Ensure that output to fast processes is processed.
            streamAppended(null, monitor);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.debug.core.IStreamListener#streamAppended(java.lang.String,
         *      org.eclipse.debug.core.model.IStreamMonitor)
         */
        public void streamAppended(String text, IStreamMonitor monitor) {
            String encoding = getEncoding();
            if (fFlushed) {
                try {
                    if (fStream != null) {
                        if (encoding == null)
                            fStream.write(text);
                        else 
                            fStream.write(text.getBytes(encoding));
                    }
//                    if (fFileOutputStream != null) {
//                        synchronized (fFileOutputStream) {
//                            if (encoding == null)
//                                fFileOutputStream.write(text.getBytes());
//                            else 
//                                fFileOutputStream.write(text.getBytes(encoding));
//                        }
//                    }
                } catch (IOException e) {
                    DebugUIPlugin.log(e);
                }
            } else {
                String contents = null;
                synchronized (fStreamMonitor) {
                    fFlushed = true;
                    contents = fStreamMonitor.getContents();
                    if (fStreamMonitor instanceof IFlushableStreamMonitor) {
                        IFlushableStreamMonitor m = (IFlushableStreamMonitor) fStreamMonitor;
                        m.flushContents();
                        m.setBuffered(false);
                    }
                }
                try {
                    if (contents != null && contents.length() > 0) {
                        if (fStream != null) {
                            fStream.write(contents);
                        }
//                        if (fFileOutputStream != null) {
//                            synchronized (fFileOutputStream) {
//                                fFileOutputStream.write(contents.getBytes());
//                            }
//                        }
                    }
                } catch (IOException e) {
                    DebugUIPlugin.log(e);
                }
            }
        }

        public void closeStream() {
            if (fStreamMonitor == null) {
                return;
            }
            synchronized (fStreamMonitor) {
                fStreamMonitor.removeListener(this);
                if (!fFlushed) {
                    String contents = fStreamMonitor.getContents();
                    streamAppended(contents, fStreamMonitor);
                }
                fListenerRemoved = true;
                try {
                    if (fStream != null) {
                        fStream.close();
                    }
                } catch (IOException e) {
                }
            }
        }

        public void dispose() {
            if (!fListenerRemoved) {
                closeStream();
            }
            fStream = null;
            fStreamMonitor = null;
            fStreamId = null;
        }
    }
    
    private class InputReadJob extends Job {

        private IStreamsProxy streamsProxy;

        InputReadJob(IStreamsProxy streamsProxy) {
            super("Process Console Input Job"); //$NON-NLS-1$
            this.streamsProxy = streamsProxy;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        protected IStatus run(IProgressMonitor monitor) {
            try {
                byte[] b = new byte[1024];
                int read = 0;
                while (inputStream != null && read >= 0) {
                    read = inputStream.read(b);
                    if (read > 0) {
                        String s = new String(b, 0, read);
                        streamsProxy.write(s);
                    }
                }
            } catch (IOException e) {
                DebugUIPlugin.log(e);
            }
            return Status.OK_STATUS;
        }
    }
    

    /**
     * @see org.eclipse.debug.ui.console.IConsole#connect(org.eclipse.debug.core.model.IStreamsProxy)
     */
    public void connect(IStreamsProxy streamsProxy) {
        IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
        IStreamMonitor streamMonitor = streamsProxy.getErrorStreamMonitor();
        if (streamMonitor != null) {
            connect(streamMonitor, IDebugUIConstants.ID_STANDARD_ERROR_STREAM,
                    store.getBoolean(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR));
        }
        streamMonitor = streamsProxy.getOutputStreamMonitor();
        if (streamMonitor != null) {
            connect(streamMonitor, IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM, 
                    store.getBoolean(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT));
        }
        InputReadJob readJob = new InputReadJob(streamsProxy);
        readJob.setSystem(true);
        readJob.schedule();
    }
    
    /**
     * @see org.eclipse.debug.ui.console.IConsole#connect(org.eclipse.debug.core.model.IStreamMonitor, java.lang.String)
     */
    public void connect(IStreamMonitor streamMonitor, String streamIdentifier) {
        connect(streamMonitor, streamIdentifier, false);
    }
    
    /**
     * Connects the given stream monitor to a new output stream with the given identifier.
     * 
     * @param streamMonitor stream monitor
     * @param streamIdentifier stream identifier
     * @param activateOnWrite whether the stream should displayed when written to 
     */
    private void connect(IStreamMonitor streamMonitor, String streamIdentifier, boolean activateOnWrite) {
        IOConsoleOutputStream stream = null;
//        if (fAllocateConsole) {
            stream = newOutputStream();
//            Color color = fColorProvider.getColor(streamIdentifier);
//            stream.setColor(color);
            stream.setActivateOnWrite(activateOnWrite);
//        }
        synchronized (streamMonitor) {
            StreamListener listener = new StreamListener(streamIdentifier, streamMonitor, stream);
            fStreamListeners.add(listener);
        }
    }

}


