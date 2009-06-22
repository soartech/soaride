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
package com.soartech.soar.ide.ui.linking;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Soar Html Hyperlink representing an html page to be opened.
 * 
 * @author aron
 *
 */
public class SoarHtmlHyperlink implements IHyperlink 
{
	private IRegion region;
	private String file;
	
	private static Shell shell;
	private static Point shellLocation;
	private static Point shellSize;

	public SoarHtmlHyperlink(IRegion region, String file)
	{
		this.region = region;
		this.file = file;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
	 */
	public IRegion getHyperlinkRegion() 
	{
		return this.region;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
	 */
	public String getHyperlinkText() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
	 */
	public String getTypeLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
	 */
	public void open() 
	{
		final Display display = Display.getDefault();
		
		display.asyncExec(new Runnable() {
			public void run() {
				
				if(shell != null && !shell.isDisposed())
				{
					shell.close();
				}
				
				int index = file.lastIndexOf("/") + 1;
				String filename = file.substring(index);
				
			    shell = new Shell(display);
				shell.setText("Soar/Tcl Help Browser - " + filename);
				shell.setLayout(new GridLayout());
				
				if(shellLocation != null && shellSize != null)
				{
					shell.setLocation(shellLocation);
					shell.setSize(shellSize);
				}
				else
				{
					shell.setSize(600, 600);
				}
				
				Composite compTools = new Composite(shell, SWT.NONE);
				GridData data = new GridData(GridData.FILL_HORIZONTAL);
				compTools.setLayoutData(data);
				compTools.setLayout(new GridLayout(1, false));
				
				ToolBar navBar = new ToolBar(compTools, SWT.NONE);
				navBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
				final ToolItem back = new ToolItem(navBar, SWT.PUSH);
				back.setText("Back");
				back.setEnabled(false);
				final ToolItem forward = new ToolItem(navBar, SWT.PUSH);
				forward.setText("Forward");
				forward.setEnabled(false);
				
				Composite comp = new Composite(shell, SWT.NONE);
				data = new GridData(GridData.FILL_BOTH);
				comp.setLayoutData(data);
				comp.setLayout(new FillLayout());
				final SashForm form = new SashForm(comp, SWT.HORIZONTAL);
				form.setLayout(new FillLayout());
				
				final Browser browser = new Browser(form, SWT.BORDER);
				
				back.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						browser.back();
					}
				});
				forward.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						browser.forward();
					}
				});
				
				browser.addLocationListener(new LocationListener() {
					public void changed(LocationEvent event) {
						Browser browser = (Browser)event.widget;
						back.setEnabled(browser.isBackEnabled());
						forward.setEnabled(browser.isForwardEnabled());
					}
					public void changing(LocationEvent event) {
					}
				});
				
				shell.addControlListener(new ControlListener() {

					public void controlMoved(ControlEvent e) {
						shellLocation = shell.getLocation();
					}

					public void controlResized(ControlEvent e) {
						shellSize = shell.getSize();
					}
					
				});
				
				browser.setUrl(file);
				
				
				shell.open();
			}
		});		
	}
}
