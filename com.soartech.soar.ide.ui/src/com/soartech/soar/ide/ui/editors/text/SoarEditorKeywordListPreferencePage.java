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
import com.soartech.soar.ide.ui.editors.text.rules.CommandRule;

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
    
    //This set contains all the keywords added by the user. Used to prevent duplicates.
    //The keywords in this set are the only ones synced with the preference store and have empty proc
    //calls made for them in SoarAgent.java
    private Collection<String> newKeywordSet = new TreeSet<String>();
    
    //This set contains all the original JSoar keywords. Prevents the user from duplicating one of the 
    //JSoar commands and overwriting it with an empty proc call. 
    //Not synced with the preferences store, just pulled from the static arrays in CommandRule.java.
    private Collection<String> oldKeywordSet = new TreeSet<String>();

    /**
     * 
     */
    public SoarEditorKeywordListPreferencePage()
    {
        setPreferenceStore( SoarEditorUIPlugin.getDefault()
                            .getPreferenceStore() );
        
        System.out.println("CTOR CALLED");
        
        //Initialize each set separately

        //Copy only the original keywords into the old keyword map
        //We don't want to include the custom commands array, so we only take the first 8
        for (int i = 0; i < 8; ++i)
        {
            initializeSet(oldKeywordSet, CommandRule.ALL_COMMANDS[i]);
        }
        //Initialize the ordered set to all words in the store as well.
        String[] commandList = SoarEditorUIPlugin.getDefault().getKeywordsPreference();
        initializeSet(newKeywordSet, commandList);
        
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
        System.out.println("PERFORM DEFAULTS CALLED, SET CLEARED");
        //Reset default preferences; empty by default
        SoarEditorUIPlugin.getDefault().initializeDefaultPreferences(SoarEditorUIPlugin.getDefault().getPreferenceStore());
        if (keywordList != null)
        {
            //Needs to take items from oldKeywordSet only
            keywordList.setItems(oldKeywordSet.toArray(new String[newKeywordSet.size()]));
        }
        //Clear the set containing all the user added commands.
        newKeywordSet.clear();
        
    }
    /** 
     * Method declared on IPreferencePage. Save the
     * author name to the preference store. 
     */
    public boolean performOk() {
        System.out.println("PERFORM OK CALLED");
        //Only takes the items from the newKeywordSet to set the preference store
        SoarEditorUIPlugin.getDefault().setKeywordsPreference(newKeywordSet.toArray(new String[newKeywordSet.size()]));
        return super.performOk();
    }
    
    //Method to easily create a set from an array of strings
    private void initializeSet(Collection<String> set, String[] commandList)
    {
        //Add to ordered set
        for (String cmd: commandList)
        {
            set.add(cmd);
        }
    }
    
    //Method that concatenates two string arrays into one and returns it.
    private String[] concatStrArr(String[] a, String[] b) {
        int aLen = a.length;
        int bLen = b.length;
        String[] result = new String[aLen+bLen];
        System.arraycopy(a, 0, result, 0, aLen);
        System.arraycopy(b, 0, result, aLen, bLen);
        return result;
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
        
        //Create visible from the newKeyword Set + oldKeywordSet
        String[] fullKeywordList = concatStrArr(newKeywordSet.toArray((new String[newKeywordSet.size()])), 
                                                oldKeywordSet.toArray(new String[oldKeywordSet.size()]));
        keywordList.setItems(fullKeywordList);
        
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
                //Only add words that are not empty and are not already in either command list.
                if (!text.trim().isEmpty() && !newKeywordSet.contains(text) && !oldKeywordSet.contains(text)){
                    keywordList.add(newEntryText.getText(), keywordList.getItemCount());
                    newKeywordSet.add(text);
                    //Set the keywords store to include the recently added command
                    SoarEditorUIPlugin.getDefault().setKeywordsPreference(newKeywordSet.toArray(new String[newKeywordSet.size()]));
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
                String toDelete = keywordList.getItem(keywordList.getSelectionIndex());
                //Check to make sure they aren't deleting a JSoar keyword and selected a value from the list
                if (keywordList.getSelectionIndex() >= 0 && newKeywordSet.contains(toDelete))
                {
                    System.out.println("ordered set REMOVE:");
                    System.out.println(toDelete);
                    //Remove the discarded command from the user keyword list set
                    newKeywordSet.remove(keywordList.getItem(keywordList.getSelectionIndex()));
                    keywordList.remove(keywordList.getSelectionIndex());
                    //Set the keywords to not have the recently deleted command
                    SoarEditorUIPlugin.getDefault().setKeywordsPreference(newKeywordSet.toArray(new String[newKeywordSet.size()]));
                }
            }
        });
        //Export button?
//        Button exportButton= new Button(buttonComposite, SWT.PUSH);
//        exportButton.setText("Export...");
//        exportButton.addListener(SWT.Selection, new Listener() {
//            public void handleEvent(Event e) {
//                export();
//            }
//        });

        data = new GridData();
        data.horizontalSpan = 2;
        removeButton.setLayoutData(data);
    
        return entryTable;
    }

}
