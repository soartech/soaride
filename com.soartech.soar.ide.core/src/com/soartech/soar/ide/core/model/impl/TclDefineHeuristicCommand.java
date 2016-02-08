/**
 * 
 */
package com.soartech.soar.ide.core.model.impl;

import java.util.List;

import com.soartech.soar.ide.core.model.IExpandableElement;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.tcl.TclAstNode;

/**
 * @author aron
 *
 */
public class TclDefineHeuristicCommand extends TclCommand implements IExpandableElement
{
//    private TclAstNode bodyNode;
//    private ISoarSourceRange bodyRange;
//    private ISoarSourceRange nameRange;
//    private String name = "";
//    private SoarProductionAst ast;

    public TclDefineHeuristicCommand(SoarFileAgentProxy parent, TclAstNode astNode) throws SoarModelException 
    {
        super(parent, astNode, null);
        
        List<TclAstNode> words = getTclSyntaxTree().getWordChildren();
        if(words.size() >= 2)
        {
            
        }
    }

    @Override
    public String getExpandedSource() throws SoarModelException 
    {
//        SoarAgent agent = getAgent();
//        ExpandedProductionInfo info = agent.getExpandedProductionBody(name);
//        String namespace = info != null ? info.namespace : "::";
//        if(!getSoarFile().isWorkingCopy())
//        {
//            if(info != null)
//            {
//                return rebuildFromExpandedBody(info.expandedBody);
//            }
//        }
//        
//        // Well, let's see if we can expand it manually...
//        String bodySource = getSource(bodyRange);
//        
//        IExpandedTclCode code = agent.expandTclString(namespace, bodySource, bodyRange.getOffset());
//
//        if(code.getError() != null)
//        {
//            //return code.getResultString();
//            return code.getError().getMessage();
//        }
//        return rebuildFromExpandedBody(code.getResultString());
        
        return null;
    }
    
//    private String rebuildFromExpandedBody(String body) throws SoarModelException
//    {
//        StringBuilder builder = new StringBuilder();
//        ITclComment comment = getAssociatedComment();
//        if(comment != null)
//        {
//            builder.append(comment.getSource());
//            //builder.append("\n");
//        }
//        builder.append("sp {");
//        builder.append(body);
//        builder.append("}");
//        return builder.toString();
//    }
    
//    private SoarAgent getAgent()
//    {
//        return (SoarAgent) getSoarFile().getAgent();
//    }

}
