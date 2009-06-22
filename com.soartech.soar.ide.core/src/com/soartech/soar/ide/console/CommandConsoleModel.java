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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

/**
 * <code>CommandConsoleModel</code> handles the common functionality between
 * <code>CommandConsole</code> objects.
 * 
 * @author annmarie.steichmann
 *
 */
public class CommandConsoleModel {
	
	private Map<String,String> lastCommands =
		new HashMap<String,String>();
	
	private Map<String,StringBuffer> agentBuffers = 
		new HashMap<String,StringBuffer>();
	
	private Map<String,StringBuffer> commandBuffers =
		new HashMap<String,StringBuffer>();

	private List<ICommandConsoleModelListener> listeners = 
						new ArrayList<ICommandConsoleModelListener>();
	
	private static CommandConsoleModel instance = null;
	
	/**
	 * @return The singleton instance of the model
	 */
	public static CommandConsoleModel getInstance() {
		
		if ( instance == null ) {
			
			instance = new CommandConsoleModel();
		}
		
		return instance;
	}
	
	/**
	 * Add a model listener that will be notified when the text
	 * to be displayed is changed.
	 * 
	 * @param listener The model listener
	 */
	public void addListener( ICommandConsoleModelListener listener ) {
		if(listeners.contains(listener))
        {
		    return;
        }
		listeners.add( listener );
	}
	
	/**
	 * Remove the model listener so that it will no longer be notified
	 * when the text to be displayed is changed.
	 * 
	 * @param listener The model listener
	 */
	public void removeListener( ICommandConsoleModelListener listener ) {
		
		listeners.remove( listener );
	}
	
	private String getKey( String processId, String agentName ) {
		
		return processId + ":" + agentName;
	}
	
	private StringBuffer getBuffer( String processId, String agentName ) {
		
		String key = getKey( processId, agentName );
		
		StringBuffer agentBuffer = agentBuffers.get( key );
		if ( agentBuffer == null ) {
			agentBuffer = new StringBuffer();
			agentBuffers.put( key, agentBuffer );
		}
		
		return agentBuffer;
	}
	
	private void buffer( String processId, String agentName, String text ) {
		
		StringBuffer agentBuffer = getBuffer( processId, agentName );
		agentBuffer.append( text );		
	}
	
	/**
	 * Append the given text to the buffer for the given agent of the
	 * given process.
	 *  
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @param text The text to append
	 */
	public void append( String processId, String agentName, String text ) {
		
		buffer( processId, agentName, text );
		
		for ( ICommandConsoleModelListener listener : listeners ) {
			
			listener.notifyAppend( processId, agentName, text );
		}
	}
	
	/**
	 * Clear out the buffered output from the given agent for the given
	 * process.
	 *  
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 */
	public void clear( String processId, String agentName ) {
		
		String key = getKey( processId, agentName );
		agentBuffers.remove( key );
		
		for ( ICommandConsoleModelListener listener : listeners ) {
			
			listener.notifyClear( processId, agentName );
		}
	}
	
	/**
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @return The text that is in the buffer for the given agent of the
	 * given process
	 */
	public String getText( String processId, String agentName ) {
		
		StringBuffer agentBuffer = getBuffer( processId, agentName );
		return agentBuffer.toString();
	}
	
	/**
	 * Process the given key event.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @param e The key event to process
	 */
	public void processKey( String processId, String agentName, KeyEvent e ) {
		
		boolean doProcess = true;
		String lastCommand = getCommand(processId, agentName);
		lastCommands.put( getKey( processId, agentName ), lastCommand );
		for ( ICommandConsoleModelListener listener : listeners ) {
			
			listener.allowProcessing( processId, agentName, doProcess );
			if ( doProcess ) {
				doProcess = false;
			}
			
			listener.notifyKeyEvent( processId, agentName, e );
		}
	}

	/**
	 * Process the given command.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @param command The command to process
	 */
	public void processCommand(String processId, String agentName, String command) {

		int commandLen = command.length();
		char[] chars = new char[commandLen+1];
		chars[commandLen] = SWT.CR;
		command.getChars( 0, commandLen, chars, 0);

		boolean doProcess = false;
		
		for ( char c : chars ) {
			
			doProcess = true;
			String lastCommand = getCommand(processId, agentName);
			lastCommands.put( getKey( processId, agentName ), lastCommand );
			
			for ( ICommandConsoleModelListener listener : listeners ) {
				
				listener.allowProcessing( processId, agentName, doProcess );
				if ( doProcess ) {
					doProcess = false;
				}
				
				listener.notifyKeyEvent( processId, agentName, c );
			}
		}
	}	
	
	/**
	 * Return a static snapshot of the command.  The command
	 * buffer itself may be modified by consoles.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 */
	public String getLastCommand( String processId, String agentName ) {
		
		return lastCommands.get( getKey( processId, agentName ) );
	}
	
	/**
	 * Delete the static snapshot of the command.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 */
	public void resetLastCommand( String processId, String agentName ) {
		
		lastCommands.remove( getKey( processId, agentName ) );
	}
	
	/**
	 * Log the given message.
	 * 
	 * @param kernelId The unique kernel identifier
	 * @param agentName The name of the agent
	 * @param message The message to log
	 */
	public void log( String kernelId, String agentName, String message ) {
	
		buffer( kernelId, agentName, message );
		
		for ( ICommandConsoleModelListener listener : listeners ) {
			
			listener.notifyLog( kernelId, agentName, message );
		}
	}
	
	/**
	 * Move the caret to the end of the text.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 */
	public void moveCaretToEnd( String processId, String agentName ) {
		
		for ( ICommandConsoleModelListener listener : listeners ) {
			
			listener.notifyMoveCaretToEnd( processId, agentName );
		}
	}
	
	/**
	 * Clean up resources.
	 */
	public static void destroy() {
		
		if ( instance != null ) {
			
			instance.agentBuffers.clear();
			instance.commandBuffers.clear();
			instance.lastCommands.clear();
			instance.listeners.clear();
			instance = null;
		}
	}
	

	private StringBuffer getCommandBuffer( String processId, String agentName ) {
		
		String key = getKey( processId, agentName );
		
		StringBuffer buffer = commandBuffers.get( key );
		if ( buffer == null ) {
			buffer = new StringBuffer();
			commandBuffers.put( key, buffer );
		}
		
		return buffer;
	}

	/**
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @return The text entered before an enter key was pressed.
	 */
	public String getCommand( String processId, String agentName ) {
		
		StringBuffer commandBuffer = getCommandBuffer( processId, agentName );
		if ( commandBuffer != null ) return commandBuffer.toString();
		return null;
	}

	/**
	 * Clear out the command buffer for the given agent attached to the
	 * given process.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 */
	public void resetCommandBuffer( String processId, String agentName ) {
		
		String key = getKey( processId, agentName );
		commandBuffers.remove( key );
	}
	
	/**
	 * Append the given command to the command buffer.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @param command The command text to append
	 */
	public void appendToCommandBuffer( String processId, String agentName, String command ) {
		
		StringBuffer commandBuffer = getCommandBuffer( processId, agentName );
		commandBuffer.append( command );
	}
	
	/**
	 * Delete the given character at the given index from the command
	 * buffer.
	 * 
	 * @param processId The unique process identifier
	 * @param agentName The name of the agent
	 * @param offset The location index of where to delete the character
	 */
	public void deleteCharInCommandBuffer( String processId, String agentName, int offset ) {
		
		StringBuffer commandBuffer = getCommandBuffer( processId, agentName );
		commandBuffer.deleteCharAt( offset );
	}
	
	/**
	 * Insert the given character into the command buffer at the given
	 * offset.
	 * 
	 * @param processId
	 * @param agentName
	 * @param offset
	 * @param c
	 */
	public void insertCharInCommandBuffer( String processId, String agentName, 
			int offset, char c ) {
		
		StringBuffer commandBuffer = getCommandBuffer( processId, agentName );
		commandBuffer.insert( offset, c );
	}
}
