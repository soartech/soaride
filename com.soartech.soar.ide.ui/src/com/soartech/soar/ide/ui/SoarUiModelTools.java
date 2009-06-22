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


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarSourceReference;
import com.soartech.soar.ide.ui.editors.text.SoarEditor;

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
     * Find the active Soar editor.
     * 
     * @return The active Soar editor, or null if there is no active editor
     *      or if the acitve editor isn't a Soar editor.
     */
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
}
