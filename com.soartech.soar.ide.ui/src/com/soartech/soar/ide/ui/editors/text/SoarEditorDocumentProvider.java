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
package com.soartech.soar.ide.ui.editors.text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarWorkingCopyOwner;
import com.soartech.soar.ide.core.model.SoarModelException;

/**
 * Document provider that creates an ISoarFile working copy for a Soar file
 * being edited by the Soar editor. This file is a simplified adaptation of
 * RubyDocumentAdapter.java (try a google code search), which is itself an
 * adaptation of CompilationUnitDocumentProvider in the JDT. This object is
 * a singleton because it is shared by all Soar editors. This allows multiple
 * editors to simultaneously edit the same file, i.e. window->new editor.
 *
 * @author ray
 */
public class SoarEditorDocumentProvider 
    extends TextFileDocumentProvider 
{ 
    private static final SoarEditorDocumentProvider instance = new SoarEditorDocumentProvider();
    
    /**
     * @return The singleton instance.
     */
    public static SoarEditorDocumentProvider getInstance()
    {
        return instance;
    }
    
    /**
     * Custom FileInfo object in which we store the associated working copy and
     * document adapter
     * 
     * @author ray
     */
    protected static class SoarFileInfo extends FileInfo implements ISoarWorkingCopyOwner
    {
        ISoarFile workingCopy;
        SoarBufferDocumentAdapter documentAdapter;
        
        /* (non-Javadoc)
         * @see com.soartech.soar.ide.core.model.ISoarWorkingCopyOwner#createBuffer(com.soartech.soar.ide.core.model.ISoarFile)
         */
        public ISoarBuffer createBuffer(ISoarFile file)
        {
            assert file == workingCopy;
            return documentAdapter;
        }
    }
    
    private SoarEditorDocumentProvider()
    {
        // Create an internal text file document provider as our parent. Any
        // methods we don't implement will be forwarded to it. Kind of weird.
        IDocumentProvider provider = new TextFileDocumentProvider();
        setParentDocumentProvider(provider);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#createEmptyFileInfo()
     */
    protected FileInfo createEmptyFileInfo()
    {
        return new SoarFileInfo();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#createFileInfo(java.lang.Object)
     */
    protected FileInfo createFileInfo(Object element) throws CoreException 
    {
        // This is called when a document is requested for a file. Here is
        // where we set up our working copy and document adapter.
        if (!(element instanceof IFileEditorInput)) {
        	return super.createFileInfo(element);
        }

        IFileEditorInput input = (IFileEditorInput) element;
        ISoarFile original = findSoarFile(input.getFile());
        if (original == null) {
        	return super.createFileInfo(element);
        }

        FileInfo info = super.createFileInfo(element);
        if (!(info instanceof SoarFileInfo))
        {
            return info;
        }

        SoarFileInfo cuInfo = (SoarFileInfo) info;
        setUpSynchronization(cuInfo);
        
        cuInfo.documentAdapter = new SoarBufferDocumentAdapter(original, cuInfo.fTextFileBuffer.getDocument());
        cuInfo.workingCopy = original.createWorkingCopy(cuInfo);

        return cuInfo;
    }    
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#disposeFileInfo(java.lang.Object, org.eclipse.ui.editors.text.TextFileDocumentProvider.FileInfo)
     */
    protected void disposeFileInfo(Object element, FileInfo info) 
    {
        // This is called when the last editor on a file is closed so we can
        // dispose the working copy and clean up the buffer adapter
        if (info instanceof SoarFileInfo) 
        {
            SoarFileInfo cuInfo = (SoarFileInfo) info;
            if(cuInfo.workingCopy != null)
            {
                cuInfo.workingCopy.discardWorkingCopy();
                cuInfo.documentAdapter.close();
            }
        }
        super.disposeFileInfo(element, info);
    }
    
    /**
     * @return The current working copy
     * @throws SoarModelException 
     */
    public ISoarFile getSoarFileWorkingCopy(IEditorInput input)
    {
        FileInfo info = getFileInfo(input);
        if(!(info instanceof SoarFileInfo))
        {
            return null;
        }
        
        return ((SoarFileInfo) info).workingCopy;
    }

    public static ISoarFile findSoarFile(IFile file) throws SoarModelException
    {
    	return SoarCorePlugin.getDefault().getSoarModel().getFile(file);
    }
}
