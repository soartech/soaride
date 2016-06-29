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
package com.soartech.soar.ide.ui.editors.text.rules;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.editors.text.SoarRuleScanner;
import com.soartech.soar.ide.ui.editors.text.SoarSourceEditorConfiguration;
import com.soartech.soar.ide.ui.editors.text.SoarSourceViewerConfiguration;
//import com.soartech.soar.ide.ui.editors.text.SoarRuleScanner;
import com.soartech.soar.ide.ui.editors.text.SyntaxColorManager;

/**
 * <code>CommandRule</code> <code>WordRule</code> implementation for Soar
 * commands.
 *
 * TODO: Most of the keywords in this file should be dynamically generated
 * from the Tcl help model rather than hardcoding them.
 * 
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2009-06-22 13:05:30 -0400 (Mon, 22 Jun 2009) $
 */
public class CommandRule extends KeywordRule implements IPropertyChangeListener{
    
    // commands from the Soar8 Manual
    
    private static String[] BASIC_COMMANDS = new String[] {
            "excise", "help", "init-soar", "quit", "run", "sp", "stop-soar"  
    };
    
    private static String[] MEMORY_COMMANDS = new String[] {
            "default-wme-depth", "gds-print", "internal-symbols", "matches",
            "memories", "preferences", "print", "production-find"
    };
    
    private static String[] DEBUG_COMMANDS = new String[] {
            "chunk-name-format", "firing-counts", "fc", "pwatch", "stats",
            "warnings", "watch", "watch-wmes"
    };
    
    private static String[] RUNTIME_COMMANDS = new String[] {
            "attribute-preferences-mode", "epmem", "explain-backtraces",
            "indifferent-selection", "learn", "max-chunks",
            "max-elaborations", "max-nil-output-cycles",
            "multi-attributes", "numeric-indifferent-mode",
            "o-support-mode", "rl", "save-backtraces", "smem", "soar8", "svs", "timers",
            "waitsnc",
            "output-strings-destination"
    };
    
    private static String[] SYSTEM_COMMANDS = new String[] {
            "cd", "cli", "dirs", "log", "ls",
            "rete-net", "set-library-location"
    };
    
    private static String[] TCL_COMMANDS = new String[] {
    		"echo", "global", "proc", "pushd", "popd", "cd", "puts", 
            "set", "unset", "dict",
            "eval", "namespace", "array", "cd", "pwd", "variable",
            "catch", "info",
            "package",
            "if", "else", "return", "break", "switch", "while",
            "upvar", "uplevel", "for", "foreach", 
            "source",
            "expr", "incr", "string", 
            "lappend", "llength", "lindex", "linsert", "append",
            "open", "close", 
            "regsub",
            
            "wm", "winfo"
    };
    
    private static String[] IO_COMMANDS = new String[] {
            "add-wme", "remove-wme"
    };
    
    private static String[] MISC_COMMANDS = new String[] {
            "alias", "soarnews", "time", "version"
    };
    
    //Array of strings for commands added through preferences.
    
    private static String[] CUSTOM_COMMANDS = SoarEditorUIPlugin.getDefault().getKeywordsPreference();
    //Alternate way to retrieve keywords store. Left as comment because may come in useful in the future.
    //            Platform.getPreferencesService()           
    //            .getString(SoarEditorUIPlugin.PLUGIN_ID, SoarEditorUIPlugin.KEYWORDS_PREFERENCE, SoarEditorUIPlugin.DEFAULT_CMDS, null)
    //            .split(SoarEditorUIPlugin.PREFERENCE_DELIMITER);
    
    public static String[][] ALL_COMMANDS = new String[][] {
            BASIC_COMMANDS, MEMORY_COMMANDS, DEBUG_COMMANDS, RUNTIME_COMMANDS, 
            SYSTEM_COMMANDS, TCL_COMMANDS, IO_COMMANDS, MISC_COMMANDS, 
            CUSTOM_COMMANDS  
    };
    
    private SoarSourceViewerConfiguration config;

    /**
     * Constructor for a <code>CommandRule</code> object.
     */
    public CommandRule(SoarSourceViewerConfiguration config) {

        super( new CommandDetector() );
        
        this.config = config; 
        
        SoarEditorUIPlugin
        .getDefault()
        .getPreferenceStore()
        .addPropertyChangeListener(this);
    }

    @Override
    protected Color getFgColor() {

        return SyntaxColorManager.getCommandColor();
    }

    @Override
    protected Color getBgColor() {
        
        return null;
    }

    @Override
    protected int getProperty() {
    
        return SWT.BOLD;
    }
    

    @Override
    protected String[][] getAllKeywords() {
        return ALL_COMMANDS;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        //This block is triggered whenever "ok" is clicked from the preferences window
        System.out.println("PROPERTY CHANGE");
        //Set the arrays of custom commands to the keywords store.
        CUSTOM_COMMANDS = SoarEditorUIPlugin.getDefault().getKeywordsPreference();
        
        //Makes scrollbar appear somehow        
        //config.reinitializeScanner();
        
//        for (String cmd: CUSTOM_COMMANDS)
//        {
//            System.out.println(cmd);
//        }
    }
}
