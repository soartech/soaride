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
package com.soartech.soar.ide.ui.actions;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarSourceReference;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.editors.text.SoarEditor;
import com.soartech.soar.ide.ui.editors.text.autoedit.TabPrefs;


/**
 * Action delegate used to duplicate a production.
 * 
 * @author sfurtwangler
 *
 */
public class DuplicateProductionActionDelegate implements IEditorActionDelegate 
{
    private ISoarSourceReference reference;
    private TabPrefs prefs;
    
    public TabPrefs getIndentPrefs()
    {
        if (this.prefs == null)
        {
            this.prefs = new TabPrefs();
        }
        return this.prefs;
    }
    
    private SoarEditor editor;
    private ISelection selection;
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) 
    {
        if(targetEditor instanceof SoarEditor)
        {
            this.editor = (SoarEditor) targetEditor;
        }
        else
        {
            this.editor = null;
        }
       	action.setEnabled(getCurrentProduction()!=null);
    }
    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) 
    {
        if(editor == null)
        {
            return;
        }
            
        ISoarProduction p = getCurrentProduction();
        if(p!=null)
        {
        	String oldName = p.getProductionName();
        	String newName = "copy*of*"+oldName;        	
	        try
	        {
	        	//check to make sure we can use this name
	        	ISoarFileAgentProxy proxy = (ISoarFileAgentProxy)p.getParent();
        		List<ISoarProduction> prods = proxy.getProductions();
        		boolean found=true;
        		int copies=1;
        		while(found)
        		{
        			found=false;
        			for(ISoarProduction p2 : prods)
        			{
        				String testName = p2.getProductionName();
        				if(newName.equalsIgnoreCase(testName))
        				{
        					found=true;
    	        			copies++;
    	        			break;
        				}
        			}
	        		if(copies>1)
	        		{
	        			newName="copy*"+String.valueOf(copies)+"*of*"+oldName;
	        		}
        		}
	        	String source = reference.getSource();
	        	String newSource = source.replace(oldName, newName);
	        	
	        	//Append on a bare-bones comment to say where it came from
	        	newSource = "\n\n##!\n# Copy of "+oldName+"\n" + newSource;
	        	int offset = reference.getSourceRange().getOffset();
	        	
	        	//Insert the new production right after the current one.
	        	try
	        	{
	        		editor.getDocument().replace(offset+source.length(), 0, newSource);
	        	}
	        	catch(BadLocationException e)
	        	{
	        		SoarEditorUIPlugin.log(e);
	        	}
	        }
	        catch(SoarModelException e)
	        {
	        	SoarEditorUIPlugin.log(e);
	        }
        }
    }
    private ISoarProduction getCurrentProduction()
    {
        if(this.editor == null)
        {
            return null;
        }
        
    	ISoarProduction prod = null;
    	Object o = null;
        //get the soar-reference from the editor
        if(selection instanceof IStructuredSelection)
        {
        	//this never happens?
            IStructuredSelection ss = (IStructuredSelection) selection;
            o = !selection.isEmpty() ? ss.getFirstElement() : null;
        }
        else if(selection instanceof ITextSelection)
        {
        	ITextSelection ts = (ITextSelection) selection;
            o = ((SoarEditor) editor).getProductionWithinOffsets(ts.getOffset(),ts.getOffset()+ts.getLength());
        }
        else if(editor instanceof SoarEditor)
        {
            o = ((SoarEditor) editor).getElementAtCaretOffset();
        }
        
        reference=null;
        if(o!=null && (o instanceof IAdaptable))
        	reference = (ISoarSourceReference) ((IAdaptable) o).getAdapter(ISoarSourceReference.class);

        //make sure its a production
        if(reference!=null && reference instanceof ISoarProduction)
        {
            prod = (ISoarProduction) reference;
        }
    	return prod;
    }
    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) 
    {
        this.selection = selection;
       	action.setEnabled(getCurrentProduction()!=null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
     */
    public void dispose() 
    {
        //do nothing
    }

}
