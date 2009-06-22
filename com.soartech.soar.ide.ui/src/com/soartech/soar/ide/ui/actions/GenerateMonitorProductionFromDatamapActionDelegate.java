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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode;
import com.soartech.soar.ide.core.model.datamap.SoarDatamapTools;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.SoarUiTools;
import com.soartech.soar.ide.ui.editors.text.SoarEditor;

/**
 * @author ray
 */
public class GenerateMonitorProductionFromDatamapActionDelegate implements
        IObjectActionDelegate
{
    private List<ISoarDatamapAttribute> selectedAttributes = new ArrayList<ISoarDatamapAttribute>();
    
    private Map<Character, Integer> varIndexMap = new HashMap<Character, Integer>();
    private Map<String, List<ISoarDatamapAttribute>> attrsBySourceVar = new LinkedHashMap<String, List<ISoarDatamapAttribute>>();
    private String tab = "    ";
    
    private String generateProductionName()
    {
        StringBuilder b = new StringBuilder("monitor-attributes*");
        boolean first = true;
        for(ISoarDatamapAttribute a : selectedAttributes)
        {
            if(!first)
            {
                b.append('-');
            }
            if ( a.getName() != null ) {
            	b.append(a.getName().replace(' ', '_'));
            }
            
            first = false;
        }
        
        return b.toString();
    }
    
    private String generateVariableName(ISoarDatamapAttribute a)
    {
        String name = null;
        String attrName = a.getName();
        if ( attrName == null ) {
        	// variablized
        	attrName = "*";
        }
        char c = attrName.toLowerCase().charAt(0);
        Integer index = varIndexMap.get(c);
        if(index == null)
        {
            index = 0;
        }
        name = "<" + c + index + ">";
        varIndexMap.put(c, index + 1);
        
        return name;
    }
    
    private void updateTab(SoarEditor editor)
    {
        // TODO: Update tab to correct width based on current editor setting.
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action)
    {
        SoarEditor editor = SoarUiModelTools.getActiveSoarEditor();
        if(selectedAttributes.isEmpty() || editor == null || editor.getSoarFileWorkingCopy() == null)
        {
            return;
        }
        
        updateTab(editor);
        
        varIndexMap.clear();
        attrsBySourceVar.clear();
        
        ISoarDatamap datamap = selectedAttributes.get(0).getDatamap();
        
        // Get all of the attributes involved
        Set<ISoarDatamapAttribute> allAttrs = new LinkedHashSet<ISoarDatamapAttribute>();
        for(ISoarDatamapAttribute a : selectedAttributes)
        {
            // TODO: This is pretty inefficient and stupid. If the selection 
            // received in selectionChanged is a TreeSelection, use the paths
            // there rather than calculating them here
            allAttrs.addAll(SoarDatamapTools.getPathToNode(a.getTarget()));
        }
        
        // Map from node to variable name
        Map<ISoarDatamapNode, String> varMap = new HashMap<ISoarDatamapNode, String>();
        varMap.put(datamap.getState(), "<s>");
        
        // Generate variable names for all nodes
        for(ISoarDatamapAttribute a : allAttrs)
        {
            ISoarDatamapNode target = a.getTarget();
            
            if(!varMap.containsKey(target))
            {
                varMap.put(target, generateVariableName(a));
            }
        }
        
        // Put attributes into buckets keyed by their source variable
        for(ISoarDatamapAttribute a : allAttrs)
        {
            ISoarDatamapNode source = a.getSource();
            String sourceVar = varMap.get(source);
            if(!attrsBySourceVar.containsKey(sourceVar))
            {
                attrsBySourceVar.put(sourceVar, new ArrayList<ISoarDatamapAttribute>());
            }
            List<ISoarDatamapAttribute> attrs = attrsBySourceVar.get(sourceVar);
            if(!attrs.contains(a))
            {
                attrs.add(a);
            }
        }
        
        // Build the production
        StringBuilder b = new StringBuilder();
        b.append("\n##!\n# @brief Auto-generated monitor production\n");
        b.append("sp {" + generateProductionName() + "\n");
        for(Map.Entry<String, List<ISoarDatamapAttribute>> e : attrsBySourceVar.entrySet())
        {
            String sourceVar = e.getKey();
            List<ISoarDatamapAttribute> attrs = e.getValue();
            b.append(tab + "(" + (sourceVar.equals("<s>") ? "state ": "") + sourceVar);
            for(ISoarDatamapAttribute a : attrs)
            {
                String targetVar = varMap.get(a.getTarget());
                String name = a.getName();
                if ( name == null ) {
                	name = generateVariableName(a);
                }
                b.append(" ^" + name + " " + targetVar);
            }
            b.append(")\n");
        }
        b.append("-->\n");
        b.append(tab + "(write (crlf) |Auto-generated monitor: |\n");
        for(int i = 0; i < selectedAttributes.size(); ++i)
        {
            ISoarDatamapAttribute a = selectedAttributes.get(i);
            
            String v = varMap.get(a.getTarget());
            b.append(tab + tab + "|");
            String name = a.getName();
            if ( name == null ) {
            	name = v;
            }
            b.append(name + "=| " + v);
            if(i < selectedAttributes.size() - 1)
            {
                b.append(" |, |\n");
            }
        }
        b.append(tab + "\n)\n");
        b.append("}\n");
        
        // Now lock the working copy and insert the new production at the end
        // of the buffer.
        int lineOffset = -1;
        ISoarFile workingCopy = editor.getSoarFileWorkingCopy();
        if(workingCopy == null)
        {
            // TODO: Show a message
            return;
        }
        
        synchronized (workingCopy.getLock())
        {
            
            try
            {
                ISoarBuffer buffer = workingCopy.getBuffer();
                lineOffset = buffer.getLength() + 1;
                buffer.replace(buffer.getLength(), 0, b.toString());
            }
            catch(SoarModelException e)
            {
                return;
            }
        }
        
        if(lineOffset != -1)
        {
            editor.selectAndReveal(lineOffset, 0);
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection)
    {
        selectedAttributes.clear();
        selectedAttributes.addAll(SoarUiTools.getValuesFromSelection(selection, ISoarDatamapAttribute.class));
        if(SoarDatamapTools.allFromSameDatamap(selectedAttributes))
        {
            action.setEnabled(!selectedAttributes.isEmpty() && 
                              SoarUiModelTools.getActiveSoarEditor() != null);
        }
        else
        {
            selectedAttributes.clear();
            action.setEnabled(false);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart)
    {
    }

}
