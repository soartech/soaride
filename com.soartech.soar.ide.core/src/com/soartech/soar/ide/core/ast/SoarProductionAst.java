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
package com.soartech.soar.ide.core.ast;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Brad Jones
 * @version 0.75 3 Mar 2000
 */
public final class SoarProductionAst {
	// Data Members
	private String d_name = "";
	private int d_startLine;
	private String d_comment = "";
	private String d_productionType = "";
	private List<Condition> conditions = new ArrayList<Condition>();
    private List<Action> actions = new ArrayList<Action>();
    
	// Constructors
	public SoarProductionAst() {}
		
	// Accessors
	public final void setName(String name) {
		d_name = name;
	}
	
	public final void setComment(String comment) {
		d_comment = comment;
	}
	
	public final void setProductionType(String productionType) {
		d_productionType = productionType;
	}
	
	public final void setStartLine(int startLine) {
		d_startLine = startLine;
	}
	
	public final void addCondition(Condition c) {
        conditions.add(c);
	}

    public final void addAction(Action a) {
        actions.add(a);
    }
    
	public final int getStartLine() {
		return d_startLine;
	}
	
	public final String getName() {
		return d_name;
	}
	
	public final String getComment() {
		return d_comment;
	}
	
	public final String getProductionType() {
		return d_productionType;
	}
	
    /**
     * @return the actions
     */
    public List<Action> getActions()
    {
        return actions;
    }

    /**
     * @return the conditions
     */
    public List<Condition> getConditions()
    {
        return conditions;
    }

    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return d_name + ":" + d_productionType + "\n" + conditions + "\n" + actions;
    }

    public static void main(String args[])
    {
        String input = "" +
        "test (state <s> -^attribute) -{ (<s> ^hi) } --> " +
        "(interrupt)";
        
        SoarParserTokenManager tokMgr = new SoarParserTokenManager(new SoarCharStream(new StringReader(input), 0));
        SoarParser parser = new SoarParser(tokMgr);
        SoarProductionAst ast;
        try
        {
            ast = parser.soarProduction();
            System.out.println(ast);
            
            TriplesExtractor extractor = new TriplesExtractor(ast);
            extractor.sortTriples(new ArrayList<String>());
            System.out.println(extractor.triples());
            System.out.println(extractor.variables());
        }
        catch (ParseException e)
        {
            System.err.println(e);
            //SoarToken token = (SoarToken) e.currentToken;
            //System.err.println(token.beginOffset + ", " + token.endOffset);
        }
            
    }
}
