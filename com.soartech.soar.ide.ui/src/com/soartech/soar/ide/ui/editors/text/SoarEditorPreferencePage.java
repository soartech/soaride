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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * <code>SoarEditorPreferencePage</code> top level preference page.
 *
 * TODO: Re-write this page using FieldEditorPreferencePage which makes
 * all this stuff way easier.
 * 
 * @author annmarie.steichmann
 * @author aron
 */
public class SoarEditorPreferencePage extends PreferencePage 
									  implements IWorkbenchPreferencePage 
{
	public static final String TAB_WIDTH = "SoarEditorPreferencePage.tabWidth";
	public static final String CONVERT_TABS = "SoarEditorPreferencePage.convertTabs";
	
	private int initialTabWidth = 0;
	private boolean initialConvertTabs = true;
	
	private Label tabWidthLabel;
	private Text tabWidthText;
	private Link tabWidthLink;
	
	private Button convertTabsButton;
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(final Composite parent) 
	{
		Composite prefComposite = new Composite( parent, SWT.NULL );
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 3;
		layout.verticalSpacing = 10;
		prefComposite.setLayout(layout);
		        
		createNumberSpaces(prefComposite, parent);
		
		createUseTabsToSpaces(prefComposite);
		
		return prefComposite;
	}
	
	private void createNumberSpaces(Composite parent, final Composite pageParent)
	{
		tabWidthLabel = new Label(parent, SWT.LEFT);
		tabWidthLabel.setText("Displayed tab width:       ");
		
		tabWidthText = new Text(parent, SWT.BORDER | SWT.LEFT);
		tabWidthText.setTextLimit(2);
		tabWidthText.setText(Integer.toString(initialTabWidth));
		
		GridData textData = new GridData();
		textData.horizontalSpan = 1;
		textData.grabExcessHorizontalSpace = true;
		tabWidthText.setLayoutData(textData);
		
		tabWidthText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				try {
					Integer.parseInt(tabWidthText.getText());
					setValid(true);
					setErrorMessage(null);
				} catch (NumberFormatException nfe) {
					setValid(false);
					setErrorMessage("Invalid Tab Width");
				}
			}
		});
		
		tabWidthLink = new Link(parent, SWT.NONE);
		tabWidthLink.setText("[Using tab width from the " + 
			"<a href=\"org.eclipse.ui.preferencePages.GeneralTextEditor\">Text Editors</a> page]");
		tabWidthLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(pageParent.getShell(), e.text, null, null); 
			}
		});
		
		enableTabWidthText(initialConvertTabs);
	}
	
	private void createUseTabsToSpaces(Composite parent)
	{	
		convertTabsButton = new Button(parent, SWT.CHECK);
		convertTabsButton.setText("Convert tabs to spaces");
		convertTabsButton.setSelection(initialConvertTabs);
		
		GridData buttonData = new GridData();
		buttonData.horizontalSpan = 2;
		convertTabsButton.setLayoutData(buttonData);
		
		convertTabsButton.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) { }

			public void widgetSelected(SelectionEvent e) {
				enableTabWidthText(convertTabsButton.getSelection());
			}
		});
	}
	
	private void enableTabWidthText(boolean enable)
	{
		if(enable)
		{
			tabWidthLabel.setEnabled(true);
			tabWidthText.setEnabled(true);
			tabWidthLink.setVisible(false);			
		}
		else
		{
			tabWidthLabel.setEnabled(false);
			tabWidthText.setEnabled(false);
			tabWidthLink.setVisible(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) 
	{
		IPreferenceStore prefs = getPreferenceStore();
		
		if(!prefs.contains(TAB_WIDTH))
		{	
			prefs.setValue(TAB_WIDTH, 3);
		}
		
		if(!prefs.contains(CONVERT_TABS))
		{
			prefs.setValue(CONVERT_TABS, true);
		}
		
		initialTabWidth = prefs.getInt(TAB_WIDTH);
		initialConvertTabs = prefs.getBoolean(CONVERT_TABS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#getPreferenceStore()
	 */
	@Override
	public IPreferenceStore getPreferenceStore() 
	{
		return EditorsUI.getPreferenceStore();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performCancel()
	 */
	@Override
	public boolean performCancel() 
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() 
	{
		IPreferenceStore prefs = getPreferenceStore();
		
		tabWidthText.setText(Integer.toString(prefs.getDefaultInt(TAB_WIDTH)));
		convertTabsButton.setSelection(prefs.getDefaultBoolean(CONVERT_TABS));
		
		enableTabWidthText(prefs.getDefaultBoolean(CONVERT_TABS));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	@Override
	protected void performApply() 
	{
		IPreferenceStore prefs = getPreferenceStore();
						
		prefs.setValue(TAB_WIDTH, Integer.parseInt(tabWidthText.getText()));
		prefs.setValue(CONVERT_TABS, convertTabsButton.getSelection());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() 
	{
		performApply();
		return true;
	}
}
