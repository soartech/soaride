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

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;

import com.soartech.soar.ide.ui.editors.text.rules.ArrowRule;
import com.soartech.soar.ide.ui.editors.text.rules.BlockCommentRule;
import com.soartech.soar.ide.ui.editors.text.rules.BraceRule;
import com.soartech.soar.ide.ui.editors.text.rules.CommandRule;
import com.soartech.soar.ide.ui.editors.text.rules.DisjunctionRule;
import com.soartech.soar.ide.ui.editors.text.rules.FlagRule;
import com.soartech.soar.ide.ui.editors.text.rules.FunctionRule;
import com.soartech.soar.ide.ui.editors.text.rules.InlineCommentRule;
import com.soartech.soar.ide.ui.editors.text.rules.PreprocessedTclRule;
import com.soartech.soar.ide.ui.editors.text.rules.StringRule;
import com.soartech.soar.ide.ui.editors.text.rules.TclRule;
import com.soartech.soar.ide.ui.editors.text.rules.TclVariableRule;
import com.soartech.soar.ide.ui.editors.text.rules.VariableRule;

/**
 * <code>SoarRuleScanner</code> sets the rules for the <code>SoarEditor</code> 
 * and defines the colors that will be used in the damage/repair class.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2009-06-22 13:05:30 -0400 (Mon, 22 Jun 2009) $
 */
public class SoarRuleScanner extends RuleBasedScanner {

    private CommandRule commandRule = null;
    private VariableRule variableRule = new VariableRule();
    private TclVariableRule tclVariableRule = null;
    
    /**
     * Constructor for a <code>SoarRuleScanner</code> object.
     * @param The associated <code>SoarSourceEditorConfiguration</code>
     */
    public SoarRuleScanner() {

        super();
        initializeRules();
                
    }

    /**
     * @return the tclVariableRule
     */
    public TclVariableRule getTclVariableRule()
    {
        return tclVariableRule;
    }

    /**
     * @return the variableRule
     */
    public VariableRule getVariableRule()
    {
        return variableRule;
    }

    public void initializeRules() {
    	
    	setDefaultReturnToken(
                new Token( new TextAttribute( 
                      SyntaxColorManager.getForegroundColor() ) )
        );    	
        
        // Add rules to the ruleList
    	// NOTE: ORDERING MATTERS HERE
        ArrayList<IRule> ruleList = new ArrayList<IRule>();
        ruleList.add( commandRule = new CommandRule() );
        
        
        ruleList.add( new BlockCommentRule() );
        // TODO: DisjunctionRule must be added before VariableRule because
        // of some ambiguity issues.  Should probably figure out where
        // the conflict occurs.
        ruleList.add( new DisjunctionRule() );
        ruleList.add( variableRule = new VariableRule() );
        ruleList.add(new MultiLineRule( "${", "}", new Token(new TextAttribute( SyntaxColorManager.getTclVarColor(), null, SWT.NORMAL ) ) ));
        ruleList.add( tclVariableRule = new TclVariableRule() );
        ruleList.add( new InlineCommentRule() );
        ruleList.add( new BraceRule() );
        ruleList.add( new ArrowRule() );
        ruleList.add( new FunctionRule() );
        ruleList.add( new TclRule() );
        ruleList.add( new StringRule() );
        ruleList.add( new PreprocessedTclRule() );
        ruleList.add( new FlagRule() );
        
        IRule[] rules = new IRule[ ruleList.size() ];
        ruleList.toArray( rules );

        setRules( rules );
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.rules.RuleBasedScanner#read()
     */
    @Override
    public int read() {
        
        int nextChar = super.read();
        if ( nextChar == -1 )
            commandRule.resetDetector();
        return nextChar;
    }
}
