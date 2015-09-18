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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.SoarProblem;

/**
 * Implementation of {@link ISoarProblemReporter} that reports errors as 
 * problem markers on a particular Eclipse resource.
 * 
 * @author ray
 */
public class SoarResourceProblemReporter implements ISoarProblemReporter
{
    /**
     * A simple runnable interface so we can queue up actions and run them
     * when the apply() is called. We don't use Runnable because we want
     * to be able to throw exceptions.
     */
    private static interface ProblemAction
    {
        void run() throws SoarModelException;
    }
    
    private IResource resource;
    /**
     * Queued list of actions
     */
    private List<ProblemAction> actions = new ArrayList<ProblemAction>();
    /**
     * List of problems added so far. This is used to avoid duplicate problems
     * from being reported.
     */
    private Set<SoarProblem> problemsToAdd = new HashSet<SoarProblem>();
    
    /**
     * Construct a new reporter for the given resource. There should not be
     * more than one reporter for a given resource at any one time.
     * 
     * @param resource The resource to report problems on
     */
    public SoarResourceProblemReporter(IResource resource)
    {
        this.resource = resource;
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProblemReporter#clear()
     */
    public void clear() throws SoarModelException
    {
        actions.add(new ProblemAction() {

            public void run() throws SoarModelException
            {
                doClear();
            }});
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProblemReporter#report(com.soartech.soar.ide.core.model.SoarProblem)
     */
    public void report(final SoarProblem problem) throws SoarModelException
    {
        // Don't report duplicates
        if(!problemsToAdd.add(problem))
        {
            return;
        }
        problemsToAdd.add(problem);
        actions.add(new ProblemAction() {

            public void run() throws SoarModelException
            {
                doAdd(problem);
            }});
    }

    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProblemReporter#apply()
     */
    public void apply() throws SoarModelException
    {
        for(ProblemAction action : actions)
        {
            action.run();
        }
        actions.clear();
        problemsToAdd.clear();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProblemReporter#getProblems()
     */
    public List<SoarProblem> getProblems() throws SoarModelException
    {
        List<SoarProblem> result = new ArrayList<SoarProblem>();
        
        try
        {
        for(IMarker marker : resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE))
        {
            if(marker.isSubtypeOf(IMarker.TEXT))
            {
                SoarProblem problem = new SoarProblem();
                problem.severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                problem.start = marker.getAttribute(IMarker.CHAR_START, -1);
                problem.length = marker.getAttribute(IMarker.CHAR_END, problem.start) - problem.start;
                problem.message = marker.getAttribute(IMarker.MESSAGE, "");
                
                result.add(problem);
            }
        }
        }
        catch(CoreException e)
        {
            SoarCorePlugin.log(e);
            throw new SoarModelException(e);
        }
        return result;
    }

    private void doClear() throws SoarModelException
    {
        try
        {
            SoarModelTools.deleteMarkers(resource, SoarCorePlugin.PROBLEM_MARKER_ID);
            SoarModelTools.deleteMarkers(resource, SoarCorePlugin.TASK_MARKER_ID);
        }
        catch(CoreException e)
        {
            SoarCorePlugin.log(e);
            throw new SoarModelException(e);
        }
    }
    
    private void doAdd(SoarProblem problem) throws SoarModelException
    {
        try
        {
            IMarker marker = resource.createMarker(SoarCorePlugin.PROBLEM_MARKER_ID);
            marker.setAttributes(problem.map);
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }
    }
}
