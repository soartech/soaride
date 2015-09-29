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
package com.soartech.soar.ide.core.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.SoarProblem;

/**
 * @author ray
 */
public class SoarModelTools
{
    public static boolean isSoarFile(IResourceProxy proxy)
    {
        if(proxy.isDerived())
        {
            return false;
        }
        
        if(proxy.getType() == IResource.FILE)
        {
            return isSoarFile(proxy.getName());
        }
        return false;
    }

    public static boolean isSoarFile(IFile file)
    {
        if (file.isDerived(IResource.CHECK_ANCESTORS) )
        {
            return false;
        }
        
        return isSoarFile(file.getName());
    }
    
    public static boolean isSoarFile(String name)
    {
        return name.endsWith(".soar") || name.endsWith(".tcl");
    }

    public static boolean isAgentFile(IFile file)
    {
        if ( file.isDerived(IResource.CHECK_ANCESTORS) )
        {
            return false;
        }
        
        return "soaragent".equals(file.getFileExtension());
    }

    public static boolean isAgentFile(IResourceProxy proxy)
    {
        if ( proxy.isDerived() )
        {
            return false;
        }
        
        if(proxy.getType() == IResource.FILE)
        {
            String name = proxy.getName();

            return name.endsWith(".soaragent");
        }
        return false;
    }



    /**
     * Return a non-null progress monitor
     *
     * @param monitor Input monitor
     * @return The input if non-null, or a new progress monitor.
     */
    public static IProgressMonitor getSafeMonitor(IProgressMonitor monitor)
    {
        return monitor != null ? monitor : new NullProgressMonitor();
    }

    /**
     * Checks whether the given monitor is canceled and throws an
     * OperationCanceledException if it is. This is the behavior expected in
     * the run method of IWorkspaceRunnable
     *
     * @param monitor
     */
    public static void checkForCancellation(IProgressMonitor monitor)
    {
        if(monitor != null && monitor.isCanceled())
        {
            throw new OperationCanceledException();
        }
    }

    /**
     * Show a particular Soar element in an editor.
     *
     * <p>Taken from
     * <a href="http://wiki.eclipse.org/index.php/FAQ_How_do_I_open_an_editor_on_a_file_in_the_workspace%3F">here</a>
     *
     * @param page The workbench page
     * @param element The Soar element to display
     * @return The editor the element was shown in, or null if the element
     *      could not be displayed.
     * @throws CoreException
     */
    public static IEditorPart showElementInEditor(IWorkbenchPage page, ISoarElement element) throws CoreException
    {
        // Find the resource that contains the element
        IResource resource = element.getContainingResource();
        if(resource == null)
        {
            return null;
        }

        // Now see if it's a file
        IFile file = (IFile) resource.getAdapter(IFile.class);
        if(file == null)
        {
            return null;
        }

        IEditorPart part = null;
        if(element instanceof ISoarSourceReference)
        {
            ISoarSourceRange range = ((ISoarSourceReference) element).getSourceRange();

            HashMap<String, Integer> map = new HashMap<String, Integer>();
            map.put(IMarker.CHAR_START, range.getOffset());
            map.put(IMarker.CHAR_END, range.getOffset() + range.getLength());

            IMarker marker = file.createMarker(IMarker.TEXT);
            marker.setAttributes(map);

            part = IDE.openEditor(page, marker);
            marker.delete();
        }
        else
        {
            part = IDE.openEditor(page, file);
        }

//      ISoarSourceReference ref = (ISoarSourceReference) element;
//      ISoarSourceRange range;
//      try
//      {
//          range = ref.getSourceRange();
//          settingEditorPosition = true;
//          editor.selectAndReveal(range.getOffset(), 0 /* just highlight line, not all text */);
//          settingEditorPosition = false;
//      }
//      catch (SoarModelException e)
//      {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//      }



        return part;
    }

    /**
     * Find the child element of the given parent at the given buffer offset.
     *
     * @param parent Parent element
     * @param offset The offset
     * @return The child or <code>null</code> if not found
     * @throws SoarModelException
     */
    public static ISoarElement getChildAtOffset(ISoarElement parent, int offset) throws SoarModelException
    {
        for(ISoarElement child : parent.getChildren())
        {
            if(child instanceof ISoarSourceReference)
            {
                ISoarSourceReference ref = (ISoarSourceReference) child;
                if(ref.getSourceRange().contains(offset))
                {
                    return child;
                }
            }
        }
        return null;
    }
    public static int getLineNumber(IFile file, int offset) throws SoarModelException
    {
        char[] contents = SoarModelTools.readFileAsCharArray(file);
        int lineNumber=1;
        for(int i=0;i<offset;i++)
        {
        	if(contents[i]=='\n')
        		lineNumber++;
        }
        return lineNumber;
    }

    /**
     * Create an error marker on a file.
     *
     * @param file The file
     * @param location String location such as line number
     * @param start Start character offset
     * @param length Length of text
     * @param message The error message
     * @param fixID the Quick Fix ID used to identify this warning
     * @param map A hashmap of values needed to 
     * @return the marker
     * @throws SoarModelException
     */
    public static IMarker createErrorMarker(IFile file, int start, int length, String message) throws SoarModelException
    {
        // TODO: Put line number in second argument of:
        return createErrorMarker(SoarCorePlugin.PROBLEM_MARKER_ID, file, "", start, length, message,"",new HashMap<String, Comparable<?>>());
    }

    public static IMarker createErrorMarker(String markerType, IFile file, String location, int start, int length, String message, String fixID, HashMap<String, Comparable<?>> map) throws SoarModelException
    {
    	SoarProblem p = SoarProblem.createError(message,location,start,length,fixID,map);
        try
        {
            IMarker marker = file.createMarker(markerType);
            marker.setAttributes(p.map);
            return marker;
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }
    }

    /**
     * Create an warning marker on a file.
     *
     * @param file The file
     * @param start Start character offset
     * @param length Length of text
     * @param message The warning message
     * @return the marker
     * @throws SoarModelException
     */
    public static IMarker createWarningMarker(IResource resource, int start, int length, String message) throws SoarModelException
    {
        // TODO: Put line number in second argument of:
    	return createWarningMarker(resource,"", start,length,message, "",new HashMap<String, Comparable<?>>());
    }
    /**
     * Create an warning marker on a file.
     *
     * @param file The file
     * @param location String location such as line number
     * @param start Start character offset
     * @param length Length of text
     * @param message The warning message
     * @param fixID the Quick Fix ID used to identify this warning
     * @param map A hashmap of values needed to 
     * @return the marker
     * @throws SoarModelException
     */
    public static IMarker createWarningMarker(IResource resource, String location, int start, int length, String message, String fixID, HashMap<String, Comparable<?>> map) throws SoarModelException
    {
        SoarProblem p = SoarProblem.createWarning(message, location, start, length, fixID, map);

        try
        {
            IMarker marker = resource.createMarker(SoarCorePlugin.PROBLEM_MARKER_ID);
            marker.setAttributes(p.map);
            return marker;
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }
    }

    public static boolean isSoarMarker(IMarker marker)
    {
        try
        {
            String type = marker.getType();

            return type.startsWith(SoarCorePlugin.MARKER_TYPE_PREFIX);
        }
        catch (CoreException e)
        {
            return false;
        }
    }

    /**
     * Delete all markers of a given type on a resource
     *
     * @param resource The resource
     * @param type The type of marker
     * @throws CoreException
     */
    public static void deleteMarkers(IResource resource, String type) throws CoreException
    {
        deleteMarkers(resource, type, -1, -1);
    }

    /**
     * Find and remove all markers of the given type that overlap a given
     * region.
     *
     * @param resource The resource
     * @param type The marker type
     * @param startOffset Start offset of region, or -1 for all markers
     * @param endOffset End offset of region
     */
    public static void deleteMarkers(IResource resource, String type,
                                     int startOffset, int endOffset) throws CoreException
    {
        if(startOffset < 0)
        {
            resource.deleteMarkers(type, true, IResource.DEPTH_INFINITE);
            return;
        }

        IMarker[] allMarkers = resource.findMarkers(type, true, IResource.DEPTH_INFINITE);
        for(IMarker marker : allMarkers)
        {
            int markerStart = marker.getAttribute(IMarker.CHAR_START, -1);
            int markerEnd = marker.getAttribute(IMarker.CHAR_END, -1);

            if((startOffset >= markerStart && startOffset < markerEnd) ||
               (endOffset >= markerStart && endOffset < markerEnd) ||
               (markerStart >= startOffset && markerStart < endOffset) ||
               (markerEnd >= startOffset && markerEnd < endOffset))
            {
                marker.delete();
            }
        }
    }

    /**
     * Read the content of a file and return it as a character array
     *
     * @param file The file to read
     * @return The file content
     * @throws SoarModelException
     */
    public static char[] readFileAsCharArray(IFile file) throws SoarModelException
    {
        InputStream stream = null;
        try
        {
            stream = file.getContents();
        }
        catch (CoreException e1)
        {
            throw new SoarModelException(e1);
        }
        Reader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        try
        {
            char chars[] = new char[4096];
            int r = reader.read(chars);
            while(r >= 0)
            {
                builder.append(chars, 0, r);
                r = reader.read(chars);
            }
        }
        catch(IOException e)
        {
            throw new SoarModelException(e);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                throw new SoarModelException(e);
            }
        }
        return builder.toString().toCharArray();
    }

    /**
     * Given a path, possibly an absolute, non-workspace path, find the
     * corresponding eclipse resource in the workspace.
     *
     * @param path The path
     * @return The resource or null if not found
     */
    public static IResource getEclipseResource(IPath path)
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(path);
        if(resource != null)
        {
            return resource;
        }

        resource = root.getFileForLocation(path);
        if(resource != null)
        {
            return resource;
        }

        IPath rootLoc = root.getLocation();
        if(!rootLoc.isPrefixOf(path))
        {
            return null;
        }
        int segments = rootLoc.matchingFirstSegments(path);

        IPath relativePath = path.removeFirstSegments(segments).setDevice(null).makeAbsolute();

        return root.findMember(relativePath);
    }

    public static IPath getPathRelativeToContainer(IContainer container, IResource resource)
    {
        IPath containerPath = container.getFullPath();
        IPath resourcePath = resource.getFullPath();

        int i = 0;
        for(; i < containerPath.segmentCount() && i < resourcePath.segmentCount(); ++i)
        {
            if(!containerPath.segment(i).equals(resourcePath.segment(i)))
            {
                break;
            }
        }

        String path = "";
        for(int j = i; j < containerPath.segmentCount(); ++j)
        {
            path += ".." + Path.SEPARATOR;
        }
        for(int j = i; j < resourcePath.segmentCount(); ++j)
        {
            path += resourcePath.segment(j);
            if(j != resourcePath.segmentCount() - 1)
            {
                path += Path.SEPARATOR;
            }
        }
        return new Path(path);
    }

    /**
     * Search the project a Soar file is in for all "source" reference to that file.
     *
     * @param file The referenced file
     * @return List of references to that file
     * @throws SoarModelException
     */
    public static List<ITclFileReference> getReferences(ISoarFile file) throws SoarModelException
    {
        synchronized(file.getLock())
        {
            IPath referencedLocation = file.getFile().getLocation();
            List<ITclFileReference> result = new ArrayList<ITclFileReference>();
            ISoarProject project = file.getSoarProject();

            for(ISoarElement e : project.getChildren())
            {
                if(e instanceof ISoarFile)
                {
                    getReferences(referencedLocation, (ISoarFile) e, result);
                }
            }
            return result;
        }
    }

    private static void getReferences(IPath referencedLocation, ISoarFile referencer, List<ITclFileReference> result) throws SoarModelException
    {
        ISoarFileAgentProxy proxy = referencer.getPrimaryAgentProxy();
        if(proxy == null)
        {
            return;
        }
        for(ISoarElement e : proxy.getChildren())
        {
            if(e instanceof ITclFileReference)
            {
                ITclFileReference r = (ITclFileReference) e;
                if(referencedLocation.equals(r.getReferencedLocation()))
                {
                    result.add(r);
                }
            }
        }
    }
}
