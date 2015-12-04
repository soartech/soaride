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
package com.soartech.soar.ide.core.model.impl;

import java.io.StringReader;
import java.util.List;

import com.soartech.soar.ide.core.model.BasicSoarSourceRange;
import com.soartech.soar.ide.core.model.IExpandableElement;
import com.soartech.soar.ide.core.model.IExpandedTclCode;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ITclComment;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarProblem;
import com.soartech.soar.ide.core.model.ast.Action;
import com.soartech.soar.ide.core.model.ast.AttributeValueMake;
import com.soartech.soar.ide.core.model.ast.AttributeValueTest;
import com.soartech.soar.ide.core.model.ast.Condition;
import com.soartech.soar.ide.core.model.ast.ConditionForOneIdentifier;
import com.soartech.soar.ide.core.model.ast.CustomSoarParserTokenManager;
import com.soartech.soar.ide.core.model.ast.FunctionCall;
import com.soartech.soar.ide.core.model.ast.ParseException;
import com.soartech.soar.ide.core.model.ast.PositiveCondition;
import com.soartech.soar.ide.core.model.ast.RHSValue;
import com.soartech.soar.ide.core.model.ast.SoarCharStream;
import com.soartech.soar.ide.core.model.ast.SoarParser;
import com.soartech.soar.ide.core.model.ast.SoarProductionAst;
import com.soartech.soar.ide.core.model.ast.Token;
import com.soartech.soar.ide.core.model.ast.TokenMgrError;
import com.soartech.soar.ide.core.model.ast.ValueMake;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.SoarDatamapAdditionResult;
import com.soartech.soar.ide.core.model.impl.datamap.SoarDatamap;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;
import com.soartech.soar.ide.core.model.impl.serialization.ProductionMemento;
import com.soartech.soar.ide.core.model.impl.serialization.SourceRangeMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;
import com.soartech.soar.ide.core.tcl.TclAstNodeSourceRange;

/**
 * Implementation of ISoarProduction interface.
 * 
 * @author ray
 */
public class SoarProduction extends TclCommand implements ISoarProduction, IExpandableElement
{
    private TclAstNode bodyNode;
    private boolean bodyInBraces = false;
    private ISoarSourceRange bodyRange;
    private ISoarSourceRange nameRange;
    private String name = "";
    private SoarProductionAst ast;
    
    public SoarProduction(SoarFileAgentProxy parent, ISoarProblemReporter reporter, TclAstNode astNode) throws SoarModelException
    {
        super(parent, astNode);
        
        List<TclAstNode> words = getTclSyntaxTree().getWordChildren();
        if(words.size() >= 2)
        {
            // Here they typed "sp X ..."
            bodyNode = words.get(1);
            bodyInBraces = bodyNode.getType() == TclAstNode.BRACED_WORD;
            bodyRange = new TclAstNodeSourceRange(bodyNode);
            String bodySource = getSource(bodyRange);
            extractProductionName(bodySource);
            
            // Report an error if there are too many args to sp. We'll still 
            // try to parse the body though, just for fun.
            if(words.size() > 2)
            {
                String extraArg = getSource(new TclAstNodeSourceRange(words.get(2)));
                extraArg = extraArg != null ? extraArg.trim() : "";
                // If the extra argument starts with #, then it's just a trailing comment which
                // Soar is fine with. (bug #1461)
                if(!extraArg.startsWith("#"))
                {
                    reporter.report(SoarProblem.createError(
                            "Too many arguments to sp command. Expected 1, got " + (words.size() - 1) + 
                            ".\nDon't forget that the production name goes *inside* the braces.", 
                            words.get(0).getStart(), words.get(0).getLength()));
                }
            }
            
            String parseableBody = getParseableBody(reporter);
            if(parseableBody.length() > 0)
            {
                parseProductionBody(reporter, parseableBody);
            }
            
        }
        else
        {
            // Here they only typed "sp" with no arguments.
            assert words.size() == 1;
            reporter.report(SoarProblem.createError("Production has no body", 
                    words.get(0).getStart(), words.get(0).getLength()));
        }
    }
    
    public SoarProduction(SoarFileAgentProxy file, ProductionMemento memento) throws SoarModelException
    {
        super(file, memento);
        
        this.ast = null;
        this.bodyInBraces = memento.isBodyInBraces();
        this.bodyNode = null;
        this.bodyRange = new BasicSoarSourceRange(memento.getBodyRange());
        this.name = memento.getProductionName();
        this.nameRange = new BasicSoarSourceRange(memento.getProductionNameRange());
    }
    
    public SoarFileAgentProxy getSoarFileProxy()
    {
        return getSoarFile();
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#detach()
     */
    @Override
    protected void detach()
    {
        if(!getSoarFile().isWorkingCopy())
        {
            ISoarAgent agent = getAgent();
            if(agent != null)
            {
                ISoarDatamap datamap = agent.getDatamap();
                datamap.removeProduction(this);
            }
        }
        super.detach();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.TclCommand#createMemento()
     */
    @Override
    public ElementMemento createMemento()
    {
        return saveState(new ProductionMemento());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.TclCommand#saveState(com.soartech.soar.ide.core.model.impl.serialization.Mementos.Element)
     */
    @Override
    protected ElementMemento saveState(ElementMemento memento)
    {
        super.saveState(memento);
        
        ProductionMemento prodMemento = (ProductionMemento) memento;
        prodMemento.setBodyInBraces(bodyInBraces);
        prodMemento.setProductionName(name);
        prodMemento.setProductionNameRange(new SourceRangeMemento(nameRange));
        prodMemento.setBodyRange(new SourceRangeMemento(bodyRange));
        
        return memento;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProduction#getExpandedSource()
     */
    public String getExpandedSource() throws SoarModelException
    {
        if(bodyInBraces)
        {
            return getSource();
        }
        
        SoarAgent agent = getAgent();
        ExpandedProductionInfo info = agent.getExpandedProductionBody(name);
        String namespace = info != null ? info.namespace : "::";
        if(!getSoarFile().isWorkingCopy())
        {
            if(info != null)
            {
                return rebuildFromExpandedBody(info.expandedBody);
            }
        }
        
        // Well, let's see if we can expand it manually...
        String bodySource = getSource(bodyRange);
        
        IExpandedTclCode code = agent.expandTclString(namespace, bodySource, bodyRange.getOffset());

        if(code.getError() != null)
        {
            //return code.getResultString();
        	return code.getError().getMessage();
        }
        return rebuildFromExpandedBody(code.getResultString());
    }
    
    private String rebuildFromExpandedBody(String body) throws SoarModelException
    {
        StringBuilder builder = new StringBuilder();
        ITclComment comment = getAssociatedComment();
        if(comment != null)
        {
            builder.append(comment.getSource());
            //builder.append("\n");
        }
        builder.append("sp {");
        builder.append(body);
        builder.append("}");
        return builder.toString();
    }

    private void extractProductionName(String bodySource) throws SoarModelException
    {
        if(bodySource.length() == 0)
        {
            return;
        }
        int offset = 0;
        if(bodySource.charAt(offset) == '"' || bodySource.charAt(offset) == '{')
        {
            ++offset;
        }
        while(offset < bodySource.length())
        {
            char c = bodySource.charAt(offset);
            if(!Character.isWhitespace(c))
            {
                break;
            }
            ++offset;
        }
        int nameStart = offset;
        while(offset < bodySource.length())
        {
            char c = bodySource.charAt(offset);
            if(Character.isWhitespace(c))
            {
                break;
            }
            ++offset;
        }
        if(nameStart == offset)
        {
            return;
        }
        
        nameRange = new BasicSoarSourceRange(bodyRange.getOffset() + nameStart, 
                                             offset - nameStart);
        name = getSource(nameRange);
    }
    
    private static boolean isAllWhiteSpace(String s)
    {
        for(int i = 0; i < s.length(); ++i)
        {
            if(!Character.isWhitespace(s.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }
    
    private SoarAgent getAgent()
    {
        return (SoarAgent) getSoarFile().getAgent();
    }
    
    /**
     * Add this production to the datamap and process any resulting errors
     * or warnings. In the case of a working copy, a temporary private datamap
     * is used rather than the project.
     * 
     * @throws SoarModelException
     */
    private void addToDatamap(ISoarProblemReporter reporter) throws SoarModelException
    {
        ISoarDatamap datamap = null;
        if(!getSoarFile().isWorkingCopy())
        {
            datamap = getAgent().getDatamap();
        }
        else
        {
            datamap = new SoarDatamap(); // temporary datamap
        }
        
        SoarDatamapAdditionResult result = datamap.addProduction(this);
        if(result.disconnectedVariables.isEmpty() && 
           result.unboundRhsFunctionVariables.isEmpty())
        {
            return;
        }
        
        String body = getSoarFile().getBuffer().getText(bodyRange.getOffset(), bodyRange.getLength());
        
        for(String var : result.disconnectedVariables)
        {
            ISoarSourceRange range = getVariableRange(body, var);
            reporter.report(SoarProblem.createError("Variable " + var + " is not connected to state", 
                            range.getOffset(), range.getLength()));
        }
        
        for(String var : result.unboundRhsFunctionVariables)
        {
            ISoarSourceRange range = getVariableRange(body, var);
            reporter.report(SoarProblem.createError("Unbound variable " + var + " passed to function", 
                            range.getOffset(), range.getLength()));
        }
    }
    
    private ISoarSourceRange getVariableRange(String body, String var)
    {
        // TODO: This is a temporary method of marking the variable
        // in the production. Once the parser gives us better location
        // info, we can drop it.
        int offset = body.indexOf(var);
        int length = var.length();
        if(offset < 0)
        {
            offset = getCommandNameRange().getOffset();
            length = 0;
        }
        else
        {
            offset += bodyRange.getOffset();
        }
        
        return new BasicSoarSourceRange(offset, length);
    }
    
    private void parseProductionBody(ISoarProblemReporter reporter, String bodySource) throws SoarModelException
    {
        // If the production body is empty, give a better error message than 
        // "reached <EOF>" that we get from JavaCC.
        if(isAllWhiteSpace(bodySource))
        {
            reporter.report(SoarProblem.createError("Production is empty",
                    bodyRange.getOffset(), bodyRange.getLength()));
            return;
        }
        
        // Now parse the body of the production as Soar.
        StringReader reader = new StringReader(bodySource);
        SoarCharStream charStream = new SoarCharStream(reader, bodyRange.getOffset());
        CustomSoarParserTokenManager mgr = new CustomSoarParserTokenManager(charStream);
        SoarParser parser = new SoarParser(mgr);
        
        try
        {
            ast = parser.soarProduction();
            checkForPositiveConditions(reporter);
            addToDatamap(reporter);
            validateFunctionCalls(reporter);
        }
        catch (ParseException e)
        {
            int start = bodyRange.getOffset() + 1;
            int length = bodyRange.getLength() - 2;
            
            // Yuck. This was built mostly by trial and error because the 
            // info in the ParseException isn't that consistent.
            Token token = e.currentToken;
            if(e.specialConstructor && token.next != null)
            {
                token = token.next;
            }
            if(token.endOffset == 0)
            {
                token.endOffset = token.beginOffset + 1;
            }
            start = token.beginOffset;
            length = (token.endOffset - token.beginOffset) + 1;
            
            // If there was a lot of tcl expansion, then the error may be 
            // displayed outside of the production body. Constrain it here.
            // TODO: Figure out a way to map from expanded offsets back to
            // unexpanded offsets.
            if(start >= bodyRange.getEnd())
            {
                start = bodyRange.getOffset() + 1;
                length = 0;
            }
            
            // If the range goes past the end of the buffer (due to weirdness 
            // in offsets returned by parser) then the marker doesn't show up in
            // the editor. So constrain it.
            int bufferLength = getSoarFile().getBuffer().getLength();
            if(start + length > bufferLength)
            {
                length = bufferLength - start;
            }
            
            reporter.report(SoarProblem.createError(e.getMessage(), start, length));
        }
        catch(TokenMgrError e) // thrown for lexical errors like unterminated strings
        {
            int start = charStream.beginOffset;
            int length = bodyRange.getEnd() - start;
            
            // Strip off the leading line/column info from JavaCC since the
            // lines and columns won't make much sense since we started 
            // somewhere in the middle of the file.
            String message = e.getMessage();
            int i = message.indexOf('.');
            if(i >= 0)
            {
                message = message.substring(i + 1);
            }
            reporter.report(SoarProblem.createError(message, start, length));
        }
    }
    
    private String getParseableBody(ISoarProblemReporter reporter) throws SoarModelException
    {
        ISoarBuffer buffer = getOpenableParent().getBuffer();
        if(buffer == null)
        {
            return "";
        }
        
        // If the body's in braces, we can just parse it directly. Yay.
        if(isBodyInBraces())
        {
            return bodyNode.getInternalText(buffer.getCharacters());
        }
        
        SoarAgent agent = getAgent();
        
        // If this isn't a working copy, maybe the expanded body was already 
        // stored in the agent during the last full Tcl processing pass
        ExpandedProductionInfo info = agent.getExpandedProductionBody(name);
        String namespace = info != null ? info.namespace : "::";
        if(!getSoarFile().isWorkingCopy())
        {
            if(info != null)
            {
                return info.expandedBody;
            }
        }
        
        // Well, lastly try to expand the body as Tcl directly
        String bodySource = getSource(bodyRange);
        IExpandedTclCode code = agent.expandTclString(namespace, bodySource, bodyRange.getOffset());

        if(code.getError() != null)
        {
            reporter.report(SoarProblem.createError(
                    "Tcl error: " + code.getError().getSummary(), 
                    bodyRange.getOffset(), 0));
            return "";
        }
        return code.getResultString();
    }
    
    /**
     * Validate any RHS function calls in the production.
     * 
     * @param reporter The problem reporter
     * @throws SoarModelException
     */
    private void validateFunctionCalls(ISoarProblemReporter reporter) throws SoarModelException
    {
        if(ast == null)
        {
            return;
        }
        
        for(Action a : ast.getActions())
        {
            if(a.isVarAttrValMake())
            {
                for(AttributeValueMake avm : a.getVarAttrValMake().getAttributeValueMakes())
                {
                    for(RHSValue value : avm.getRHSValues())
                    {
                        validateRhsValue(value, reporter);
                    }
                    for(ValueMake vm : avm.getValueMakes())
                    {
                        validateRhsValue(vm.getRHSValue(), reporter);
                        // TODO: PreferenceSpecifier?
                    }
                }
            }
            else
            {
                validateFunctionCall(a.getFunctionCall(), reporter);
            }
        }
    }
    
    private void validateRhsValue(RHSValue value, ISoarProblemReporter reporter) throws SoarModelException
    {
        if(value.isFunctionCall())
        {
            validateFunctionCall(value.getFunctionCall(), reporter);
        }
    }
    
    private void validateFunctionCall(FunctionCall function, ISoarProblemReporter reporter) throws SoarModelException
    {
        String name = function.getFunctionName().getString();
        
        SoarFunctionDescriptor desc = SoarFunctionDescriptor.DEFAULTS.get(name);
        if(desc == null)
        {
            // TODO: Make descriptors extensible per-agent
            return;
        }
        
        desc.validate(function, reporter);
        
        for(RHSValue value : function.getRHSValues())
        {
            validateRhsValue(value, reporter);
        }
    }
    
    /**
     * Analyze the conditions of the production to ensure that there is at 
     * least one positive test on the LHS. This catches the "no LHS roots"
     * error thrown by the kernel.
     * 
     * @param reporter The problem reporter.
     * @throws SoarModelException
     */
    private void checkForPositiveConditions(ISoarProblemReporter reporter) throws SoarModelException
    {
        if(ast != null)
        {
            for(Condition c : ast.getConditions())
            {
                if(conditionHasPositiveTest(c))
                {
                    return;
                }
            }
            
            reporter.report(SoarProblem.createError(
                    "Production has no positive conditions", 
                    nameRange.getOffset(), nameRange.getLength()));
        }
    }
    
    private boolean conditionHasPositiveTest(Condition c)
    {
        if(c.isNegated())
        {
            return false;
        }
        PositiveCondition pc = c.getPositiveCondition();
        if(pc.isConjunction())
        {
            for(Condition cc : pc.getConjunction())
            {
                if(conditionHasPositiveTest(cc))
                {
                    return true;
                }
            }
        }
        else
        {
            ConditionForOneIdentifier cfoi = pc.getConditionForOneIdentifier();
            if(cfoi.hasState() && cfoi.getAttributeValueTests().isEmpty())
            {
                return true;
            }
            for(AttributeValueTest avt : cfoi.getAttributeValueTests())
            {
                if(!avt.isNegated())
                {
                    return true;
                }
            }
        }
        
        return false;
        
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProduction#isBodyInBraces()
     */
    public boolean isBodyInBraces()
    {
        return bodyInBraces;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProduction#getSyntaxTree()
     */
    public SoarProductionAst getSyntaxTree()
    {
        return ast;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProduction#getProductionName()
     */
    public ISoarSourceRange getProductionNameRange()
    {
        return nameRange;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProduction#getProductionName()
     */
    public String getProductionName()
    {
        return name;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "SoarProduction " + getProductionName();
    }

}
