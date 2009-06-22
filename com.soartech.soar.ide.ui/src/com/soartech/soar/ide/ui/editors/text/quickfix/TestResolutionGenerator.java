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
package com.soartech.soar.ide.ui.editors.text.quickfix;

import org.eclipse.swt.graphics.Image;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import com.soartech.soar.ide.ui.SoarEditorPluginImages;

public class TestResolutionGenerator implements IMarkerResolutionGenerator2
{
	public IMarkerResolution[] getResolutions(IMarker marker)
	{
		IMarkerResolution[] results = {new TestMarkerResolution(marker)};
		return results;
	}
	public boolean hasResolutions(IMarker marker)
	{
		String id = marker.getAttribute("com.soartech.soar.ide.core.quickFixId", "");
		return (id.equalsIgnoreCase("test"));
	}
  private class TestMarkerResolution implements IMarkerResolution2 {
	    /**
	     * Instantiates review marker resolution.
	     * @param marker the marker.
	     */
	    private TestMarkerResolution(IMarker marker) {
	    	// use marker.getResource() to get the resource which should always be an IFile (verify anyway)
	    }
	    /**
	     * @see org.eclipse.ui.IMarkerResolution#getLabel()
	     */
	    public String getLabel() {
	      String summary = "Do nothing.";
	      return summary;
	    }
	    /**
	     * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	     */
	    public void run(IMarker marker) {
	    	
	    }

	    /**
	     * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	     */
	    public String getDescription() {
	      String description = "Do nothing at ALL.";
	      return description;
	    }

	    /**
	     * @see org.eclipse.ui.IMarkerResolution2#getImage()
	     */
	    public Image getImage() {
	      return SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_FILE_REFERENCE);
	    }

	  }

}