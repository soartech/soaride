/**
 * 
 */
package edu.umich.soar.editor.editors.datamap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.action.Action;
import org.eclipse.core.runtime.Status;

import com.soartech.soar.ide.core.Logger;
import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarSourceReference;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode;
import com.soartech.soar.ide.core.model.datamap.SoarDatamapTools;
import com.soartech.soar.ide.core.model.impl.SoarProduction;
import com.soartech.soar.ide.core.model.impl.SoarProduction2;

import edu.umich.soar.editor.editors.datamap.Datamap;
import edu.umich.soar.editor.editors.datamap.DatamapAttribute;


/**
 * @author aron
 *
 */
public class ValidateDatamapAction extends Action {

    private Datamap staticDatamap;
    private ISoarDatamap soarDatamap;
    private String source;
    
    public ValidateDatamapAction(Datamap staticDatamap, ISoarDatamap soarDatamap, String source)
    {
        super("Validate Datamap against Soar Project");
        
        this.staticDatamap = staticDatamap;
        this.soarDatamap = soarDatamap;
        this.source = source;
    }

    @Override
    public void run() 
    {
        System.out.println("Validating datamap against the Soar Project");
        
        //get the datamap file
        File datamapFile = staticDatamap.getFile();
        IFile datamapIFile = staticDatamap.getIFile();
        File datamapDir = null;
        IContainer datamapIDir = null;
        try
        {
            datamapDir = datamapFile.getCanonicalFile().getParentFile();
            datamapIDir = datamapIFile.getParent();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
            return;
        }
//        File[] datamapDirFiles = datamapDir.listFiles();
        IResource[] datamapIDirFiles;
        try
        {
            datamapIDirFiles = datamapIDir.members();
        }
        catch (CoreException e1)
        {
            e1.printStackTrace();
            return;
        }

        ISoarProject soarProject = null;
        try {

            // get the project from the datamap file
            soarProject = SoarCorePlugin.getDefault().getInternalSoarModel().createSoarProject(datamapIFile.getProject());
            
            System.out.println("Processing SoarProject: " + soarProject.toString());
            
            //get the agent to check by getting a soar file next to the .dm file
            ISoarAgent agentToCheck = null;
            for (IResource resource : datamapIDirFiles)
            {
                if (resource instanceof IFile)
                {
                      IFile file = (IFile) resource;
                      
                      if (file.getName().endsWith(".soar"))
                      {
                          ISoarFile soarFile = soarProject.getSoarFile(file);
                          if(soarFile != null) 
                          {
                              ISoarAgent soarAgent = soarProject.getPreferredAgent(soarFile);
                              if(soarAgent != null) 
                              {
                                  agentToCheck = soarAgent;
                                  break;
                              }
                          }
                      }
                }
                
            }
            
            System.out.println("Processing Soar Agent: " + agentToCheck.getName());
            
            //get the static datamap
            Map<Integer, ArrayList<DatamapAttribute>> attrMap = staticDatamap.getAttributes();
            
            //get the dynamic datamap and attrs
            if(soarDatamap == null)
            {
                soarDatamap = agentToCheck.getDatamap();
            }
            
            Map<String, ISoarDatamapNode> dynamicNodes = SoarDatamapTools.getAllElementNodes(soarDatamap);
            
            //create a map
            ArrayList<DatamapAttribute> level0Attrs = attrMap.get(0);
            Map<String, DatamapAttribute> staticLevel0AttrMap = new HashMap<String, DatamapAttribute>();
            for(DatamapAttribute attr:level0Attrs)
            {
                staticLevel0AttrMap.put(attr.name, attr);
            }
            
            //go over each leaf attribute node in the dynamic datamap
            for(String key : dynamicNodes.keySet())
            {
                ISoarDatamapNode node = dynamicNodes.get(key);
                
                System.out.println("Checking dynamic node: " + key);
                
                DatamapAttribute currStaticAttr = null;
                ISoarDatamapAttribute currDynamicAttr = null;
                
                boolean hasError = false;
                
                String errorPath = "[state";
                String errorMessage = "Missing in static datamap: \n ";
                
                //get the path to this dynamic leaf attribute node
                List<ISoarDatamapAttribute> nodePath = SoarDatamapTools.getPathToNode(node);
                //iterate over the ordered list of ISoarDatamapAttribute objects representing the path to this node
                for(ISoarDatamapAttribute attr : nodePath)
                {
                    System.out.println("path part: " + attr.getName());
                    currDynamicAttr = attr;
                    errorPath += "." + attr.getName();
                    
                    //if there is no staticAttr mapped to this dynamic node path part, then get it from the base level
                    if(currStaticAttr == null)
                    {
                        currStaticAttr = staticLevel0AttrMap.get(attr.getName());
                        if(currStaticAttr == null)
                        {
                            System.out.println("Dynamic node " + attr.getName() + " not in static map");
                            hasError = true;
                            errorMessage = updateErrorMessage(errorMessage, errorPath);
//                            break;
                        }
                        else
                        {
                            //base level attr is in the map
                            //do nothing?
                        }
                    }
                    else
                    {
                        //check if attr is on next level in map
                        boolean hasAttr = false;
                        ArrayList<DatamapAttribute> toAttrs = attrMap.get(currStaticAttr.to);
                        if(toAttrs == null)
                        {
                            hasError = true;
                            errorMessage = updateErrorMessage(errorMessage, errorPath);
//                            break;
                        }
                        else
                        {
                            for(DatamapAttribute toAttr : toAttrs)
                            {
                                if(toAttr.name.equals(attr.getName()))
                                {
                                    currStaticAttr = toAttr;
                                    hasAttr = true;
                                }
                            }
                            
                            if(!hasAttr)
                            {
                                System.out.println("Dynamic node " + attr.getName() + " not in static map");
                                hasError = true;
                                errorMessage = updateErrorMessage(errorMessage, errorPath);
//                                break;
                            }
                        }
                        
                    }
                    
                }
                
                if(hasError)
                {
                    System.out.println("ERROR: node is not in static datamap: " + key);
                    
                    //set error on node in editor here
                    Set<ISoarProduction> supportingProductions = currDynamicAttr.getSupportingProductions();
                    
                    for(ISoarProduction sp : supportingProductions)
                    {
                        System.out.println(" -> error in " + sp.getProductionName());
                        
                        if(source != null)
                        {
                            
                            //sp is the ISoarProduction from the expanded code
                            //ideally we want the ISoarProduction from the file's code
                            //so the marker is in the right spot
                            int index = source.indexOf(sp.getProductionName());
                            int length = sp.getProductionName().length();
                            
                            if(sp instanceof SoarProduction2)
                            {
                                ISoarSourceRange tclMacroRange = ((SoarProduction2) sp).getTclSourceRange();
                                if(tclMacroRange != null)
                                {
                                    index = tclMacroRange.getOffset();
                                    length = tclMacroRange.getLength();
                                }
                            }
                            
                            try {
//                                ISoarSourceRange sourceRange = getEditorLocation(sp);
//                                createErrorMarker(sp, currDynamicAttr, soarProject);
//                                errorMessage += "]";
                                createErrorMarker(sp, errorMessage, index, length, currDynamicAttr, soarProject);
                                
                            } catch (CoreException e) {
                                e.printStackTrace();
                            }
                        }
                        else
                        {
                            try {
//                                ISoarSourceRange sourceRange = getEditorLocation(sp);
//                                errorMessage += "]";
                                createErrorMarker(sp, errorMessage, currDynamicAttr, soarProject);
                                
                            } catch (CoreException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                else
                {
                    System.out.println("SUCCESS: node is in static datamap: " + key);
                }
            }
            
            
            
        } catch (SoarModelException e1) {
            e1.printStackTrace();
        }
        
        System.out.println("Finished Validating datamap!");
        
    }
    
    private String updateErrorMessage(String errorMessage, String errorPath)
    {
        errorMessage += errorPath + "] \n ";
        return errorMessage;
    }
    
    private void createErrorMarker(ISoarElement element, String message, ISoarDatamapAttribute attr, ISoarProject soarProject) throws CoreException
    {
        // Find the resource that contains the element
        IResource resource = element.getContainingResource();
        if(resource == null)
        {
            return;
        }
        
        // Now see if it's a file
        IFile file = (IFile) resource.getAdapter(IFile.class);
        if(file == null)
        {
            return;
        }
        
        //don't proceed with null attrs?
        if(attr == null || attr.getName() == null)
        {
            return;
        }
        
        if(element instanceof ISoarSourceReference)
        {
            ISoarSourceReference sourceRef = (ISoarSourceReference) element;
            
            ISoarSourceRange range = sourceRef.getSourceRange();
            
//            System.out.println(" -> with file " + file.getFullPath().toOSString());
//            System.out.println(" -> with offset " + range.getOffset() + " and length " + range.getLength());
            
            String source = sourceRef.getSource();
            
//            System.out.println("source: " + source);
            
            //try to get the offset of the attr in the production
            int offsetIntoProduction = source.indexOf("^" + attr.getName());
            int lengthAdded = 1;
            if(offsetIntoProduction < 0)
            {
                offsetIntoProduction = source.indexOf(attr.getName());
                lengthAdded = 0;
            }
            
            System.out.println("[ValidateDatamapAction]: creating error marker for attr " + attr.getName());
            
            if(offsetIntoProduction > 0)
            {
                SoarModelTools.createWarningMarker(SoarCorePlugin.DATAMAP_PROBLEM_MARKER_ID, file, range.getOffset() + offsetIntoProduction, attr.getName().length() + lengthAdded, message);
//                SoarModelTools.createErrorMarker(SoarCorePlugin.DATAMAP_PROBLEM_MARKER_ID, file, range.getOffset() + offsetIntoProduction, attr.getName().length() + lengthAdded, "Attribute " + attr.getName() + " not in static datamap");
            }
            else
            {
                SoarModelTools.createWarningMarker(SoarCorePlugin.DATAMAP_PROBLEM_MARKER_ID, file, range.getOffset(), range.getLength(), message);
//                SoarModelTools.createErrorMarker(SoarCorePlugin.DATAMAP_PROBLEM_MARKER_ID, file, range.getOffset(), range.getLength(), "Attribute " + attr.getName() + " not in static datamap");
            }
            
            return;
            
        }
        else
        {
            return;
        }
    }
    
    
    private void createErrorMarker(ISoarElement element, String message, int index, int length, ISoarDatamapAttribute attr, ISoarProject soarProject) throws CoreException
    {
        // Find the resource that contains the element
        IResource resource = element.getContainingResource();
        if(resource == null)
        {
            return;
        }
        
        // Now see if it's a file
        IFile file = (IFile) resource.getAdapter(IFile.class);
        if(file == null)
        {
            return;
        }
        
        //don't proceed with null attrs?
        if(attr == null || attr.getName() == null)
        {
            return;
        }
        
//        System.out.println(" -> with file " + file.getFullPath().toOSString());
//        System.out.println(" -> with offset " + index + " and length " + length);
        System.out.println("[ValidateDatamapAction]: creating error marker for attr " + attr.getName());
        
        SoarModelTools.createWarningMarker(SoarCorePlugin.DATAMAP_PROBLEM_MARKER_ID, file, index, length, message);
//        SoarModelTools.createErrorMarker(SoarCorePlugin.DATAMAP_PROBLEM_MARKER_ID, file, index, length, "Attribute " + attr.getName() + " not in static datamap");
    }
    
}
