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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.widgets.Display;

import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarProblem;

/**
 * @author ray
 */
public class SoarEditorProblemReporter implements ISoarProblemReporter
{
    private static interface ProblemAction
    {
        void run();
    }
    
    private SoarEditor editor;
    private List<ProblemAction> actions = new ArrayList<ProblemAction>();
    private List<SoarProblem> problems = new ArrayList<SoarProblem>();
    
    /**
     * @param model
     */
    public SoarEditorProblemReporter(SoarEditor editor)
    {
        this.editor = editor;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProblemReporter#apply()
     */
    public void apply() throws SoarModelException
    {
        Display.getDefault().asyncExec(new Runnable() {

            public void run()
            {
                doApply();
            }});
    }

    private synchronized void doApply()
    {
        for(ProblemAction action : actions)
        {
            action.run();
        }
        actions.clear();
        problems.clear();
    }
    
    private IAnnotationModel getModel()
    {
        return editor.getAnnotationModel();
    }
    
    @SuppressWarnings("unchecked")
    private void doClear()
    {
        Iterator<Annotation> it = getModel().getAnnotationIterator();
        while(it.hasNext())
        {
            Annotation a = it.next();
            
            if(a instanceof SoarEditorAnnotation)
            {
                getModel().removeAnnotation(a);
            }
        }
        
    }
    
    private void doReport(SoarProblem problem)
    {
        Position position = new Position(problem.start, problem.length);
        if(problem.severity == IMarker.SEVERITY_ERROR)
        {
            SoarEditorAnnotation.addError(getModel(), problem.message, position);
        }
        else if(problem.severity == IMarker.SEVERITY_WARNING)
        {
            SoarEditorAnnotation.addWarning(getModel(), problem.message, position);
        }
        else if(problem.severity == IMarker.SEVERITY_INFO)
        {
            // TODO
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProblemReporter#clear()
     */
    public synchronized void clear() throws SoarModelException
    {
        actions.add(new ProblemAction() {

            public void run()
            {
                doClear();
            }});
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProblemReporter#getProblems()
     */
    public synchronized List<SoarProblem> getProblems() throws SoarModelException
    {
        // TODO Auto-generated method stub
        return new ArrayList<SoarProblem>(problems);
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProblemReporter#report(com.soartech.soar.ide.core.model.SoarProblem)
     */
    public synchronized void report(final SoarProblem problem) throws SoarModelException
    {
        actions.add(new ProblemAction() {

            public void run()
            {
                doReport(problem);
            }});
    }

}
