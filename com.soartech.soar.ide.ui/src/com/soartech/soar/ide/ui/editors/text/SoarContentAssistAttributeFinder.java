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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.soartech.soar.editor.docs.SoarDocs;
import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapValue;
import com.soartech.soar.ide.core.model.datamap.SoarDatamapTools;

public class SoarContentAssistAttributeFinder
{
    private final static String AttributeNameStandIn = "__temporary_soar_ide_attribute__";
    private final static String AttributeValueStandIn = "__temporary_soar_ide_value__";

    private static class PartialProductionResult
    {
        public String correctedProduction;
        public String attributeBase;

        public PartialProductionResult(String corrected, String attribute)
        {
            correctedProduction = corrected;
            attributeBase = attribute;
        }
    }

    private static PartialProductionResult completePartialProduction(String productionSource, int cursorOffset)
    {
        String preOffset = productionSource.substring(0, cursorOffset);
        String postOffset = productionSource.substring(cursorOffset, productionSource.length());

        // Decide whether we're in the middle of a possibly-dotted attribute
        int caretBeforeOffset = preOffset.lastIndexOf("^");
        if (caretBeforeOffset == -1) return null;

        String caretToOffset = preOffset.substring(caretBeforeOffset, preOffset.length());

        boolean insideAttribute = true;
        for (int i = 0; i < caretToOffset.length(); ++i)
        {
            if (Character.isWhitespace(caretToOffset.charAt(i)))
            {
                insideAttribute = false;
                break;
            }
        }

        if (!insideAttribute) return null;

        // Trim the current offset location down to last caret or dot
        int lastAttributeDelimiter = 0;
        lastAttributeDelimiter = Math.max(lastAttributeDelimiter, caretToOffset.lastIndexOf("^"));
        lastAttributeDelimiter = Math.max(lastAttributeDelimiter, caretToOffset.lastIndexOf("."));

        // Start forming a composite production out of scooped-out production
        // pieces between last caret/dot and end-of-attribute marker
        String firstHalf = productionSource.substring(0, caretBeforeOffset);
        String attributeBase = caretToOffset.substring(0, lastAttributeDelimiter + 1);

        StringBuilder builder = new StringBuilder();
        builder.append(firstHalf);
        builder.append(attributeBase);
        builder.append(AttributeNameStandIn + ' ');
        builder.append(AttributeValueStandIn + ')');

        boolean arrowFoundAtAll = (productionSource.indexOf("-->") != -1);
        boolean arrowToRightOfOffset = (postOffset.indexOf("-->") != -1);
        if (arrowToRightOfOffset || !arrowFoundAtAll) builder.append("\n-->\n");

        builder.append("\n(write |at least one RHS action required|)\n");

        // We're post TCL-parse so we can assume braces
        builder.append('}');

        return new PartialProductionResult(builder.toString(), attributeBase);
    }

    public static SoarCompletionProcessor.ProposalInfo[][] getPossibleAttributes(ISoarDatamap agentDatamap, String productionSource, int cursorOffset)
    {
        if (productionSource == null) return null;
        if (productionSource.equals("sp null")) return null;

        PartialProductionResult corrected = completePartialProduction(productionSource, cursorOffset);
        if (corrected == null) return null;

        ISoarProduction reparsedProduction = SoarCorePlugin.getDefault().parseProduction(corrected.correctedProduction);

        // Add our newly corrected production to an empty datamap so our search space is as small as possible
        ISoarDatamap productionDatamap = SoarCorePlugin.getDefault().createEmptyDatamap();
        productionDatamap.addProduction(reparsedProduction);

        // Search the datamap for the stand-in attribute
        Set<ISoarDatamapAttribute> standInAttributes = SoarDatamapTools.findAttributesByName(productionDatamap, AttributeNameStandIn);
        if (standInAttributes.size() != 1) return null;

        // Grab the path to the parent of the stand-in
        ISoarDatamapNode standInParent = standInAttributes.iterator().next().getSource();
        List<ISoarDatamapAttribute> pathAttributes = SoarDatamapTools.getPathToNode(standInParent);

        StringBuilder fullPath = new StringBuilder();
        fullPath.append("state");

        // Drill it down into something we can use to search the real datamap
        // (and build the fully-qualified name of the attribute while we're at it)
        String[] path = new String[pathAttributes.size()];
        for (int i = 0; i < pathAttributes.size(); ++i)
        {
            if (pathAttributes.get(i) == null)
            {
                fullPath.append(".<...>");
                path[i] = null;
            }
            else
            {
                fullPath.append('.');
                fullPath.append(pathAttributes.get(i).getName());
                path[i] = pathAttributes.get(i).getName();
            }
        }

        fullPath.append(".");

        TreeSet<SoarCompletionProcessor.ProposalInfo> attributeProposals = new TreeSet<SoarCompletionProcessor.ProposalInfo>();
        if (path.length > 0)
        {
            Set<ISoarDatamapAttribute> standInParents = agentDatamap.getElements(path, true);

            // Build the list of sibling proposals
            for (ISoarDatamapAttribute parentAttribute : standInParents)
            {
                attributeProposals.addAll(getAttributeProposals(corrected.attributeBase, parentAttribute.getTarget().getAttributes(), fullPath.toString()));
            }
        }
        else
        {
            // Just add proposals for attributes at the top-level
            Set<ISoarDatamapAttribute> topLevelAttributes = agentDatamap.getState().getAttributes();
            attributeProposals.addAll(getAttributeProposals(corrected.attributeBase, topLevelAttributes, fullPath.toString()));
        }

        return new SoarCompletionProcessor.ProposalInfo[][] { attributeProposals.toArray(new SoarCompletionProcessor.ProposalInfo[attributeProposals.size()]) };
    }

    private static Set<SoarCompletionProcessor.ProposalInfo> getAttributeProposals(String prefix, Set<ISoarDatamapAttribute> attributes, String fullPath)
    {
        TreeSet<SoarCompletionProcessor.ProposalInfo> proposals = new TreeSet<SoarCompletionProcessor.ProposalInfo>();

        for (ISoarDatamapAttribute att : attributes)
        {
            String name = att.getName();
            if (name == null) continue;


            StringBuilder infoWindow = new StringBuilder();
            infoWindow.append(fullPath + name);

            StringBuilder valueList = new StringBuilder();

            Set<ISoarDatamapValue> values = att.getTarget().getValues();
            for (ISoarDatamapValue value : values)
            {
                if (value.toString() == null) continue;
                if (value.toString().trim().length() == 0) continue;

                valueList.append(value.toString());
                valueList.append("\n");
            }

            if (valueList.length() > 0)
            {
                infoWindow.append("\n\nPrevious values:\n");
                infoWindow.append(valueList);
            }

            infoWindow.append("\n\nUsed in:\n");
            for (ISoarProduction production : att.getSupportingProductions())
            {
                String productionName = production.getProductionName();
                if (productionName == null) continue;
                if (productionName.trim().length() == 0) continue;

                infoWindow.append(productionName);
                infoWindow.append("\n");
            }

            // Update the context information panel property store
            // with the information we've found
            SoarDocs.getInstance().setProperty(name, infoWindow.toString());

            SoarCompletionProcessor.ProposalInfo proposal = new SoarCompletionProcessor.ProposalInfo(prefix + name, name, name);
            proposals.add(proposal);
        }

        return proposals;
    }

}
