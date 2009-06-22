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
package com.soartech.soar.ide.core.tcl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ray
 */
public class TclAstNode
{
    public static final int ROOT = 0;
    public static final int COMMENT = 1;
    public static final int COMMAND = 2;
    public static final int NORMAL_WORD = 3;
    public static final int QUOTED_WORD = 4;
    public static final int BRACED_WORD = 5;
    public static final int COMMAND_WORD = 6;
    
    private static final String TYPES[] = new String[] {
        "ROOT", "COMMENT", "COMMAND", "NORMAL_WORD", "QUOTED_WORD",
        "BRACED_WORD", "COMMAND_WORD"
    };
    
    private int type;
    private int start;
    private int length;
    private List<TclAstNode> children;
    private TclAstNode previousChild = null;
    private TclParserError error;
    
    public TclAstNode(int type, int start)
    {
        this.type = type;
        this.start = start;
    }

    public List<TclAstNode> getChildren()
    {
        if(children == null)
        {
            children = new ArrayList<TclAstNode>();
        }
        return children;
    }
    
    public void addChild(TclAstNode node)
    {
        List<TclAstNode> kids = getChildren();
        if(!kids.isEmpty())
        {
            node.previousChild = kids.get(kids.size() - 1);
        }
        else
        {
            node.previousChild = null;
        }
        kids.add(node);
    }
    
    public TclAstNode getPrevious()
    {
        return previousChild;
    }

    public int getLength()
    {
        return length;
    }
    public void setEnd(int end)
    {
        this.length = end - start;
    }

    public int getStart()
    {
        return start;
    }
    
    public boolean containsOffset(int offset)
    {
    	int end = start + length;
    	
    	if(offset >= start && offset <= end)
    	{
    		return true;
    	}
    	
    	return false;
    }

    public int getType()
    {
        return type;
    }
    
    /**
     * @return the error
     */
    public TclParserError getError()
    {
        return error;
    }

    /**
     * @param error the error to set
     */
    public void setError(TclParserError error)
    {
        this.error = error;
    }

    /**
     * @return If this node is any type of word node
     */
    public boolean isWord()
    {
        return type == NORMAL_WORD || type == QUOTED_WORD ||
               type == BRACED_WORD || type == COMMAND_WORD;
    }
    
    public boolean isExpandable()
    {
        return type == QUOTED_WORD || type == COMMAND_WORD;
    }
    
    public String getInternalText(char[] buffer)
    {
        int internalStart = start;
        int internalLength = length;
        
        if(type == BRACED_WORD || type == QUOTED_WORD)
        {
            ++internalStart;
            internalLength -= 2;
        }
        
        if(internalLength <= 0 || (internalStart + internalLength > buffer.length))
        {
            return "";
        }
        
        return new String(buffer, internalStart, internalLength);
    }
    
    public TclAstNode getChild(int type)
    {
        if(children == null)
        {
            return null;
        }
        for(TclAstNode child : getChildren())
        {
            if(child.getType() == type)
            {
                return child;
            }
        }
        return null;
    }
    
    public List<TclAstNode> getWordChildren()
    {
        List<TclAstNode> words = new ArrayList<TclAstNode>();
        if(children != null)
        {
            for(TclAstNode child : getChildren())
            {
                if(child.isWord())
                {
                    words.add(child);
                }
            }
        }
        return words;
        
    }
    
    public void printTree(PrintStream stream, char input[], int depth)
    {
        for(int i = 0; i < depth; ++i)
        {
            stream.print("   ");
        }
        stream.print(this);
        if(children == null || children.isEmpty())
        {
            stream.println(": " + new String(input, start, length));
        }
        else
        {
            stream.println();
        }
        for(TclAstNode node : getChildren())
        {
            node.printTree(stream, input, depth + 1);
        }
    }

    @Override
    public String toString()
    {
        StringBuffer b = new StringBuffer();
        b.append(TYPES[type]);
        b.append(" [" + start + ", " + length + ")");
        return b.toString();
    }
    
    /**
     * Check whether this node contains the given word type.
     * @param type <pre>
     * NORMAL_WORD,
     * QUOTED_WORD,
     * BRACED_WORD,
     * or
     * COMMAND_WORD
     * </pre>
     * @return true if this node contains any of the given word types.
     */
    public boolean containsType( int type ) {
    	
    	if ( this.type == type ) {
    		return true;
    	}
    	
    	for ( TclAstNode child : getChildren() ) {
    		if ( child.containsType( type ) ) return true;
    	}
    	
    	return false;
    }
    
//    /**
//     * Find a child with a particular line and column anywhere in the tree.
//     *
//     * @param ast the parent node
//     * @param type the type of child to find
//     * @return the child, or null if not found
//     */
//    static public TclAstNode findChild(TclAstNode ast, int line, int column)
//    {
//    	TclAstNode node = null;
//    	
////        for(TclAstNode c = ast.getFirstChild(); c != null; c = c.getNextSibling())
//    	for(TclAstNode n:ast.getChildren())
//        {
//            if(evaluateNode((Node) c, line, column))
//            {
//                return c;
//            }
//            else
//            {
//                node = findChild(c, line, column);
//                if(node != null)
//                {
//                    return node;
//                }
//            }
//        }
//        return node;
//    }
//    
//    private static boolean evaluateNode(TclAstNode node, int line, int column)
//    {
//    	if(node == null)
//    	{
//    		return false;
//    	}
//    	
////    	node.
//    }
    
}
