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
package com.soartech.soar.ide.console;

import org.eclipse.swt.events.KeyEvent;

/**
 * <code>ICommandConsoleModelListener</code> TODO: what is the purpose
 * @author annmarie.steichmann
 *
 */
public interface ICommandConsoleModelListener {
	
	/**
	 * Prevent multiple consoles from duplicating work.
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @param doProcess true if processing should be allowed
	 */
	public void allowProcessing( String processId, String agentName, boolean doProcess ); 
	
	/**
	 * Notify listener that output was logged externally by the given
	 * agent for the given process.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @param message The message that was logged
	 */
	public void notifyLog( String processId, String agentName, String message );

	/**
	 * Notify listener that output was appended internally by
	 * a command console.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @param text The text that was appended
	 */
	public void notifyAppend( String processId, String agentName, String text );
	
	/**
	 * Notify listener that a key event occured.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @param e The key event
	 */
	public void notifyKeyEvent( String processId, String agentName, KeyEvent e );
	
	/**
	 * Notify listener that a character key was entered. (Mimics a user typing.)
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @param keyChar The character to process
	 */
	public void notifyKeyEvent( String processId, String agentName, char keyChar );
	
	/**
	 * Notify listener that the output from the given agent
	 * for the given process should be cleared.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 */
	public void notifyClear( String processId, String agentName );
	
	/**
	 * Notify listener that the caret should be moved to the
	 * end of the text.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName the name of the agent
	 */
	public void notifyMoveCaretToEnd( String processId, String agentName );
}
