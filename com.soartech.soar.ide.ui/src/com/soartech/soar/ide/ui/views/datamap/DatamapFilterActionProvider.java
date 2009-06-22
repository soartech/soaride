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
package com.soartech.soar.ide.ui.views.datamap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapValue;
import com.soartech.soar.ide.core.model.datamap.SoarDatamapTools;
import com.soartech.soar.ide.ui.SoarUiTools;

public class DatamapFilterActionProvider implements ISelectionChangedListener
{
    private SoarDatamapView view;    
    private ISoarDatamapAttribute selectedAttribute = null;
    
    public DatamapFilterActionProvider(SoarDatamapView view)
    {
        this.view = view;
    }
 
    public IContributionItem createFilterMenu()
    {
        // Only add the filter options if exactly one ISoarDatamapAttribute is selected
        if (selectedAttribute == null) return null;
        
        MenuManager filterMenu = new MenuManager("Filter for productions that test...");
        filterMenu.add(new AttributeFilterAction(selectedAttribute));

        // Stop here if the attribute doesn't have any values
        ISoarDatamapNode target = selectedAttribute.getTarget();
        if (target == null) return filterMenu;
               
        filterMenu.add(new Separator());
        
        for (ISoarDatamapValue value : target.getValues() )
        {
            filterMenu.add(new ValueFilterAction(selectedAttribute, value));    
        }

        return filterMenu; 
    }
    
    public void selectionChanged(SelectionChangedEvent event)
    {
        selectedAttribute = null;

        // Record the selected attribute only if there is exactly one        
        List<ISoarDatamapAttribute> selectedAttributes = new ArrayList<ISoarDatamapAttribute>();
        selectedAttributes.addAll(SoarUiTools.getValuesFromSelection(event.getSelection(), ISoarDatamapAttribute.class));
        if (selectedAttributes.size() == 1)
        {
            selectedAttribute = selectedAttributes.get(0);
        }
    }
    
    /**
     * Re-seek attribute, as it could have been removed and recreated
     * after a parse, if the attribute was only used in the current file.
     * 
     * @param attributePath path to the original location the attribute could be found.
     * @return the same or a new attribute that matches the path in the current datamap, or null if that attribute no longer exists.
     */
    private static ISoarDatamapAttribute findCurrentAttributeFromOldPath(List<ISoarDatamapAttribute> attributePath)
    {
        if (attributePath.size() == 0) { return null; }
        
        ISoarDatamapAttribute fromPath = attributePath.get(0);
        if (fromPath == null) { return null; }
        
        // The linked-back datamap always persists, even if the attribute
        // has since been removed.
        ISoarDatamap datamap = fromPath.getDatamap();

        ISoarDatamapAttribute actual = datamap.getState().getAttribute(fromPath.getName());

        for (int i = 1; i < attributePath.size(); ++i)
        {
            if (actual == null) { return null; }
            if (!fromPath.getName().equals(actual.getName())) { return null; }
            
            // We've got a match so far, so step both elements forward
            
            fromPath = attributePath.get(i);
            if (fromPath == null)
            {
                // This shouldn't typically be possible if the list came from
                // SoarDatamapTools.getPathToNode(), but it's good practice.
                return null;
            }
            
            actual = actual.getTarget().getAttribute(fromPath.getName());
        }
        
        return actual;
    }
    
    private static boolean allowElementThroughProductionFilter(Object element, Set<ISoarProduction> allowedProductions)
    {
        if (element instanceof ISoarDatamapAttribute)
        {
            ISoarDatamapAttribute attribute = (ISoarDatamapAttribute)element;

            for (ISoarProduction production : allowedProductions)
            {
                if (attribute.getSupportingProductionUsage(production) != 0)
                {
                    return true;
                }
            }
            
            return false;
        }
        
        if (element instanceof ISoarProduction)
        {
            ISoarProduction production = (ISoarProduction)element;
            return (allowedProductions.contains(production));
        }
        
        return true;
    }
    
    // This works for both the datamap element tree and the datamap production list 
    private class AttributeFilter extends ViewerFilter
    {
        private List<ISoarDatamapAttribute> attributePath; 

        public AttributeFilter(List<ISoarDatamapAttribute> attributePath)
        {
            this.attributePath = attributePath;
        }
        
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element)
        {
            // TODO: This is (most-likely) super-inefficient
            ISoarDatamapAttribute attribute = findCurrentAttributeFromOldPath(attributePath);
            if (attribute == null)
            {
                // Essentially stop filtering by allowing everything
                return true;
            }
            
            return allowElementThroughProductionFilter(element, attribute.getSupportingProductions());
        }
    }
    
    // This works for both the datamap element tree and the datamap production list 
    private class ValueFilter extends ViewerFilter
    {
        private List<ISoarDatamapAttribute> attributePath; 
        private ISoarDatamapValue originalValue;
        
        public ValueFilter(List<ISoarDatamapAttribute> attributePath, ISoarDatamapValue value)
        {
            this.attributePath = attributePath;
            this.originalValue = value;
        }
        
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element)
        {
            // TODO: This is (most-likely) super-inefficient
            ISoarDatamapAttribute attribute = findCurrentAttributeFromOldPath(attributePath);
            if (attribute == null)
            {
                // Essentially stop filtering by allowing everything
                return true;
            }
            
            ISoarDatamapValue actualValue = null;
            
            for (ISoarDatamapValue v : attribute.getTarget().getValues() )
            {
                if (v.toString().equals(originalValue.toString()))
                {
                    actualValue = v;
                    break;
                }
            }
            
            if (actualValue == null)
            {
                // Essentially stop filtering by allowing everything
                return true;
            }
            
            return allowElementThroughProductionFilter(element, actualValue.getSupportingProductions());
        }
    }
        
    private String buildFilterDescription(ISoarDatamapAttribute attribute)
    {
        ISoarAgent agent = attribute.getDatamap().getAgent();
        String agentName = (agent == null) ? "" : agent.getName() + ": ";
        
        return agentName + "^" + attribute.getName();        
    }

    private String buildFilterDescription(ISoarDatamapAttribute attribute, ISoarDatamapValue value)
    {
        String piped = "|" + value.toString() + "|";
        
        String result = buildFilterDescription(attribute) + " ";
        result += (value.toString().contains(" ")) ? piped : value.toString();
        
        return result; 
    }

    private class AttributeFilterAction extends Action
    {
        private String description;
        private List<ISoarDatamapAttribute> attributePath; 
        
        public AttributeFilterAction(ISoarDatamapAttribute attribute)
        {
            attributePath = SoarDatamapTools.getPathToNode(attribute.getTarget());
            
            description = buildFilterDescription(attribute);
            setText("Attribute " + description);
        }
        
        public void run()
        {
            view.setFilter(new AttributeFilter(attributePath), description);
        }
    }
    
    private class ValueFilterAction extends Action
    {
        private String description;
        private ISoarDatamapValue value;
        private List<ISoarDatamapAttribute> attributePath; 

        
        public ValueFilterAction(ISoarDatamapAttribute attribute, ISoarDatamapValue value)
        {
            this.attributePath = SoarDatamapTools.getPathToNode(attribute.getTarget());
            this.value = value;
            
            description = buildFilterDescription(attribute, value);
            setText("Value " + description);
        }
        
        public void run()
        {
            view.setFilter(new ValueFilter(attributePath, value), description);
        }
    }


}
