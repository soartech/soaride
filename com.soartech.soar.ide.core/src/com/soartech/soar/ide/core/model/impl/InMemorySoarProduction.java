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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarOpenable;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ITclComment;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.ast.CustomSoarParserTokenManager;
import com.soartech.soar.ide.core.model.ast.ParseException;
import com.soartech.soar.ide.core.model.ast.SoarCharStream;
import com.soartech.soar.ide.core.model.ast.SoarParser;
import com.soartech.soar.ide.core.model.ast.SoarProductionAst;
import com.soartech.soar.ide.core.model.ast.TokenMgrError;
import com.soartech.soar.ide.core.tcl.TclAstNode;

/** Largely unimplemented ISoarProduction interface to parse
 *  productions without requiring Eclipse-specific objects.
 */
public class InMemorySoarProduction implements ISoarProduction
{
    private SoarProductionAst ast;

    public InMemorySoarProduction(String source)
    {
        this.ast = null;

        parseProductionBody(source);
    }

    public SoarProductionAst getSyntaxTree()
    {
        return ast;
    }

    private void parseProductionBody(String source)
    {
        // Remove comments
        final String Comments = "#[^\\n]*";
        String bodySource = source.replaceAll(Comments, "");

        // Trim the opening of the production off (we're post
        // TCL parse, so we can assume braces).
        final String BeginningOfProduction = "sp[\\s]*\\{";
        bodySource = bodySource.replaceFirst(BeginningOfProduction, "");

        // Trim the back (which is sometimes missing for some reason)
        bodySource = bodySource.trim();
        char lastChar = bodySource.charAt(bodySource.length() - 1);
        if (lastChar == '}' || lastChar == '"') bodySource = bodySource.substring(0, bodySource.length() - 1);

        StringReader reader = new StringReader(bodySource);
        SoarCharStream charStream = new SoarCharStream(reader, 0);
        CustomSoarParserTokenManager mgr = new CustomSoarParserTokenManager(charStream);
        SoarParser parser = new SoarParser(mgr);

        try
        {
            ast = parser.soarProduction();
        }
        catch (ParseException e) { }
        catch (TokenMgrError e) { }
    }




    public String getExpandedSource() throws SoarModelException
    {
        throw new UnsupportedOperationException();
    }

    public String getProductionName()
    {
        throw new UnsupportedOperationException();
    }

    public ISoarSourceRange getProductionNameRange()
    {
        throw new UnsupportedOperationException();
    }

    public boolean isBodyInBraces()
    {
        throw new UnsupportedOperationException();
    }

    public ITclComment getAssociatedComment()
    {
        throw new UnsupportedOperationException();
    }

    public String getCommandName()
    {
        throw new UnsupportedOperationException();
    }

    public ISoarSourceRange getCommandNameRange()
    {
        throw new UnsupportedOperationException();
    }

    public TclAstNode getTclSyntaxTree()
    {
        throw new UnsupportedOperationException();
    }

    public List<ISoarElement> getChildren() throws SoarModelException
    {
        throw new UnsupportedOperationException();
    }

    public IResource getContainingResource()
    {
        throw new UnsupportedOperationException();
    }

    public IResource getCorrespondingResource()
    {
        throw new UnsupportedOperationException();
    }

    public Object getLock()
    {
        throw new UnsupportedOperationException();
    }

    public ISoarOpenable getOpenable()
    {
        throw new UnsupportedOperationException();
    }

    public ISoarElement getParent()
    {
        throw new UnsupportedOperationException();
    }

    public IPath getPath()
    {
        throw new UnsupportedOperationException();
    }

    public ISoarModel getSoarModel()
    {
        throw new UnsupportedOperationException();
    }

    public ISoarProject getSoarProject()
    {
        throw new UnsupportedOperationException();
    }

    public boolean hasChildren()
    {
        throw new UnsupportedOperationException();
    }

    public boolean hasErrors()
    {
        throw new UnsupportedOperationException();
    }

    public boolean hasWarnings()
    {
        throw new UnsupportedOperationException();
    }

    public boolean isDetached()
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object getAdapter(Class adapter)
    {
        throw new UnsupportedOperationException();
    }

    public String getSource() throws SoarModelException
    {
        throw new UnsupportedOperationException();
    }

    public ISoarSourceRange getSourceRange() throws SoarModelException
    {
        throw new UnsupportedOperationException();
    }

}
