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
package com.soartech.soar.ide.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Scrollable;
//import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.editors.text.EditorsUI;

import com.soartech.soar.ide.ui.editors.text.SyntaxColorManager;


/**
 * <code>SoarEditorColoringPreferencePage</code> configures the Soar Editor syntax highlighting
 * preferences.
 * 
 * NOTE: This class is modelled after <code>org.eclipse.jdt.internal.ui.preferences.JavaEditorColoringConfigurationBlock</code>.
 * 
 * @author annmarie.steichmann
 *
 */
public class SoarEditorColoringPreferencePage extends PreferencePage 
			implements IWorkbenchPreferencePage {
	
	/**
	 * Color list model item.
	 */
	private static class HighlightingColorListItem {
		
		/** Display name */
		private String fDisplayName;
		/** Color preference key */
		private String fColorKey;
		/** Bold preference key */
		private String fBoldKey;
		/** Italic preference key */
		private String fItalicKey;
		/** Strikethrough preference key */
		private String fStrikethroughKey;
		/** Underline preference key */
		private String fUnderlineKey;
		/** Enable preference key */
		private String fEnableKey;
		
		/**
		 * Initialize the item with the given values.
		 * @param displayName the display name
		 * @param colorKey the color preference key
		 * @param boldKey the bold preference key
		 * @param italicKey the italic preference key
		 * @param underlineKey the underline preference key
		 * @param enableKey the enable preference key
		 */
		public HighlightingColorListItem(String displayName, String colorKey, String boldKey, String italicKey, String underlineKey, String enableKey) {
			fDisplayName = displayName;
			fColorKey = colorKey;
			fBoldKey = boldKey;
			fItalicKey = italicKey;
			fUnderlineKey = underlineKey;
			fEnableKey = enableKey;
		}
		
		/**
		 * @return the bold preference key
		 */
		public String getBoldKey() {
			return fBoldKey;
		}
		
		/**
		 * @return the bold preference key
		 */
		public String getItalicKey() {
			return fItalicKey;
		}
		
		/**
		 * @return the strikethrough preference key
		 */
		public String getStrikethroughKey() {
			return fStrikethroughKey;
		}
		
		/**
		 * @return the underline preference key
		 */
		public String getUnderlineKey() {
			return fUnderlineKey;
		}
		
		/**
		 * @return the color preference key
		 */
		public String getColorKey() {
			return fColorKey;
		}
		
		/**
		 * @return the display name
		 */
		public String getDisplayName() {
			return fDisplayName;
		}
		
		/**
		 * @return the enable key
		 */
		public String getEnableKey() {
			return fEnableKey;
		}
	}
	
	/**
	 * Color list label provider.
	 */
	private class ColorListLabelProvider extends LabelProvider {
		/*
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element instanceof String)
				return (String) element;
			return ((HighlightingColorListItem)element).getDisplayName();
		}
	}
	
	/**
	 * Color list content provider.
	 */
	private class ColorListContentProvider implements ITreeContentProvider {
	
		/*
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return new String[] {fSoarCategory, fCommentsCategory};
		}
	
		/*
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}
	
		/*
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof String) {
				String entry = (String) parentElement;
				if (fSoarCategory.equals(entry))
					return fListModel.subList(2, fListModel.size()).toArray();
				if (fCommentsCategory.equals(entry))
					return fListModel.subList(0, 2).toArray();
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof String)
				return null;
			int index= fListModel.indexOf(element);
			if (index >= 2)
				return fSoarCategory;
			return fCommentsCategory;
		}

		public boolean hasChildren(Object element) {
			return element instanceof String;
		}
	}	
	
	private static final String LINK_TEXT = 
			"Default colors and font can be configured on the " +
			"<a href=\"org.eclipse.ui.preferencePages.GeneralTextEditor\">Text Editors</a> " +
			"and on the " +
			"<a href=\"org.eclipse.ui.preferencePages.ColorsAndFonts\">Colors and Fonts</a> " +
			"preference page.";
	
	private static final String ENABLE = "Enable";
	private static final String BOLD = "Bold";
	private static final String ITALIC = "Italic";
	private static final String UNDERLINE = "Underline";
	
	private final String[][] fSyntaxColorListModel = new String[][] {
		{ "Block comment", SyntaxColorManager.BLOCK_COMMENT_RGB },
		{ "Inline comment", SyntaxColorManager.INLINE_COMMENT_RGB },
		{ "Arrow background", SyntaxColorManager.ARROW_BG_RGB },
		{ "Arrow foreground", SyntaxColorManager.ARROW_FG_RGB },
		{ "Command", SyntaxColorManager.COMMAND_RGB },
		{ "Disjunct", SyntaxColorManager.DISJUNCT_RGB },
		{ "Flag", SyntaxColorManager.FLAG_RGB },
		{ "Function", SyntaxColorManager.FUNCTION_RGB },
		{ "String", SyntaxColorManager.STRING_RGB },
		{ "Tcl macro background", SyntaxColorManager.TCL_BG_RGB },
		{ "Tcl macro foreground", SyntaxColorManager.TCL_FG_RGB },
		{ "Variable", SyntaxColorManager.VARIABLE_RGB },
		{ "Tcl variable", SyntaxColorManager.TCL_VAR_RGB }
	};
	
	private final String fSoarCategory = "Soar";
	private final String fCommentsCategory = "Comments";
	
	private List<HighlightingColorListItem> fListModel = 
								new ArrayList<HighlightingColorListItem>();
	
	private StructuredViewer fListViewer;
//	private Button fEnableCheckBox;
	private Label fColorEditorLabel;
//	private Button fBoldCheckBox;
//	private Button fItalicCheckBox;
//	private Button fUnderlineCheckBox;
//	private Control previewer;
	
	private ColorSelector fSyntaxForegroundColorEditor = null;
	
	private Map<String,RGB> initialValues = new HashMap<String,RGB>();
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(final Composite parent) {
		
		initializeDialogUnits( parent );
		
		Composite colorComposite = new Composite( parent, SWT.NULL );
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		colorComposite.setLayout( layout );
		
		Link link= new Link(colorComposite, SWT.NONE);
		link.setText( LINK_TEXT );
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(parent.getShell(), e.text, null, null); 
			}
		});
		
		GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.widthHint = 150; // only expand further if anyone else requires it
		gridData.horizontalSpan = 2;
		link.setLayoutData(gridData);
		
		addFiller( colorComposite, 1 );
		
		Label label;
		label= new Label(colorComposite, SWT.LEFT);
		label.setText("Element:"); 
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite editorComposite= new Composite(colorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		GridData gd= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		editorComposite.setLayoutData(gd);
		
		fListViewer= new TreeViewer(editorComposite, SWT.SINGLE | SWT.BORDER);
		fListViewer.setLabelProvider(new ColorListLabelProvider());
		fListViewer.setContentProvider(new ColorListContentProvider());
		fListViewer.setSorter(new ViewerSorter() {
			public int category(Object element) {
				// don't sort the top level categories
				if (fSoarCategory.equals(element))
					return 0;
				if (fCommentsCategory.equals(element))
					return 1;
				return 0;
			}
		});		
		
		gd= new GridData(SWT.BEGINNING, SWT.BEGINNING, false, true);
		gd.heightHint= convertHeightInCharsToPixels(9);
		int maxWidth= 0;
		for (Iterator it= fListModel.iterator(); it.hasNext();) {
			HighlightingColorListItem item= (HighlightingColorListItem) it.next();
			maxWidth= Math.max(maxWidth, convertWidthInCharsToPixels(item.getDisplayName().length()));
		}
		ScrollBar vBar= ((Scrollable) fListViewer.getControl()).getVerticalBar();
		if (vBar != null)
			maxWidth += vBar.getSize().x * 3; // scrollbars and tree indentation guess
		gd.widthHint= maxWidth;
		
		fListViewer.getControl().setLayoutData(gd);		
		
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// TODO: Add support for disabling
		
//		fEnableCheckBox= new Button(stylesComposite, SWT.CHECK);
//		fEnableCheckBox.setText( ENABLE ); 
//		gd= new GridData(GridData.FILL_HORIZONTAL);
//		gd.horizontalAlignment= GridData.BEGINNING;
//		gd.horizontalSpan= 2;
//		fEnableCheckBox.setLayoutData(gd);
//		fEnableCheckBox.setEnabled( true );
		
		fColorEditorLabel= new Label(stylesComposite, SWT.LEFT);
		fColorEditorLabel.setText("Color:"); 
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= 20;
		fColorEditorLabel.setLayoutData(gd);
	
		fSyntaxForegroundColorEditor= new ColorSelector(stylesComposite);
		Button foregroundColorButton= fSyntaxForegroundColorEditor.getButton();
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		foregroundColorButton.setLayoutData(gd);
		
		// TODO: Add support for style configuration
		
//		fBoldCheckBox= new Button(stylesComposite, SWT.CHECK);
//		fBoldCheckBox.setText( BOLD ); 
//		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
//		gd.horizontalIndent= 20;
//		gd.horizontalSpan= 2;
//		fBoldCheckBox.setLayoutData(gd);
//		
//		fItalicCheckBox= new Button(stylesComposite, SWT.CHECK);
//		fItalicCheckBox.setText( ITALIC ); 
//		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
//		gd.horizontalIndent= 20;
//		gd.horizontalSpan= 2;
//		fItalicCheckBox.setLayoutData(gd);
//		
//		fUnderlineCheckBox= new Button(stylesComposite, SWT.CHECK);
//		fUnderlineCheckBox.setText( UNDERLINE ); 
//		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
//		gd.horizontalIndent= 20;
//		gd.horizontalSpan= 2;
//		fUnderlineCheckBox.setLayoutData(gd);
//		
//		label= new Label(colorComposite, SWT.LEFT);
//		label.setText("Preview:"); 
//		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		
//		previewer = new Text( colorComposite, SWT.BORDER );
//		gd= new GridData(GridData.FILL_BOTH);
//		gd.widthHint = convertWidthInCharsToPixels(20);
//		gd.heightHint = convertHeightInCharsToPixels(5);
//		previewer.setLayoutData(gd);
		
		fListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSyntaxColorListSelection();
			}
		});	
		
		fListViewer.setInput(fListModel);
		fListViewer.setSelection( new StructuredSelection( fSoarCategory ) );
		
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				PreferenceConverter.setValue(getPreferenceStore(), item.getColorKey(), fSyntaxForegroundColorEditor.getColorValue());
			}
		});	
//		
//		fBoldCheckBox.addSelectionListener(new SelectionListener() {
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// do nothing
//			}
//			public void widgetSelected(SelectionEvent e) {
//				HighlightingColorListItem item= getHighlightingColorListItem();
//				getPreferenceStore().setValue(item.getBoldKey(), fBoldCheckBox.getSelection());
//			}
//		});		
//		
//		fItalicCheckBox.addSelectionListener(new SelectionListener() {
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// do nothing
//			}
//			public void widgetSelected(SelectionEvent e) {
//				HighlightingColorListItem item= getHighlightingColorListItem();
//				getPreferenceStore().setValue(item.getItalicKey(), fItalicCheckBox.getSelection());
//			}
//		});	
//		
//		fUnderlineCheckBox.addSelectionListener(new SelectionListener() {
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// do nothing
//			}
//			public void widgetSelected(SelectionEvent e) {
//				HighlightingColorListItem item= getHighlightingColorListItem();
//				getPreferenceStore().setValue(item.getUnderlineKey(), fUnderlineCheckBox.getSelection());
//			}
//		});
//		
//		fEnableCheckBox.addSelectionListener(new SelectionListener() {
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// do nothing
//			}
//			public void widgetSelected(SelectionEvent e) {
//				
//				HighlightingColorListItem item= getHighlightingColorListItem();
//				boolean enable= fEnableCheckBox.getSelection();
//				getPreferenceStore().setValue(item.getEnableKey(), enable);
//				fEnableCheckBox.setSelection(enable);
//				fSyntaxForegroundColorEditor.getButton().setEnabled(enable);
//				fColorEditorLabel.setEnabled(enable);
//				fBoldCheckBox.setEnabled(enable);
//				fItalicCheckBox.setEnabled(enable);
//				fUnderlineCheckBox.setEnabled(enable);
//			}
//		});	
		
		colorComposite.layout(false);
		
		return colorComposite;
	}
	
	private void addFiller( Composite composite, int horizontalSpan ) {
		
		Label filler= new Label(composite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = horizontalSpan;
		gd.heightHint = convertHeightInCharsToPixels(1)/2;
		filler.setLayoutData(gd);
	}
	
	private void handleSyntaxColorListSelection() {
		
		HighlightingColorListItem item = getHighlightingColorListItem();
		
		if (item == null) {
//			fEnableCheckBox.setEnabled(false);
			fSyntaxForegroundColorEditor.getButton().setEnabled(false);
			fColorEditorLabel.setEnabled(false);
//			fBoldCheckBox.setEnabled(false);
//			fItalicCheckBox.setEnabled(false);
//			fUnderlineCheckBox.setEnabled(false);
			return;
		}
		
		RGB rgb = PreferenceConverter.getColor(getPreferenceStore(), item.getColorKey());
		fSyntaxForegroundColorEditor.setColorValue(rgb);		
//		fBoldCheckBox.setSelection(getPreferenceStore().getBoolean(item.getBoldKey()));
//		fItalicCheckBox.setSelection(getPreferenceStore().getBoolean(item.getItalicKey()));
//		fUnderlineCheckBox.setSelection(getPreferenceStore().getBoolean(item.getUnderlineKey()));

//		fEnableCheckBox.setEnabled(true);
//		boolean enable = getPreferenceStore().getBoolean(item.getEnableKey());
//		fEnableCheckBox.setSelection(enable);
		fSyntaxForegroundColorEditor.getButton().setEnabled(/*enable*/true);
		fColorEditorLabel.setEnabled(/*enable*/true);
//		fBoldCheckBox.setEnabled(enable);
//		fItalicCheckBox.setEnabled(enable);
//		fUnderlineCheckBox.setEnabled(enable);		
	}
	
	private HighlightingColorListItem getHighlightingColorListItem() {
		
		IStructuredSelection selection = (IStructuredSelection) fListViewer.getSelection();
		Object element = selection.getFirstElement();
		if (element instanceof String)
			return null;
		return (HighlightingColorListItem) element;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		
		for( String[] model : fSyntaxColorListModel ) {
			fListModel.add( new HighlightingColorListItem( model[0], model[1], model[1] + BOLD, model[1] + ITALIC, model[1] + UNDERLINE, model[1] + ENABLE ) );
			initialValues.put( model[1], PreferenceConverter.getColor(getPreferenceStore(), model[1]) );
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#getPreferenceStore()
	 */
	@Override
	public IPreferenceStore getPreferenceStore() {
		
		return EditorsUI.getPreferenceStore();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		
		super.performDefaults();
		
		SyntaxColorManager.reset();
		HighlightingColorListItem item = getHighlightingColorListItem();
		RGB value = PreferenceConverter.getColor(getPreferenceStore(), item.getColorKey());
		fSyntaxForegroundColorEditor.setColorValue( value );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performCancel()
	 */
	@Override
	public boolean performCancel() {
		
		// restore initial values
		for( HighlightingColorListItem item : fListModel ) {
			String key = item.getColorKey();
			RGB value = initialValues.get( key );
			PreferenceConverter.setValue(getPreferenceStore(), key, value);
		}
		
		return super.performCancel();
	}
}
