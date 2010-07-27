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
package com.soartech.soar.ide.ui;


import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarSourceReference;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.editors.database.SoarDatabaseOperatorEditor;
import com.soartech.soar.ide.ui.editors.database.SoarDatabaseTextEditor;
import com.soartech.soar.ide.ui.editors.datamap.SoarDatabaseDatamapEditor;

/**
 * @author aron
 *
 */
public class SoarUiModelTools 
{
	/**
     * Show a particular Soar element in an editor.
     * 
     * <p>Taken from 
     * <a href="http://wiki.eclipse.org/index.php/FAQ_How_do_I_open_an_editor_on_a_file_in_the_workspace%3F">here</a>
     * 
     * @param page The workbench page
     * @param element The Soar element to display
     * @return The editor the element was shown in, or null if the element 
     *      could not be displayed.
     * @throws CoreException
     */
    @SuppressWarnings("unchecked")
    public static IEditorPart showElementInEditor(IWorkbenchPage page, ISoarElement element) throws CoreException
    {
        // Find the resource that contains the element
        IResource resource = element.getContainingResource();
        if(resource == null)
        {
            return null;
        }
        
        // Now see if it's a file
        IFile file = (IFile) resource.getAdapter(IFile.class);
        if(file == null)
        {
            return null;
        }
        
        IEditorPart part = null;
        if(element instanceof ISoarSourceReference)
        {
            ISoarSourceRange range = ((ISoarSourceReference) element).getSourceRange();
            
            AbstractTextEditor editor = (AbstractTextEditor) IDE.openEditor(page, file);
			editor.selectAndReveal(range.getOffset(), 0 /* just highlight line, not all text */);
        }
        else
        {
            part = IDE.openEditor(page, file);
        }
        
        return part;
    }
    
	/**
     * Show a rule in an editor.
     * 
     * @param page The workbench page
     * @param row The rule to display
     * @return The editor the element was shown in, or null if the element could not be displayed.
     * @throws CoreException
     */
    public static IEditorPart showRuleInEditor(IWorkbenchPage page, SoarDatabaseRow row) throws CoreException
    {
    	assert row.getTable() == Table.RULES;
    	
        String editorId = SoarDatabaseTextEditor.ID;
        IEditorInput input = row.getEditorInput();
		if (input != null) {
			IEditorPart part = page.openEditor(input, editorId);
			return part;
		}
		return null;
    }
    
	/**
     * Show a problem space in an editor.
     * 
     * @param page The workbench page
     * @param row The rule to display
     * @return The editor the element was shown in, or null if the element could not be displayed.
     * @throws CoreException
     */
    public static IEditorPart showProblemSpaceInEditor(IWorkbenchPage page, SoarDatabaseRow row) throws CoreException
    {
    	assert row.getTable() == Table.PROBLEM_SPACES;
    	
        String editorId = SoarDatabaseDatamapEditor.ID;
        IEditorInput input = row.getEditorInput();
		if (input != null) {
			IEditorPart part = page.openEditor(input, editorId);
			return part;
		}
		return null;
    }
    
	public static IEditorPart showOperatorInEditor(IWorkbenchPage page, SoarDatabaseRow row) throws PartInitException {
    	assert row.getTable() == Table.OPERATORS;
    	
        String editorId = SoarDatabaseOperatorEditor.ID;
        IEditorInput input = row.getEditorInput();
		if (input != null) {
			IEditorPart part = page.openEditor(input, editorId);
			return part;
		}
		return null;
	}

	public static IEditorPart showAgentInEditor(IWorkbenchPage page, SoarDatabaseRow row) throws PartInitException {
    	assert row.getTable() == Table.AGENTS;
    	
        String editorId = SoarDatabaseTextEditor.ID;
        IEditorInput input = row.getEditorInput();
		if (input != null) {
			IEditorPart part = page.openEditor(input, editorId);
			return part;
		}
		return null;
	}
    
    /**
     * Find the active Soar editor.
     * 
     * @return The active Soar editor, or null if there is no active editor
     *      or if the acitve editor isn't a Soar editor.
     */
	/*
    public static SoarEditor getActiveSoarEditor()
    {
        IWorkbench wb = PlatformUI.getWorkbench();
        IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
        IWorkbenchPage page = win.getActivePage();
        IEditorPart editor = page.getActiveEditor();
        if(!(editor instanceof SoarEditor))
        {
            return null;
        }
        return (SoarEditor) editor;
    }
    */
    
    public static void closeAllEditors(boolean save) {
    	PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(save);
    }
    
    public static void closeEditorsForInput(IWorkbenchPage page, SoarDatabaseRow row, boolean save) {
    	IEditorInput rowInput = row.getEditorInput();
    	IEditorReference[] references = page.getEditorReferences();
    	for (IEditorReference reference : references) {
    		IEditorPart part = reference.getEditor(false);
    		if (part == null) continue;
    		IEditorInput partInput = part.getEditorInput();
    		if (rowInput.equals(partInput)) {
    			page.closeEditor(part, save);
    		}
    	}
    }

	public static SoarDatabaseRow selectAgent() {
		ArrayList<SoarDatabaseRow> agents = SoarCorePlugin.getDefault().getSoarModel().getDatabase().selectAllFromTable(Table.AGENTS);
		if (agents.size() == 0) {
			return null;
		}
		if (agents.size() == 1) {
			return agents.get(0);
		}
		
		ListDialog dialog = new ListDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		dialog.setContentProvider(new ArrayContentProvider());
		dialog.setLabelProvider(new LabelProvider());
		dialog.setInput(agents);
		dialog.open();
		Object[] result = dialog.getResult();
		if (result != null && result.length > 0) {
			return (SoarDatabaseRow) result[0];
		}
		return null;
	}
}
