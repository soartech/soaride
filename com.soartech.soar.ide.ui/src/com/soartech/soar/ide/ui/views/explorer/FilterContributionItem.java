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
package com.soartech.soar.ide.ui.views.explorer;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The UI text widget that reads a filter string for the 
 * soar package explorer view. Uses a timer so that the view isn't update
 * until slightly after they stop typing. Otherwise, with a large project,
 * there is a delay after every keystroke.
 * 
 * @author aron
 *
 */
public class FilterContributionItem extends ControlContribution
{
	private Text textWidget;
    private SoarExplorerView view;
    private Timer timer = new Timer("Explorer View Filter Timer", true);
	private TimerTask timerTask = null; 
    
	protected FilterContributionItem(String id, SoarExplorerView view) 
	{
		super(id);
		
        this.view = view;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ControlContribution#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createControl(Composite parent) 
	{	
        Composite composite = new Composite(parent, SWT.NONE);
        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.marginHeight = 0;
        layout.spacing = 5;
        composite.setLayout(layout);
        
        Label label = new Label(composite, SWT.NONE | SWT.RIGHT);
        label.setText("Filter: ");
		textWidget = new Text(composite, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		textWidget.addModifyListener(new Listener());
		textWidget.setLayoutData(new RowData(100, 12));
        
		return composite;
	}

	/**
	 * @return the text
	 */
	public Text getTextWidget() 
	{
		return textWidget;
	}
    
    private class Listener implements ModifyListener
    {

        /* (non-Javadoc)
         * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
         */
        public void modifyText(ModifyEvent e)
        {
            // Cancel and reschedule timer with each key stroke. Once they stop
            // typing, the timer will fire and update the view.
            if(timerTask != null)
            {
                timerTask.cancel();
            }
            timerTask = new FilterTimerTask();
            timer.schedule(timerTask, 500);
        }
    }
    
    private class FilterTimerTask extends TimerTask 
    {
        public void run()
        {
            Display.getDefault().syncExec(new Runnable() {

                public void run()
                {
                    if(!textWidget.isDisposed())
                    {
                        view.setFilterString(textWidget.getText());
                    }
                }});
        }
    };
}
