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
package com.soartech.soar.ide.core.refactoring;

import java.util.List;

import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.IFile;
import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ITclFileReference;
import com.soartech.soar.ide.core.model.SoarModelTools;

/**
 * <code>SoarRename</code> participant in rename refactoring of IFiles.
 *
 * @author sfurtwangler@soartech.com
 * @version 
 */
public class SoarRename extends RenameParticipant {

	private IFile file;
    private String oldName;
    private String newName;

    @Override
    protected boolean initialize(Object element) {
        file = (IFile) element;
        String ext = file.getFileExtension();
        if(file==null || (!ext.equalsIgnoreCase("soar") && !ext.equalsIgnoreCase("tcl"))) return false;
        oldName = file.getName();
        newName = getArguments().getNewName();
        return true;
    }
    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.RenameParticipant#createChange
     */
    @Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException
    {
		CompositeChange change = null;
        if (change == null) {
            change = new CompositeChange("Changing references to "+file.getName());
        }
       	//first, we need to get the soar model
       	ISoarModel soarModel = SoarCorePlugin.getDefault().getSoarModel();
       	//then we can get the Soar-specific model for this file.
       	ISoarFile soarFile = soarModel.getFile(file);
       	//get all references to that file
       	List<ITclFileReference> references = SoarModelTools.getReferences(soarFile);
       	//iterate over them
        for(ITclFileReference ref : references)
        {
           	//get the line/reference where our file is referenced
           	String source = ref.getSource();

           	//look for the old filename
           	int offset = source.indexOf(oldName, 0);
           	
           	if(offset>=0) // verify that the filename was found (should always be the case)
           	{
           		//get the range in the overall file where our reference was located
           		ISoarSourceRange range = ref.getSourceRange();

           		//replace the instance of the old filename with this new filename (which should maintain the relative path)
           		ReplaceEdit edit = new ReplaceEdit(range.getOffset()+offset,oldName.length(),newName);

           		//get a pointer to the parent file that is referring to our file
           		IFile parentFile = (IFile)ref.getContainingResource();
           	
           		if(parentFile!=file) //do not change a file that references itself. We should, but trying to will crash eclipse.
           		{
           			//create a text-change instance
           			TextFileChange ch = new TextFileChange("Changing references in "+ref.getPath(),parentFile);

           			//Add the text replacement
           			ch.setEdit(edit);

           			// add the change to our composite
           			change.add(ch);
           		}
           	}
        }
		return change;
    }

    @Override
    public String getName() {
        return "Updating references to renamed file.";
    }

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm,
            CheckConditionsContext context) throws OperationCanceledException {
        return new RefactoringStatus();// OK
    }

}