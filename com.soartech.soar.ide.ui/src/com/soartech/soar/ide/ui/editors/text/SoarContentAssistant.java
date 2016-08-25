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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import com.soartech.soar.editor.docs.SoarDocs;
import com.soartech.soar.ide.core.model.IExpandedTclCode;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ITclHelpModel;
import com.soartech.soar.ide.core.model.ITclProcedure;
import com.soartech.soar.ide.core.model.ITclProcedureHelp;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.tcl.TclAstNode;
import com.soartech.soar.ide.core.tcl.TclParser;
import com.soartech.soar.ide.ui.SoarEditorPluginImages;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.editors.text.rules.CommandRule;
import com.soartech.soar.ide.ui.editors.text.rules.FunctionRule;
import com.soartech.soar.ide.ui.editors.text.rules.TclVariableRule;
import com.soartech.soar.ide.ui.editors.text.rules.VariableRule;

/**
 * <code>SoarContentAssistant</code> supports pop-up of available commands
 * and functions when the Ctrl + space is entered.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2006-08-24 13:03:28 -0400 (Thu, 24 Aug
 *          2006) $
 */
public class SoarContentAssistant
extends ContentAssistant {

    private SoarCompletionProcessor processor = null;

    /**
     * Constructor for a <code>SoarContentAssistant</code> object.
     *
     * @param configuration The current <code>SoarSourceEditorConfiguration</code>
     */
    public SoarContentAssistant( SoarSourceEditorConfiguration configuration ) {

        super();
        this.setContentAssistProcessor( processor = new SoarCompletionProcessor(
                configuration ),
                IDocument.DEFAULT_CONTENT_TYPE );
        this.enableAutoActivation(true);
        this.setProposalPopupOrientation( IContentAssistant.PROPOSAL_OVERLAY );
        this.setInformationControlCreator( new SoarInformationControlCreator() );
    }

    /**
     * Provides access by JUnit tests to the lastWord method of the
     * <code>SoarCompletionProcessor</code> associated with this
     * <code>SoarContentAssistant</code>. This method is not intended to
     * be accessed outside of JUnit.
     *
     * @param viewer The text viewer containing all the text
     * @param offset The offset into the text for which to begin the search
     * @return The String before the offset
     */
    public String _testGetProcessorLastWord( ITextViewer viewer, int offset ) {
        return processor.lastWord( viewer.getDocument(), offset );
    }

    /**
     * Provides access by JUnit tests to the getProposals method of the
     * <code>SoarCompletionProcessor</code> associated with this
     * <code>SoarContentAssistant</code>. This method is not intended to
     * be accessed outside of JUnit.
     *
     * @param offset The offset into the text for which to begin the search
     * @param last The last word typed (as might be returned from lastWord)
     * @param allRules A 2 dimensional <code>String</code> array of rules.
     * @return A list of <code>ICompletionProposal</code>s that meet the
     *         given criteria.
     */
    public ArrayList<ICompletionProposal> _testGetProcessorProposals( int offset,
            String last,
            String[][] allRules ) {

        return processor.getProposals( offset, last, SoarCompletionProcessor.proposalInfoFromDoubleStringArray(allRules), null );
    }
}

/**
 * <code>SoarCompletionProcessor</code> controls when the content assist
 * should pop-up and what values it contains.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2006-08-24 13:03:28 -0400 (Thu, 24 Aug
 *          2006) $
 */
class SoarCompletionProcessor
extends TemplateCompletionProcessor {

    private static final String DEFAULT_IMAGE = "$nl$/icons/template.gif";
    private SoarSourceEditorConfiguration   configuration = null;

    /**
     * Constructor for a <code>SoarCompletionProcessor</code> object.
     *
     * @param configuration The associated <code>SoarSourceEditorConfiguration</code>
     */
    public SoarCompletionProcessor( SoarSourceEditorConfiguration configuration ) {
        this.configuration = configuration;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    //TODO: Find out what files the autocomplete is indexing that might make it slow
    public ICompletionProposal[] computeCompletionProposals( ITextViewer viewer, int offset )
    {
        synchronized(configuration.getEditor().getWorkingCopyLock())
        {
            IDocument doc = viewer.getDocument();
            String last = lastWord( doc, offset );

            ArrayList<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

            boolean insideProduction = false;
            try
            {
                ISoarFile workingCopy = getWorkingCopy();
                if(workingCopy == null)
                {
                    return proposals.toArray( new ICompletionProposal[proposals.size()] );
                }
                ISoarAgent agent = getAgent(workingCopy);
                
                if(agent != null)
                {
                    proposals.addAll(getProposals(offset, last, getTclProcedureProposals(last, agent), SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_PROCEDURE)));
                }

                
                ISoarBuffer buffer = workingCopy.getBuffer();
                
                TclAstNode node = getProductionNode(buffer, offset);
                if (node != null && node.getType() == TclAstNode.COMMAND)
                {
                    List<TclAstNode> words = node.getWordChildren();
                    
                    if (words.size() >= 2)
                    {
                        TclAstNode nameWord = words.get(0);
                        String name = buffer.getText(nameWord.getStart(), nameWord.getLength());

                        if (name.equals("sp"))
                        {
                            TclAstNode bodyWord = words.get(1);
                            String body = buffer.getText(bodyWord.getStart(), bodyWord.getLength());

                            ProductionInfo production = new ProductionInfo(body, offset - bodyWord.getStart());
                            if (agent != null && bodyWord.getType() != TclAstNode.BRACED_WORD)
                            {
                                production = expandProductionBody(agent, production);
                            }

                            // The following require a complete sp-prefixed Soar production, so we
                            // need to add that back in.
                            final String ProductionHeader = "sp ";
                            production.source = ProductionHeader + production.source;
                            production.localOffset += ProductionHeader.length();

                            insideProduction = (production.source != null);
                            if (insideProduction)
                            {
                                proposals.addAll(getProposals(offset, last, getPossibleVariables(production.source), SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_VARIABLE)));
                                if(agent != null)
                                {
                                    proposals.addAll(getProposals(offset, last, getPossibleAttributes(agent, production), SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_ATTRIBUTE)));
                                }
                            }
                        }
                    }
                }
            }
            catch (SoarModelException e)
            {
                e.printStackTrace();
            }

            if (!insideProduction)
            {
                ICompletionProposal[] templateProposals = super.computeCompletionProposals( viewer, offset );
                for (ICompletionProposal proposal : templateProposals)
                {
                    if (proposal.getDisplayString().toLowerCase().indexOf(last) != 0) continue;
                    proposals.add(proposal);
                }

                proposals.addAll(getProposals(offset, last, proposalInfoFromDoubleStringArray(CommandRule.ALL_COMMANDS), SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_COMMAND)));
            }

            proposals.addAll(getProposals(offset, last, proposalInfoFromDoubleStringArray(FunctionRule.ALL_FUNCTIONS), SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_FUNCTION)));

            TclVariableRule tclVariableRule = configuration.getKeywordScanner().getTclVariableRule();
            proposals.addAll(getProposals(offset, last, proposalInfoFromDoubleStringArray(tclVariableRule.getAllTclVariables()), SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_TCL_VARIABLE)));

            return proposals.toArray( new ICompletionProposal[proposals.size()] );
        }

    }
    
    private ISoarFile getWorkingCopy()
    {
        return configuration.getEditor().getSoarFileWorkingCopy();
    }
    
    private ISoarAgent getAgent(ISoarFile workingCopy)
    {
        ISoarAgent agent = null;
        if(workingCopy != null)
        {
            try
            {
                ISoarFileAgentProxy proxy = workingCopy.getPrimaryAgentProxy();
                if(proxy != null)
                {
                    agent = proxy.getAgent();
                }
            }
            catch (SoarModelException e)
            {
                SoarEditorUIPlugin.log(e);
            }
        }
        return agent;

    }

    private TclAstNode getProductionNode(ISoarBuffer buffer, int offset)
    {
        TclParser parser = new TclParser();
        parser.setInput(buffer.getCharacters(), 0, buffer.getLength());
        TclAstNode root = parser.parse();
        for(TclAstNode node : root.getChildren())
        {
            if(node.containsOffset(offset))
            {
                return node;
            }
        }
        return null;
    }

    private class ProductionInfo
    {
        public ProductionInfo(String source, int localOffset)
        {
            this.source = source;
            this.localOffset = localOffset;
        }

        public String source;

        /** The local-translated offset in the single-production source string */
        public int localOffset;
    }

    /** Returns null if there was a problem
     * @throws SoarModelException */
    private ProductionInfo expandProductionBody(ISoarAgent agent, ProductionInfo info) throws SoarModelException
    {
        // We need to first expand TCL only up to the offset position to find out much
        // the TCL expansion is going to affect the offset itself.
        String preOffsetSource = info.source.substring(0, info.localOffset) + '"';

        IExpandedTclCode preOffset = agent.expandTclString("::", preOffsetSource, 0);
        int adjustedOffset = preOffset.getResultString().length() + 1;

        // Now we expand the whole thing
        IExpandedTclCode code = agent.expandTclString("::", info.source, 0);

        // Replace the now-missing leading and trailing quotes with braces
        String result = null;
        if (code.getError() == null) result = "{" + code.getResultString() + "}";

        return new ProductionInfo(result, adjustedOffset);
    }

    private ProposalInfo[][] getPossibleAttributes(ISoarAgent agent, ProductionInfo info)
    {
        ISoarDatamap datamap = agent.getDatamap();

        return SoarContentAssistAttributeFinder.getPossibleAttributes(datamap, info.source, info.localOffset);
    }

    private ProposalInfo[][] getTclProcedureProposals(String last, ISoarAgent agent) throws SoarModelException
    {
        TreeMap<String, String> procedureNames = new TreeMap<String, String>();
        
        for (ITclProcedure procedure : agent.getAllProcedures())
        {
            procedureNames.put(procedure.getProcedureName(), procedure.getArgumentList());            
        }
        
        ProposalInfo[] procedures = new ProposalInfo[procedureNames.size()];
        int i = 0;
        for (String name : procedureNames.keySet())
        {
            System.out.println("Procedure names");
            System.out.println(name);
            ITclHelpModel helpModel = agent.getSoarModel().getTclHelpModel();
            ITclProcedureHelp help = helpModel.getHelp(name, agent.getSoarProject(), agent);
            
            String helpText = "";
            if (help != null) helpText = help.getHoverHelpText();
            
            // Update the context information panel property store
            // with the information we've found
            SoarDocs.getInstance().setProperty(name, helpText); 
            //TODO: Jacob's most requested - change this to have keyword args
            String replacementText = "";
            //Have different replacement text if the user has already typed a bracket or not
            //Also necessary for having tclProc autocomplete suggestions outside of []
            String[] argList = procedureNames.get(name).trim().split("\\s+");
            //This clears the formatting of the replacement text
            for (int k = 0; k < argList.length; ++k )
            {
                if (argList[k].startsWith("{")) 
                {
                    int j = k;
                    while (!argList[j].endsWith("}"))
                    {
                        replacementText += " " + argList[j];
                        ++j;
                    }
                    k = j;
                }
                replacementText += " " + argList[k];
            }
            if (last.startsWith("["))
            {
                replacementText = "[" + name + replacementText + "]";
            }
            else
            {
                replacementText = name + replacementText;
            }
            procedures[i++] = new ProposalInfo(replacementText, name, helpText);
        }
        
        return new ProposalInfo[][] { procedures };
    }
    
    private ProposalInfo[][] getPossibleVariables(String source)
    {
        if (source == null) { return proposalInfoFromDoubleStringArray(getAllVariablesInFile()); }

        TreeSet<String> variables = new TreeSet<String>();

        Pattern pattern = Pattern.compile("<([\\w-])+>");
        Matcher matcher = pattern.matcher(source);
        while (matcher.find())
        {
            variables.add(matcher.group());
        }
        matcher.matches();

        ProposalInfo[] result = new ProposalInfo[variables.size()];
        int i = 0;
        for (String variable : variables)
        {
            result[i++] = new ProposalInfo(variable, variable, "");
        }

        return new ProposalInfo[][] { result };
    }

    private String[][] getAllVariablesInFile()
    {
        VariableRule variableRule = configuration.getKeywordScanner().getVariableRule();
        return variableRule.getAllVariables();
    }


    public static ProposalInfo[][] proposalInfoFromDoubleStringArray(String[][] source)
    {
        ProposalInfo[][] result = new ProposalInfo[source.length][];

        for (int i = 0; i < source.length; ++i)
        {
            result[i] = new ProposalInfo[source[i].length];

            for (int j = 0; j < source[i].length; ++j)
            {
                result[i][j] = new ProposalInfo(source[i][j]);
            }
        }

        return result;
    }

    public static class ProposalInfo implements Comparable<ProposalInfo>
    {
        public ProposalInfo(String replacementValueOnly)
        {
            replacementValue = replacementValueOnly;
            replacementDisplay = replacementValueOnly;
            informationWindow = "";
        }

        public ProposalInfo(String replacementValue, String replacementDisplay, String informationWindow)
        {
            this.replacementValue = replacementValue;
            this.replacementDisplay = replacementDisplay;
            this.informationWindow = informationWindow;
        }

        public String replacementValue;
        public String replacementDisplay;
        public String informationWindow;

        public int compareTo(ProposalInfo o)
        {
            if (replacementDisplay == null)
            {
                return replacementValue.compareTo(o.replacementValue);
            }

            return replacementDisplay.compareTo(o.replacementDisplay);
        }
    }


    /*
     * (non-Javadoc) Protected so that it can be tested.
     */
    protected ArrayList<ICompletionProposal> getProposals( int offset, String last, ProposalInfo[][] allRules, Image icon )
    {
        // TODO Could be smarter about what proposals to show in the list.
        // Right now there is only filtering on letters and not placement.

        ArrayList<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

        if (allRules == null) return proposals;

        for( ProposalInfo[] rules : allRules )
        {
            for( ProposalInfo rule : rules )
            {
                //Skip any rules that don't start with the chars typed so far
                if (rule.replacementValue.toLowerCase().indexOf(last) != 0) continue;

                // Add any keywords that start with the characters of the current word being typed
                IContextInformation info = new ContextInformation(rule.informationWindow, rule.informationWindow);

                // Adjust length of replacement text to account for the fact
                // that the word is already partially complete.
                proposals.add( new CompletionProposal( rule.replacementValue, offset
                        - last.length(), last.length(), rule.replacementValue.length(), icon,
                        rule.replacementDisplay, info, rule.replacementDisplay ) );
            }
        }

        return proposals;
    }

    /**
     * Returns a LOWER-CASE string of the last word typed.
     *
     * @param doc - the document to analyze
     * @param offset - the character offset from which to p1 analyzing
     * @return the sequence of characters (word) that leads up to the offset
     *         in doc. returns null if whitespace is the most recent
     *         character.
     */
    /*
     * (non-Javadoc) Protected so that it can be tested.
     */
   protected String lastWord( IDocument doc, int offset ) {

      try {
         int n = 0;
         for( n = offset - 1; n > 0; n-- ) {
            char c = doc.getChar( n );
            if( Character.isWhitespace( c ) || c == '(' )
               return doc.get( n + 1, offset - n - 1 ).toLowerCase();
         }
            // Check the case that the 'last word' is in fact the
            // first word of the document, which we are interested in.
            if( n == 0 ) return doc.get( 0, offset ).toLowerCase();

        }
      catch( Exception e ) {
            e.printStackTrace();
        }
        return "";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
   public IContextInformation[] computeContextInformation(
                                                           ITextViewer viewer,
                                                           int offset ) {

        ArrayList<IContextInformation> information = new ArrayList<IContextInformation>();

        IDocument doc = viewer.getDocument();
        String last = lastWord( doc, offset );

        information
        .addAll( getInformation(
                last,
                CommandRule.ALL_COMMANDS,
                SoarEditorPluginImages
                .get( SoarEditorPluginImages.IMG_COMMAND ) ) );

        information
        .addAll( getInformation(
                last,
                FunctionRule.ALL_FUNCTIONS,
                SoarEditorPluginImages
                .get( SoarEditorPluginImages.IMG_FUNCTION ) ) );

        VariableRule variableRule = configuration.getKeywordScanner().getVariableRule();
        information.addAll( getInformation( last, variableRule
                .getAllVariables(), SoarEditorPluginImages
                .get( SoarEditorPluginImages.IMG_VARIABLE ) ) );

        TclVariableRule tclVariableRule = configuration.getKeywordScanner().getTclVariableRule();
        information.addAll( getInformation( last, tclVariableRule
                .getAllTclVariables(), SoarEditorPluginImages
                .get( SoarEditorPluginImages.IMG_TCL_VARIABLE ) ) );

        return information.toArray( new IContextInformation[information.size()] );
    }

    private ArrayList<IContextInformation> getInformation(
            String last,
            String[][] allRules,
            Image icon ) {

        ArrayList<IContextInformation> information = new ArrayList<IContextInformation>();

        for( String[] rules : allRules ) {
            for( String rule : rules ) {
                if( rule.toLowerCase().indexOf( last ) == 0 ) {
                    information.add( new ContextInformation( icon, rule, rule ) );
                }
            }
        }

        return information;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    public char[] getCompletionProposalAutoActivationCharacters()
    {
        return new char[] { '^', '<', '.', '[' };
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
   public char[] getContextInformationAutoActivationCharacters() {

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
     */
    public String getErrorMessage() {

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    public IContextInformationValidator getContextInformationValidator() {

        return new ContextInformationValidator( this );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getContextType(org.eclipse.jface.text.ITextViewer,
     *      org.eclipse.jface.text.IRegion)
     */
    @Override
    protected TemplateContextType getContextType( ITextViewer viewer,
            IRegion region ) {
        return SoarEditorUIPlugin.getDefault().getContextTypeRegistry()
        .getContextType( SoarContext.SOAR_CONTEXT_TYPE );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getImage(org.eclipse.jface.text.templates.Template)
     */
    @Override
    protected Image getImage( Template template ) {
        ImageRegistry registry = SoarEditorUIPlugin.getDefault()
        .getImageRegistry();
        Image image = registry.get( DEFAULT_IMAGE );
        if( image == null ) {
            ImageDescriptor desc = AbstractUIPlugin
            .imageDescriptorFromPlugin( "com.soartech.soar.ide.ui",
                    DEFAULT_IMAGE );
            registry.put( DEFAULT_IMAGE, desc );
            image = registry.get( DEFAULT_IMAGE );
        }
        return image;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getTemplates(java.lang.String)
     */
    @Override
    protected Template[] getTemplates( String contextTypeId ) {
        return SoarEditorUIPlugin.getDefault().getTemplateStore()
        .getTemplates();
    }
}
