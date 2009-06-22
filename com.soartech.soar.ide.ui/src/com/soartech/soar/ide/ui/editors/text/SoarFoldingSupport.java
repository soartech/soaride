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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

import com.soartech.soar.ide.core.model.ISoarModelConstants;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.tcl.TclAstNode;

/**
 * @author ray
 */
public class SoarFoldingSupport
{
    private SoarEditor editor;
    private ProjectionAnnotationModel projectionModel;
    private ProjectionSupport projectionSupport;
    private List<SoarProjectionAnnotation> currentProjectionAnnotations = new LinkedList<SoarProjectionAnnotation>();
    
    private List<SoarFoldingRegion> regions = new ArrayList<SoarFoldingRegion>();
    private Set<TclAstNode> regionNodes = new HashSet<TclAstNode>();
    
    private List<SoarProjectionAnnotation> modified = new ArrayList<SoarProjectionAnnotation>();
    
    public SoarFoldingSupport(SoarEditor editor, ProjectionViewer viewer,
                              IAnnotationAccess annotationAccess,
                              ISharedTextColors sharedColors)
    {
        this.editor = editor;
        
        projectionSupport = new ProjectionSupport(viewer,annotationAccess,sharedColors);
        projectionSupport.install();

        //turn projection mode on
        viewer.doOperation(ProjectionViewer.TOGGLE);
        projectionModel = viewer.getProjectionAnnotationModel();
    }

    /**
     * @return the regions
     */
    public List<SoarFoldingRegion> getRegions()
    {
        return regions;
    }

    @SuppressWarnings("unchecked")
    public void updateFoldingStructure()
    {
        HashMap additions = new HashMap();
        modified.clear();
        regions.clear();
        
        TclAstNode root = editor.getSoarFileWorkingCopy().getTclSyntaxTree();
        if(root != null)
        {
            try
            {
                refreshRegions(root, editor.getSoarFileWorkingCopy().getBuffer().getCharacters());
            }
            catch (SoarModelException e) {}
            
            for(SoarFoldingRegion region : regions)
            {
                try
                {
                    ISoarSourceRange range = region.getSourceRange();
                    addAnnotation(additions, range.getOffset(), range.getLength());
                }
                catch (SoarModelException e)
                {
                }
            }
            for(TclAstNode child : root.getChildren())
            {
                if(regionNodes.contains(child)) continue;
                
                addAnnotation(additions, child.getStart(), child.getLength());
            }
        }
        
        projectionModel.modifyAnnotations(currentProjectionAnnotations.toArray(new SoarProjectionAnnotation[currentProjectionAnnotations.size()]), 
                                          additions, 
                                          modified.toArray(new SoarProjectionAnnotation[modified.size()]));
        
        currentProjectionAnnotations.clear();
        currentProjectionAnnotations.addAll(modified);
        currentProjectionAnnotations.addAll(additions.keySet());
    }
    
    @SuppressWarnings("unchecked")
    private void addAnnotation(HashMap additions, int start, int length)
    {
        Position pos = createPosition(start, length - 1);
        if(pos != null)
        {
            SoarProjectionAnnotation annotation = getExistingAnnotation(pos);
            if(annotation == null)
            {
                annotation = new SoarProjectionAnnotation();
                annotation.position = pos;
                additions.put(annotation, pos);
            }
        }
    }
    
    private SoarProjectionAnnotation getExistingAnnotation(Position newPos)
    {
        Iterator<SoarProjectionAnnotation> it = currentProjectionAnnotations.iterator();
        while (it.hasNext())
        {
            SoarProjectionAnnotation e = it.next();

            Position oldPos = e.position;
            if (oldPos.getLength() == newPos.getLength())
            {
                int n = oldPos.getLength();
                int a = oldPos.getOffset();
                int b = newPos.getOffset();
                if (a == b || (a > b && a < b + n) || (b > a && b < a + n))
                {
                    e.position.setOffset(newPos.getOffset());
                    e.position.setLength(newPos.getLength());
                    modified.add(e);
                    it.remove();
                    return e;
                }
            }
        }

        return null;
    }
        
    private Position createPosition(int offset, int length)
    {
        try
        {
            IDocument doc = editor.getDocument();
            int start = doc.getLineOfOffset(offset);
            int end = doc.getLineOfOffset(offset + length);
            if(start != end)
            {
                int lineOffset = doc.getLineOffset(start);
                int endOffset = doc.getLength();
                try
                {
                    endOffset = doc.getLineOffset(end + 1);
                }
                catch (BadLocationException e) {}
                return new Position(lineOffset, endOffset - offset);
            }
        }
        catch (BadLocationException e)
        {
        }
        return null;
    }
    
    private void refreshRegions(TclAstNode root, char buffer[])
    {
        Stack<TclAstNode> stack = new Stack<TclAstNode>();
        Stack<String> names = new Stack<String>();
        
        for(TclAstNode child : root.getChildren())
        {
            if(child.getType() != TclAstNode.COMMENT) continue;
            
            String text = child.getInternalText(buffer);
            if(text.startsWith(ISoarModelConstants.REGION_START))
            {
                regionNodes.add(child);
                stack.push(child);
                int end = text.indexOf('\n');
                if(end == -1)
                {
                    end = text.length();
                }
                names.push(text.substring(ISoarModelConstants.REGION_START.length(), end));
            }
            else if(text.startsWith(ISoarModelConstants.REGION_END))
            {
                regionNodes.add(child);
                if(!stack.isEmpty())
                {
                    TclAstNode start = stack.pop();
                    String name = names.pop();
                    int endOffset = child.getStart() + child.getLength();
                    
                    regions.add(new SoarFoldingRegion(editor.getDocument(),
                            name, start.getStart(), endOffset - start.getStart()));
                }
            }
        }
    }
    
    public void dispose()
    {
        if(projectionSupport != null)
        {
            projectionSupport.dispose();
            projectionSupport = null;
        }
        
    }
    
    private static class SoarProjectionAnnotation extends ProjectionAnnotation
    {
        public Position position;
    }

}
