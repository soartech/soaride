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
package com.soartech.soar.ide.core.model.impl;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.RelocatableTclInterpreter;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
public class SoarModelTclCommands
{
    /**
     * List of commands that are treated as no-ops by the IDE.
     * TODO: This list should be configurable on a per-project basis. 
     */
    public static final String[] NOTHING_COMMANDS = 
    {
        "echo",
        "learn",
        "waitsnc",
        "watch",
        "multi-attributes",
        "multi-attribute",
        "o-support-mode",
        "output-strings-destination",
        "help",
        "init-soar",
        "quit",
        "run",
        "stop-soar",
        "default-wme-depth",
        "gds-print",
        "internal-symbols",
        "matches",
        "memories",
        "preferences",
        "print",
        "production-find",
        "chunk-name",
        "firing-counts", "fc",
        "pwatch",
        "explain-backtraces",
        "indifferent-selection",
        "max-chunks",
        "max-elaborations",
        "max-nil-output-cycles",
        "multi-attributes",
        "numeric-indifferent-mode",
        "o-support-mode",
        "save-backtraces",
        "soar8",
        "timers",
        "dirs",
        "log",
        "rete-net",
        "set-library-location",
        "add-wme",
        "remove-wme",
        "soarnews",
        "version",
        "stats",
        
        "wm",
        
        "smem",
        "alias",
        "rl",
        "epmem",
        "dict"
    };
    
    /**
     * Implementation of the null command, i.e. the command that does nothing.
     */
    public static final Command NULL_COMMAND = new Command() {
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            // Just return the name of this command. This is nice  if a null 
            // command is supposed to be returning a command to be used later 
            // (like Tk widget names).
            interp.setResult(args[0]);
        }
    };
    
    private static final Command WINFO_NULL_COMMAND = new Command() {
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            interp.setResult("");
        }
    };
    
    /**
     * Installs the null commands in the given interpreter
     * 
     * @param interp The interpreter
     */
    public static void installNullCommands(RelocatableTclInterpreter interp)
    {
        for(String s : NOTHING_COMMANDS)
        {
            interp.createCommand(s, NULL_COMMAND);
        }
        interp.createCommand("winfo", WINFO_NULL_COMMAND);
    }
   
}
