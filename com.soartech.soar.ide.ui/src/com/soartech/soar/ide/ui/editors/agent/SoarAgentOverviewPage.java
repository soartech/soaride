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
package com.soartech.soar.ide.ui.editors.agent;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
//import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;

/**
 * @author ray
 */
public class SoarAgentOverviewPage extends FormPage
{
    public static final String ID = "overview";
    
    private SoarAgentEditor editor;
    private Text startFileText;
    
    public SoarAgentOverviewPage(SoarAgentEditor editor)
    {
        super(editor, ID, "Overview");
        
        this.editor = editor;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
     */
    @Override
    protected void createFormContent(IManagedForm managedForm)
    {
        ScrolledForm form = managedForm.getForm();
        
        ISoarAgent agent = editor.getAgent();
        if(agent != null)
        {
            form.setText("Overview of Agent '" +agent.getName() + "'");
        }
        else
        {
            form.setText("Soar support is not enabled for this project.");
            return;
        }
        
        Composite body = form.getBody();
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.verticalSpacing = 10;
        body.setLayout(layout);
        
        createStartFileControls(managedForm, body, layout);
        createFileSelector(managedForm, body, layout);
        createOptionsSection(managedForm, body, layout);
        form.reflow(true);
    }

    private String getStartFileString()
    {
        ISoarAgent agent = editor.getAgent();
        if(agent == null)
        {
            return "";
        }
        IFile startFile = agent.getStartFile();
        
        return startFile != null ? startFile.getFullPath().toPortableString() : "";
    }
    
    
    private void createStartFileControls(IManagedForm managedForm, Composite body, GridLayout layout)
    {
        //ScrolledForm form = managedForm.getForm();
        FormToolkit toolkit = managedForm.getToolkit();
        
        
        toolkit.createLabel(body, "Start file:");
        startFileText = toolkit.createText(body, getStartFileString());
        startFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        startFileText.addListener(SWT.DefaultSelection, new Listener() {

            public void handleEvent(Event event)
            {
                commitStartFileText();
            }});
        startFileText.addListener(SWT.FocusOut, new Listener() {

            public void handleEvent(Event event)
            {
                commitStartFileText();
            }});
        
        Button button = toolkit.createButton(body, " ... ", SWT.NONE);
        button.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e)
            {
                selectStartFile();
            }});
    }
    
    /**
     * @param body
     * @param layout
     */
    private void createFileSelector(IManagedForm managedForm, Composite body, GridLayout layout)
    {
        final ScrolledForm form = managedForm.getForm();
        final FormToolkit toolkit = managedForm.getToolkit();
        
        GridData gd = new GridData();
        gd.horizontalSpan = layout.numColumns;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;  
        
        Section section = toolkit.createSection(body, 
                Section.DESCRIPTION|Section.TITLE_BAR|
                Section.TWISTIE|Section.EXPANDED);
        section.setText("Agent File Selection");
        section.setDescription("Choose files and directories to include in this agent. " + 
                "Omitted files or directories will not be processed by the Soar IDE. " +
                "Double-click a file to open it in an editor.");
        section.addExpansionListener(new ExpansionAdapter() {
            public void expansionStateChanged(ExpansionEvent e) {
             form.reflow(true);
            }
           });
        section.setLayoutData(gd);
        
        Composite sectionBody = toolkit.createComposite(section);
        section.setClient(sectionBody);
        sectionBody.setLayout(new GridLayout());
        gd = new GridData();
        gd.horizontalSpan = layout.numColumns;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;  
        
        new SoarAgentFileSelector(editor, sectionBody);
    }
    
    
    private void createOptionsSection(IManagedForm managedForm, Composite body, GridLayout layout)
    {
        final ScrolledForm form = managedForm.getForm();
        final FormToolkit toolkit = managedForm.getToolkit();
        
        GridData gd = new GridData();
        gd.horizontalSpan = layout.numColumns;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;  
        
        Section section = toolkit.createSection(body, 
                Section.DESCRIPTION|Section.TITLE_BAR|
                Section.TWISTIE|Section.EXPANDED);
        section.setText("Options");
        //section.setDescription("Agent parsing and processing options.");
        section.addExpansionListener(new ExpansionAdapter() {
            public void expansionStateChanged(ExpansionEvent e) {
             form.reflow(true);
            }
           });
        section.setLayoutData(gd);
        
        Composite sectionBody = toolkit.createComposite(section);
        section.setClient(sectionBody);
        sectionBody.setLayout(new GridLayout());
    }
    
    private void setStartFile(IFile file)
    {
        ISoarAgent agent = editor.getAgent();
        IFile currentFile = agent.getStartFile();
        boolean changed = false;
        if(file == null && file != currentFile)
        {
            changed = true;
        }
        else if(file != null && !file.equals(currentFile))
        {
            changed = true;
        }
        agent.setStartFile(file);
        startFileText.setText(getStartFileString());
        editor.setDirty(editor.isDirty() || changed);
    }
    
    private void selectStartFile()
    {
        ISoarAgent agent = editor.getAgent();
        IFile currentSoarFile = agent.getStartFile();
        
        SoarAgentInitialFileSelector dialog = 
            new SoarAgentInitialFileSelector(getSite().getShell(), editor);
        dialog.setTitle("Select start file for agent " + editor.getAgentName());
        if(currentSoarFile != null)
        {
            dialog.setInitialSelections(new Object[] { currentSoarFile } );
        }
        dialog.open();
        
        Object[] result = dialog.getResult();
        if(result == null || result.length == 0)
        {
            return;
        }

        setStartFile((IFile) result[0]);
    }
    
    /**
     * Called when enter is hit, or focus is lost from the start file text box. 
     */
    private void commitStartFileText()
    {
        ISoarAgent agent = editor.getAgent();
        IProject project = agent.getSoarProject().getProject();
        
        final String text = startFileText.getText().trim();
        
        // Clearing the text box clears the start file
        if(text.length() == 0)
        {
            setStartFile(null);
        }
        
        // See if it's a path
        IPath path = new Path(text);
        if(!path.isValidPath(text))
        {
            startFileText.setText(getStartFileString());
            return;
        }
        
        // See if it refers to an eclipse file resource
        IResource resource = SoarModelTools.getEclipseResource(path);
        if(resource == null || !(resource instanceof IFile))
        {
            startFileText.setText(getStartFileString());
            return;
        }
        
        // Make sure it's in the same project as the agent
        if(resource.getProject() != project)
        {
            startFileText.setText(getStartFileString());
            return;
        }
        
        // Make sure it's a Soar file
        IFile file = (IFile) resource;
        try
        {
            if(agent.getSoarModel().getFile(file) == null)
            {
                startFileText.setText(getStartFileString());
                return;
            }
        }
        catch (SoarModelException e)
        {
            startFileText.setText(getStartFileString());
            return;
        }
        
        setStartFile(file);
    }    
}
