/* The Government has SBIR Data rights to this software. All other parties have
 * no rights to use, distribute, reproduce, modify, reverse engineer, or
 * otherwise utilize this software.
 *
 * All ownership rights are retained by Soar Technology, Inc.
 *
 * (C)2016 SoarTech, Proprietary, All Rights Reserved.
 */
package com.soartech.soar.ide.ui.editors.text;

import org.eclipse.swt.layout.GridLayout;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

/**
 * @author preetom.chakraborty
 *
 */
public class SoarEditorKeywordListPreferencePage 
    extends PreferencePage implements IWorkbenchPreferencePage
{
    
    //The list that displays the current keywords
    private List keywordList;
    //The newEntryText is the text where new keywords are specified
    private Text newEntryText;
    //ordered set to prevent duplicate words being added to the command list
    private Collection<String> commandSet = new TreeSet<String>();

    /**
     * 
     */
    public SoarEditorKeywordListPreferencePage()
    {
        setPreferenceStore( SoarEditorUIPlugin.getDefault()
                            .getPreferenceStore() );
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench)
    {
        setPreferenceStore(SoarEditorUIPlugin.getDefault().getPreferenceStore());
      
    }
   
    /**
     * Performs special processing when this page's Restore Defaults button has been pressed.
     * Sets the contents of the nameEntry field to
     * be the default 
     */
    protected void performDefaults() {
        //Turn the list into the list of defaults
        System.out.println("PERFORM DEFAULTS CALLED, HASH CLEARED");
        SoarEditorUIPlugin.getDefault().initializeDefaultPreferences(SoarEditorUIPlugin.getDefault().getPreferenceStore());
        if (keywordList != null)
        {
            keywordList.setItems(SoarEditorUIPlugin.getDefault().getDefaultKeywordsPreference());
        }
        //Reset ordered set to contain standard items
        String[] commandList = SoarEditorUIPlugin.getDefault().getDefaultKeywordsPreference();
        commandSet.clear();
        
        initializeSet(commandList);
    }
    /** 
     * Method declared on IPreferencePage. Save the
     * author name to the preference store. 
     */
    public boolean performOk() {
        System.out.println("PERFORM OK CALLED");
        SoarEditorUIPlugin.getDefault().setKeywordsPreference(keywordList.getItems());
        return super.performOk();
    }
    
    private void initializeSet(String[] commandList)
    {
        //Add to ordered set
        for (String cmd: commandList)
        {
            commandSet.add(cmd);
        }
    }
    

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    /* Runs every time the preferences pane shows up-the hash is recreated from scratch each time.
     * @see PreferencePage#createContents(Composite)
     */
    protected Control createContents(Composite parent) {
        System.out.println("CREATE CONTENTS");
        
        Composite entryTable = new Composite(parent, SWT.NULL);

        //Create a data that takes up the extra space in the dialog .
        GridData data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        entryTable.setLayoutData(data);

        GridLayout layout = new GridLayout();
        entryTable.setLayout(layout);
        
        //Add in a dummy label for spacing
        new Label(entryTable,SWT.NONE);

        keywordList = new List(entryTable, SWT.BORDER | SWT.V_SCROLL);

        keywordList.setItems(SoarEditorUIPlugin.getDefault().getKeywordsPreference());
        
        //Initialize the ordered set to all words in the store as well.
        String[] commandList = SoarEditorUIPlugin.getDefault().getKeywordsPreference();
        initializeSet(commandList);

        //Create a data that takes up the extra space in the dialog and spans both columns.
        data = new GridData(GridData.FILL_BOTH);
        keywordList.setLayoutData(data);
        
        Composite buttonComposite = new Composite(entryTable,SWT.NULL);
        
        GridLayout buttonLayout = new GridLayout();
        buttonLayout.numColumns = 2;
        buttonComposite.setLayout(buttonLayout);

        //Create a data that takes up the extra space in the dialog and spans both columns.
        data = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
        buttonComposite.setLayoutData(data);        
        
        Button addButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

        addButton.setText("Add to List"); //$NON-NLS-1$
        addButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                String text = newEntryText.getText();
                //Only add words that are not empty and are not already in the command list.
                if (!text.trim().isEmpty() && !commandSet.contains(text)){
                    keywordList.add(newEntryText.getText(), keywordList.getItemCount());
                    commandSet.add(text);
                    //Set the keywords to include the recently added command
                    SoarEditorUIPlugin.getDefault().setKeywordsPreference(commandSet.toArray(new String[commandSet.size()]));
                    newEntryText.setText("");
                }
            }
        });
        
        newEntryText = new Text(buttonComposite, SWT.BORDER);
        //Create a data that takes up the extra space in the dialog .
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        newEntryText.setLayoutData(data);
        
        
        Button removeButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

        removeButton.setText("Remove Selection"); //$NON-NLS-1$
        removeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                System.out.println("ordered set REMOVE:");
                System.out.println(keywordList.getItem(keywordList.getSelectionIndex()));
                //Remove the discarded command from the command list hash
                commandSet.remove(keywordList.getItem(keywordList.getSelectionIndex()));
                keywordList.remove(keywordList.getSelectionIndex());
                //Set the keywords to not have the recently deleted command
                SoarEditorUIPlugin.getDefault().setKeywordsPreference(commandSet.toArray(new String[commandSet.size()]));
            }
        });
        data = new GridData();
        data.horizontalSpan = 2;
        removeButton.setLayoutData(data);
    
        return entryTable;
    }

}
