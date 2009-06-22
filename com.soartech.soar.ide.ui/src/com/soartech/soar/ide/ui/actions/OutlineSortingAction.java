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
package com.soartech.soar.ide.ui.actions;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredViewer;

import com.soartech.soar.ide.ui.SoarEditorPluginImages;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.views.outline.SoarOutlinePage;

/**
 * Sorting action for the outline page.
 * 
 * @author aron
 */
public class OutlineSortingAction extends Action 
{
	private StructuredViewer viewer;
	
	private static String OUTLINE_SORT = "outline_sort";
	
	public OutlineSortingAction(StructuredViewer viewer)
	{
		this.viewer = viewer;
		
		//set the button text and image
		setText("Sort");
		setImageDescriptor(SoarEditorPluginImages.getDescriptor(SoarEditorPluginImages.IMG_ALPHAB_SORT));
		
		//get the initial saved state of this action
		Preferences prefs = SoarEditorUIPlugin.getDefault().getPluginPreferences();
		boolean checked = prefs.getBoolean(OUTLINE_SORT);
		
		valueChanged(checked);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() 
	{
		valueChanged(isChecked());
	}
	
	/**
	 * Change whether or not the outline is sorted.
	 * 
	 * @param value
	 */
	private void valueChanged(boolean value)
	{
		setChecked(value);
		
		if(value)
		{
			viewer.setSorter(SoarOutlinePage.SORTER);
		}
		else
		{
			viewer.setSorter(null);
		}
		
		//store the state of the action
		Preferences prefs = SoarEditorUIPlugin.getDefault().getPluginPreferences();
		prefs.setValue(OUTLINE_SORT, value);
	}
}
