package com.soartech.soar.ide.ui.editors.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ContextInformationValidator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal.TraversalUtil;

public class SoarDatabaseContentAssistant extends ContentAssistant {

	/**
     * Constructor for a <code>SoarContentAssistant</code> object.
     *
     * @param configuration The current <code>SoarSourceEditorConfiguration</code>
     */
    public SoarDatabaseContentAssistant(SoarDatabaseTextEditorConfiguration configuration) {
        super();
        this.setContentAssistProcessor(new SoarDatabaseCompletionProcessor(configuration), IDocument.DEFAULT_CONTENT_TYPE);
        this.enableAutoActivation(true);
        this.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
        this.setInformationControlCreator(new SoarInformationControlCreator());
    }
}

/**
 * <code>SoarDatabaseCompletionProcessor</code> controls when the content assist
 * should pop-up and what values it contains.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2006-08-24 13:03:28 -0400 (Thu, 24 Aug
 *          2006) $
 */
class SoarDatabaseCompletionProcessor extends TemplateCompletionProcessor {
	
    private static final String DEFAULT_IMAGE = "$nl$/icons/template.gif";
    private SoarDatabaseTextEditorConfiguration configuration = null;

    public SoarDatabaseCompletionProcessor( SoarDatabaseTextEditorConfiguration configuration ) {
        this.configuration = configuration;
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset)
    {
    	ArrayList<ICompletionProposal> list = new ArrayList<ICompletionProposal>();
    	String allText = viewer.getDocument().get();
    	String cursorText = allText.substring(0, offset); // text up to the cursor
    	
    	int lastWhitespace = 0;
    	for (int i = offset - 1; i > 0 && lastWhitespace == 0; --i) {
    		char c = allText.charAt(i);
    		if (characterIsWhitespaceOrPunctuation(c)) {
    			lastWhitespace = i + 1;
    		}
    	}
    	
    	String currentWord = cursorText.substring(lastWhitespace);
    	char lastChar = cursorText.length() > 0 ? cursorText.charAt(cursorText.length() - 1) : ' ';
    	
    	SoarDatabaseRow row = configuration.editor.getInput().getRow();
		assert row.getTable() == Table.RULES;
		
		// determine paren depth
		int parenDepth = 0;
		char[] cursorChars = cursorText.toCharArray();
		for (char c : cursorChars) {
			if (c == '(') {
				++ parenDepth;
			} else if (c == ')') {
				-- parenDepth;
			}
		}

		/*
		if (cursorText.trim().length() == 0) {
			String ruleName = row.getName().toLowerCase().replace(' ', '-');
			String proposal = "sp {" + ruleName + "\n   (state <s> ^\n\n}";
			CompletionProposal proposeSp = new CompletionProposal(proposal, 0, 0, 20 + ruleName.length());
			list.add(proposeSp);
		}
		*/

		HashSet<String> proposals = new HashSet<String>();
		if (parenDepth > 0) {
			proposals.addAll(findVariables(allText));
			if (lastChar != '(') {
				proposals.addAll(findAttributes(allText));
				proposals.addAll(findDatamapAttributes(row));
			}
		}
		ArrayList<String> proposalsList = new ArrayList<String>(proposals);
		Collections.sort(proposalsList);
		for (String proposal : proposalsList) {
			if (proposal.startsWith(currentWord)
					&& !proposal.equals(currentWord)) {
				String replacementString = proposal;
				int replacementOffset = lastWhitespace;
				int replacementLength = currentWord.length();
				int cursorPosition = proposal.length();
				CompletionProposal proposeSp = new CompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition);
				list.add(proposeSp);
			}
		}
		
		ICompletionProposal[] thisRet = list.toArray(new ICompletionProposal[]{});
		if (cursorText.length() == 0
				|| cursorText.charAt(cursorText.length() - 1) == '\n') {
			ICompletionProposal[] superRet = super.computeCompletionProposals(viewer, offset);
			ICompletionProposal[] ret = new ICompletionProposal[thisRet.length + superRet.length];
			System.arraycopy(thisRet, 0, ret, 0, thisRet.length);
			System.arraycopy(superRet, 0, ret, thisRet.length, superRet.length);
			return ret;
		}
		return thisRet;
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
    
    private ArrayList<String> findVariables(String text) {
		HashSet<String> hash = new HashSet<String>();
		Pattern pattern = Pattern.compile("<([\\w-])+>");
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			hash.add(matcher.group());
		}
		ArrayList<String> ret = new ArrayList<String>(hash);
		return ret;
	}
    
    private ArrayList<String> findAttributes(String text) {
		HashSet<String> hash = new HashSet<String>();
		Pattern pattern = Pattern.compile("\\^([\\w-]+)[\\s\\.]");
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			String group = matcher.group(1);
			hash.add(group);
		}
		pattern = Pattern.compile("\\.([\\w-]+)[\\s\\.]");
		matcher = pattern.matcher(text);
		while (matcher.find()) {
			String group = matcher.group(1);
			hash.add(group);
		}
		ArrayList<String> ret = new ArrayList<String>(hash);
		return ret;
	}
    
    private ArrayList<String> findDatamapAttributes(SoarDatabaseRow row) {
    	ArrayList<String> ret = new ArrayList<String>();
    	
    	ArrayList<ISoarDatabaseTreeItem> problemSpaces = TraversalUtil.getRelatedProblemSpaces(row);
    	
    	HashSet<Table> datamapAttributeTypes = new HashSet<Table>();
    	datamapAttributeTypes.add(Table.DATAMAP_ENUMERATIONS);
    	datamapAttributeTypes.add(Table.DATAMAP_FLOATS);
    	datamapAttributeTypes.add(Table.DATAMAP_IDENTIFIERS);
    	datamapAttributeTypes.add(Table.DATAMAP_INTEGERS);
    	datamapAttributeTypes.add(Table.DATAMAP_STRINGS);
    	
    	for (ISoarDatabaseTreeItem item : problemSpaces) {
    		if (item instanceof SoarDatabaseRow) {
    			SoarDatabaseRow ps = (SoarDatabaseRow) item;
    			assert ps.getTable() == Table.PROBLEM_SPACES;
    			ArrayList<ISoarDatabaseTreeItem> attributes = ps.getDescendantsOfTypes(datamapAttributeTypes);
    			for (ISoarDatabaseTreeItem attrItem : attributes) {
    				assert attrItem instanceof SoarDatabaseRow;
    				SoarDatabaseRow attribute = (SoarDatabaseRow) attrItem;
    				assert datamapAttributeTypes.contains(attribute.getTable());
    				ret.add(attribute.getName());
    			}
    		}
    	}
    	
    	return ret;
    }
   
   private boolean characterIsWhitespaceOrPunctuation(Character c) {
	   final Character[] punctuation = { '(', ')', '.', '^' };
	   HashSet<Character> hash = new HashSet<Character>();
	   for (Character ch : punctuation) {
		   hash.add(ch);
	   }
	   return (Character.isWhitespace(c) || hash.contains(c));
   }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    public char[] getCompletionProposalAutoActivationCharacters()
    {
        return new char[] { '^', '<', '.', '[' , '('};
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
    protected TemplateContextType getContextType( ITextViewer viewer, IRegion region ) {
		Table configurationTable = configuration.getRow().getTable();
		if (configurationTable == Table.RULES) {
			TemplateContextType context = SoarEditorUIPlugin.getDefault().getContextTypeRegistry().getContextType(SoarRuleContext.SOAR_CONTEXT_TYPE);
			if (context instanceof SoarRuleContext) {
				((SoarRuleContext) context).setConfiguration(configuration);
			}
			return context;
		} else if (configurationTable == Table.OPERATORS) {
			TemplateContextType context = SoarEditorUIPlugin.getDefault().getContextTypeRegistry().getContextType(SoarOperatorContext.SOAR_CONTEXT_TYPE);
			if (context instanceof SoarOperatorContext) {
				((SoarOperatorContext) context).setConfiguration(configuration);
			}
			return context;
		}
		return null;
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
        return SoarEditorUIPlugin.getDefault().getTemplateStore().getTemplates();
    }
}
