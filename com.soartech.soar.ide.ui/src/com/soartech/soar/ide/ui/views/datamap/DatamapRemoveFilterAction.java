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
package com.soartech.soar.ide.ui.views.datamap;

import org.eclipse.jface.action.Action;

import com.soartech.soar.ide.ui.SoarEditorPluginImages;
    
public class DatamapRemoveFilterAction extends Action
{
    static final String ID = "com.soartech.soar.ide.ui.views.datamap.RemoveFilterAction";
    static final String FILTER = "Remove Filter";

    private SoarDatamapView view;

    public DatamapRemoveFilterAction(SoarDatamapView view)
    {
        super(FILTER, Action.AS_PUSH_BUTTON);

        this.view = view;
        
        setToolTipText(FILTER);
        setImageDescriptor(SoarEditorPluginImages.getDescriptor(SoarEditorPluginImages.IMG_DATAMAP_REMOVE_FILTER));
        setId(ID);
        
        disable();
    }

    public void run()
    {
        view.clearFilter();
    }
    
    private void setRemoveFilterDisplay(String text, boolean enabled)
    {
        // Apparently the tooltip only changes successfully if the
        // control is enabled at the time of setting.
        setEnabled(true);
        setToolTipText(FILTER + (text.length() > 0 ? " " : "") + text);
        
        setEnabled(enabled);
    }
    
    public void enable(String filterDescription)
    {
        setRemoveFilterDisplay(filterDescription, true);
    }
    
    public void disable()
    {
        setRemoveFilterDisplay("", false);
    }
}

