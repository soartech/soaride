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
package com.soartech.soar.ide.ui.views.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.ITclCommand;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

/**
 * Content provider for the Soar Package Explorer.
 * 
 * This content provider procides the following structure:
 * 	+ Project
 * 		+ folder/folder/
 * 			+ file.soar
 * 			+ file.tcl
 * 				~ procedure1
 * 		+ folder/
 * 			+file.soar
 * 				~ production1
 * 				~ production2
 * 		+ file.soar
 * 		+ file.tcl
 * 		...
 * 		
 * 
 * @author aron
 *
 */
public class SoarExplorerFullViewContentProvider implements ITreeContentProvider 
{
	private final Object[] EMPTY_ARRAY = {};
	
//	private Map<ISoarFile, SoarElementHeader> procedureHeaderMap = 
//		new HashMap<ISoarFile, SoarElementHeader>();
//	
//	private Map<ISoarFile, SoarElementHeader> productionHeaderMap = 
//		new HashMap<ISoarFile, SoarElementHeader>();
	
	private Map<ISoarFile, SoarFolderHeader> folderHeaderMap =
		new HashMap<ISoarFile, SoarFolderHeader>();
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) 
	{
		//nothing for now
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object element) 
	{
		if(element instanceof ISoarModel)
		{
			ISoarModel model = (ISoarModel) element;
			try {
				Object[] ret = model.getProjects().toArray();
				return ret;
			} catch (SoarModelException e) {
				SoarEditorUIPlugin.log(e);
			}
		}
		else if(element instanceof ISoarProject)
		{
			ISoarProject project = (ISoarProject) element;
			
			List<ISoarElement> children = new ArrayList<ISoarElement>();
            List<Object> ret = new ArrayList<Object>();
			try {
                ret.addAll(project.getAgents());
				children.addAll(project.getChildren());
			} catch (SoarModelException e) {
				e.printStackTrace();
			}
			
			List<ISoarFile> files = new ArrayList<ISoarFile>();
			
			Map<String, SoarFolderHeader> headerMap = new HashMap<String, SoarFolderHeader>();
			
			for(ISoarElement e:children)
			{
				if(e instanceof ISoarFile)
				{
					//get the file
					ISoarFile file = (ISoarFile) e;
					
					//get the file path
					IPath path = file.getPath();
					String pathStr = path.toString();
					
					//get the filename
					String lastSegment = path.lastSegment();
					
					//get the project info
					ISoarProject proj = (ISoarProject) file.getParent();
					String projName = proj.getProject().getName();
					int projectNameLength = projName.length();
					
					//get the path to the file relative to the project
					String pathUnderProject = pathStr.substring(projectNameLength + 2);
					
					//get the index of the beginning of the filename in the path
					int pathIndex = pathUnderProject.indexOf(lastSegment);
					
					if(pathIndex == 0)
					{
						//the file resides directly under the project
						files.add(file);
					}
					else
					{
						//the file is within nested files within the project
						String folderPath = pathUnderProject.substring(0, pathIndex);
						
						//change the '/' to '.'
						folderPath = folderPath.replace('/', '.');
						
						//remove the trailing '.' from the path
						folderPath = folderPath.substring(0, folderPath.length() - 1);
						
						if(headerMap.containsKey(folderPath))
						{
							//the appropriate SoarFolderHeader has already been created
							//add the file to it
							
							SoarFolderHeader folder = headerMap.get(folderPath);
							folder.addChild(file);
						}
						else
						{
							//create the appropriate SoarFolderHeader
							//add the file to it, add it to the header map and return array
							
							List<ISoarFile> headerChildren = new ArrayList<ISoarFile>();
							headerChildren.add(file);
							
							SoarFolderHeader folder = new SoarFolderHeader(folderPath, proj, headerChildren);
							headerMap.put(folderPath, folder);
							
							folderHeaderMap.put(file, folder);
							
							ret.add(folder);
						}
					}
				}
				
			}
			
			//add all the files after the headers have been added
			ret.addAll(files);
			
			return ret.toArray();
		}
		else if(element instanceof ISoarFile)
		{
			ISoarFile file = (ISoarFile) element;
			
            try
            {
                return file.getAgentProxies().toArray();
            }
            catch (SoarModelException e)
            {
                e.printStackTrace();
            }
        }
        else if(element instanceof ISoarFileAgentProxy)
        {
            ISoarFileAgentProxy proxy = (ISoarFileAgentProxy) element;
			List<ITclCommand> commands = new ArrayList<ITclCommand>();
			commands.addAll(proxy.getProductions());
			commands.addAll(proxy.getProcedures());
			
			return commands.toArray();
			
//			List<ITclCommand> procedures = new ArrayList<ITclCommand>();
//			List<ITclCommand> productions = new ArrayList<ITclCommand>();
//			
//			procedures.addAll(file.getProcedures());
//			productions.addAll(file.getProductions());
//			
//			procedureHeaderMap.remove(file);
//			productionHeaderMap.remove(file);
//			
//			SoarElementHeader procedureHeader = 
//				new SoarElementHeader("Procedures", file, procedures);
//			
//			SoarElementHeader productionHeader = 
//				new SoarElementHeader("Productions", file, productions);
//			
//			procedureHeaderMap.put(file, procedureHeader);
//			productionHeaderMap.put(file, productionHeader);
//			
//			Object[] ret = {productionHeader, procedureHeader};
//			
//			return ret;
		}
		else if(element instanceof SoarFolderHeader)
		{
			SoarFolderHeader header = (SoarFolderHeader) element;
			
			return header.getChildren().toArray();
		}
//		else if(element instanceof SoarElementHeader)
//		{
//			SoarElementHeader header = (SoarElementHeader) element;
//			
//			return header.getChildren().toArray();
//		}
		
		return EMPTY_ARRAY;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) 
	{
		if(element instanceof ITclCommand)
		{
			ITclCommand cmd = (ITclCommand) element;
			
			return cmd.getParent();
			
//			if(element instanceof ITclProcedure)
//			{	
//				ITclProcedure procedure = (ITclProcedure) element;
//				
//				//look up the header (parent) for this procedure in the map
//				return procedureHeaderMap.get(procedure.getParent());
//			}
//			else if(element instanceof ISoarProduction)
//			{
//				ISoarProduction production = (ISoarProduction) element;
//				
//				//look up the header (parent) for this production in the map
//				return productionHeaderMap.get(production.getParent());
//			}
		}
		else if(element instanceof ISoarFile)
		{
			ISoarFile file = (ISoarFile) element;
			
			if(folderHeaderMap.containsKey(file))
			{
				return folderHeaderMap.get(file);
			}
			
			return file.getParent();
		}
		else if(element instanceof ISoarElement)
		{
			ISoarElement soarElement = (ISoarElement) element;
			
			return soarElement.getParent();
		}
//		else if(element instanceof SoarElementHeader)
//		{
//			SoarElementHeader header = (SoarElementHeader) element;
//			
//			return header.getParent();
//		}
		else if(element instanceof SoarFolderHeader)
		{
			SoarFolderHeader header = (SoarFolderHeader) element;
			
			return header.getParent();
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) 
	{
		if(element instanceof ITclCommand) 
		{
			return false;
		}
		else if(element instanceof ISoarElement)
		{
			ISoarElement soarElement = (ISoarElement) element;
			
            // TODO: For ISoarFile need to loop through children and look for
            // procs and productions.  Just calling hasChildren may still be
            // true if source statements are present.
			return soarElement.hasChildren();
		}
		else if(element instanceof SoarFolderHeader)
		{
			SoarFolderHeader header = (SoarFolderHeader) element;
			
			return !header.getChildren().isEmpty();
		}
//		else if(element instanceof SoarElementHeader)
//		{
//			SoarElementHeader header = (SoarElementHeader) element;
//			
//			return !header.getChildren().isEmpty();
//		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) 
	{
        return getChildren(inputElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub

	}
	
//	private String ReplaceSlashesWithDots(String path)
//	{
//		return path.replace('/', '.');
//		return null;
//	}
	
	/**
	 * This class acts as an "in-between" header element to be displayed
	 * in the Soar Package Explorer. It contains a list of ISoarFile
	 * objects as it's children.
	 *
	 */
	public class SoarFolderHeader extends PlatformObject
	{
		private String label;
		private ISoarElement parent;
		private List<ISoarFile> children = new ArrayList<ISoarFile>();
		
		private SoarFolderHeader(String label, ISoarElement parent, List<ISoarFile> children)
		{
			this.label = label;
			this.parent = parent;
			this.children.addAll(children);
		}

		/**
		 * @return the label
		 */
		public String getLabel() 
		{
			return label;
		}
		
		/**
		 * @return the parent
		 */
		public ISoarElement getParent() 
		{
			return parent;
		}
		
		/**
		 * Add a new child to this header.
		 * 
		 * @param child
		 */
		public void addChild(ISoarFile child)
		{
			children.add(child);
		}

		/**
		 * @return the children
		 */
		public List<ISoarFile> getChildren() 
		{
			return children;
		}

        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
         */
        @Override
        public Object getAdapter(Class adapter)
        {
            // Provide conversion to IContainer so that container menu options
            // show up in popup menu.
            if(adapter.equals(IResource.class) || adapter.equals(IContainer.class))
            {
                // TODO: This should probably store the IContainer directly
                // rather than getting it from the child.
                if(!children.isEmpty())
                {
                    return children.get(0).getFile().getParent();
                }
            }
            else if(adapter.equals(IFolder.class))
            {
                if(!children.isEmpty())
                {
                    return children.get(0).getFile().getParent().getAdapter(adapter);
                }
                
            }
            return super.getAdapter(adapter);
        }
        
        
	}


}
