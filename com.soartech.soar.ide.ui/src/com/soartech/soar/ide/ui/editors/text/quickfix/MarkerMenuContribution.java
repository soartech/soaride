package com.soartech.soar.ide.ui.editors.text.quickfix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.part.FileEditorInput;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.editors.text.SoarEditor;

import edu.umich.soar.editor.editors.datamap.Datamap;
import edu.umich.soar.editor.editors.datamap.DatamapAttribute;
import edu.umich.soar.editor.editors.datamap.DatamapNode;
import edu.umich.soar.editor.editors.datamap.DatamapNode.NodeType;

public class MarkerMenuContribution extends ContributionItem 
{
    private SoarEditor editor;
    private IVerticalRulerInfo rulerInfo;
    private List<IMarker> markers;

    public MarkerMenuContribution(SoarEditor editor){
        this.editor = editor;
        this.rulerInfo = getRulerInfo();
        this.markers = getMarkers();
    }

    private IVerticalRulerInfo getRulerInfo(){
        return (IVerticalRulerInfo) editor.getAdapter(IVerticalRulerInfo.class);
    }

    private List<IMarker> getMarkers(){
        List<IMarker> clickedOnMarkers = new ArrayList<IMarker>();
        for (IMarker marker : getAllMarkers()){
            if (markerHasBeenClicked(marker)){
                clickedOnMarkers.add(marker);
            }
        }

        return clickedOnMarkers;
    }

    //Determine whether the marker has been clicked using the ruler's mouse listener
    private boolean markerHasBeenClicked(IMarker marker){
        return (marker.getAttribute(IMarker.LINE_NUMBER, 0)) == (rulerInfo.getLineOfLastMouseButtonActivity() + 1);
    }

    //Get all My Markers for this source file
    private IMarker[] getAllMarkers(){
        try {
            return ((FileEditorInput) editor.getEditorInput()).getFile()
                .findMarkers(SoarCorePlugin.DATAMAP_PROBLEM_MARKER_ID, true, IResource.DEPTH_ZERO);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return new IMarker[] {};
    }

    @Override
    //Create a menu item for each marker on the line clicked on
    public void fill(Menu menu, int index){
        for (final IMarker marker : markers){
            MenuItem menuItem = new MenuItem(menu, SWT.CHECK, index);
            menuItem.setText("Add to static datamap: " + marker.getAttribute(IMarker.MESSAGE, ""));
            menuItem.addSelectionListener(createDynamicSelectionListener(marker));
        }
    }

    private DatamapAttribute getDatamapAttribute(String name, List<DatamapAttribute> attrs)
    {
        if(attrs == null)
        {
            return null;
        }
        
        for(DatamapAttribute attr:attrs)
        {
            if(attr.name.equals(name))
            {
                return attr;
            }
        }
        
        return null;
    }
    
    //Action to be performed when clicking on the menu item is defined here
    private SelectionAdapter createDynamicSelectionListener(final IMarker marker){
        return new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e)
            {
                String message = marker.getAttribute(IMarker.MESSAGE, "");
                System.out.println("Fixing " + message);
                
                //extract the paths from the marker message
                Pattern p = Pattern.compile("\\[(.*?)\\]");
                Matcher m = p.matcher(message);
                List<String> markerPaths = new ArrayList<String>();
                while(m.find()) {
                    markerPaths.add(m.group(1));
                }
                
                ISoarAgent agent = null;
                try {
                    List<ISoarFileAgentProxy> agentProxies = editor.getSoarFileWorkingCopy().getAgentProxies();
                    if(!agentProxies.isEmpty())
                    {
                        agent = agentProxies.get(0).getAgent();
                    }
                } catch (SoarModelException e1) {
                    e1.printStackTrace();
                }
                
                if(agent != null)
                {
                    //get the static datamaps
                    Set<IResource> agentFiles = agent.getMembers();
                    for(IResource res : agentFiles)
                    {
                        if (res instanceof IFile)
                        {
                            IFile f = (IFile) res;
                            
                            String extension = f.getFileExtension();
                            if(extension.equals("dm"))
                            {
                                Datamap staticDatamap = Datamap.read(f);

                                //get the static datamap
                                Map<Integer, ArrayList<DatamapAttribute>> attrMap = staticDatamap.getAttributes();
                                
                                for(String markerPathString:markerPaths)
                                {
                                    String[] markerPath = markerPathString.split("\\.");

                                    //attributes at the base level of state
                                    ArrayList<DatamapAttribute> attrsToCheck = attrMap.get(0);
                                    DatamapNode prevNode = staticDatamap.getNode(0);
                                    DatamapAttribute currentAttr = null;
                                    
                                    //follow the marker path starting at index 1 because index 0 is state
                                    for(int i = 1; i < markerPath.length; i++)
                                    {
                                        //get the attr with the name from this part of the path
                                        String attrName = markerPath[i];
                                        currentAttr = getDatamapAttribute(attrName, attrsToCheck);
                                        
                                        if(currentAttr != null)
                                        {
                                            //get the next set of attrs to check
                                            attrsToCheck = attrMap.get(currentAttr.to);
                                            
                                            //set prev node
                                            prevNode = currentAttr.getTarget();
                                        }
                                        else
                                        {
                                            //add the new node to the static datamap
                                            DatamapNode newNode = prevNode.addChild(attrName, NodeType.SOAR_ID);
                                            prevNode = newNode;
                                        }
                                    }
                                }
                                
                                //force rconcile on editor to update warning markers
                                editor.getConfiguration().forceReconcile();
                                
                            }
                        }
                    }
                }
                
            }
        };
    }
}
