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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ListDialog;

import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ITclFileReference;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.ui.SoarEditorPluginImages;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.editors.text.SoarEditor;

/**
 * An action that opens files that reference the current file in the editor.
 * That is, a quick way to jump to the load.soar file that sources the current
 * file.
 * 
 * @author ray
 */
public class OpenReferencingFilesEditorAction implements IEditorActionDelegate
{       
    private SoarEditor editor;
    
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
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) 
    {
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
        
        ISoarFile file = editor.getSoarFileWorkingCopy();
        if(file == null)
        {
            return;
        }
        
        try
        {
            List<ITclFileReference> references = SoarModelTools.getReferences(file);
            
            // If there's more than one referencing file referencing let the user choose.
            if(references.size() > 1)
            {
                references = selectFiles(references);
            }
            
            // Open editors for all selected references.
            for(ITclFileReference ref : references)
            {
                SoarUiModelTools.showElementInEditor(editor.getEditorSite().getPage(), ref);
            }
        }
        catch (CoreException e)
        {
            SoarEditorUIPlugin.log(e);
        }
     }
    
    private List<ITclFileReference> selectFiles(final List<ITclFileReference> references)
    {
        // Content provider that just returns list of references
        IStructuredContentProvider contentProvider = new IStructuredContentProvider()
        {
            public Object[] getElements(Object inputElement)
            {
                return references.toArray();
            }

            public void dispose()
            {
            }

            public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
            {
            }
        };
        
        // Label provider that shows Soar file icon and full path to file
        ILabelProvider labelProvider = new LabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                return SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_SOAR_FILE);
            }

            @Override
            public String getText(Object element)
            {
                return ((ITclFileReference) element).getContainingResource().getFullPath().toPortableString();
            }
            
        };
        
        // Initialize the handy list dialog
        ListDialog dialog = new ListDialog(editor.getEditorSite().getShell());
        dialog.setContentProvider(contentProvider);
        dialog.setLabelProvider(labelProvider);
        dialog.setInput(references);
        dialog.setMessage("Select referencing Soar file to open");
        dialog.setInitialSelections(new Object[] { references.get(0) });
        dialog.setTitle("Select referencing Soar file");
        dialog.open();
        
        Object[] result = dialog.getResult();
        List<ITclFileReference> resultRefs = new ArrayList<ITclFileReference>();
        if(result != null)
        {
            for(Object o : result)
            {
                resultRefs.add((ITclFileReference) o);
            }
        }
        
        return resultRefs;
    }
    
}
