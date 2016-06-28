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
package com.soartech.soar.ide.ui;

import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.soartech.soar.ide.ui.editors.text.SoarContext;
import com.soartech.soar.ide.ui.editors.text.SoarEditorMessages;
import com.soartech.soar.ide.ui.editors.text.SyntaxColorManager;

/**
 * The main plugin class to be used in the desktop.
 */
public class SoarEditorUIPlugin extends AbstractUIPlugin
{

    public static final String PLUGIN_ID = "com.soartech.soar.ide.ui";

    /** Unique key for referencing this plug-in's template store */
    private static final String CUSTOM_TEMPLATES_KEY = "com.soartech.soar.ide.ui.customtemplates";

    /** The shared plug-in instance. */
    private static SoarEditorUIPlugin plugin;

    private ResourceBundle resourceBundle = null;

    private ContributionTemplateStore templateStore;

    private ContributionContextTypeRegistry contextRegistry;

    private FormColors formColors;
    
    public static final String DEFAULT_CMDS = "";
    //These are the default commands that will show up in the list...if we want everything to show, just retrieve all keywords and .join
    //"cli;indifferent-selection;max-chunks;svs;watch;script;gp;gp-max;pbreak;verbose;max-dc-time;max-goal-depth;"
    //+ "max-memory-usage;predict;wma;select;set-stop-phase;relay-input;capture-input";
    
    public static final String KEYWORDS_PREFERENCE = "keywords";
    
    public static final String PREFERENCE_DELIMITER = ";";

    /**
     * The constructor.
     */
    public SoarEditorUIPlugin()
    {
        plugin = this;
        try
        {
            resourceBundle = ResourceBundle
                    .getBundle("com.soartech.soar.ide.ui.SoarEditorPluginResources");
        }
        catch (MissingResourceException x)
        {
            resourceBundle = null;
        }
        
    }

    /**
     * This method is called upon plug-in activation
     */
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
    }

    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(BundleContext context) throws Exception
    {
        SyntaxColorManager.dispose();
        super.stop(context);
        plugin = null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
     */
    @Override
    protected void initializeImageRegistry(ImageRegistry reg)
    {
        super.initializeImageRegistry(reg);
        
        SoarEditorPluginImages.registerImages(reg);
    }

    /**
     * @return the shared instance.
     */
    public static SoarEditorUIPlugin getDefault()
    {
        return plugin;
    }

    /**
     * Get the current workspace.
     * 
     * @return The workspace.
     */
    public static IWorkspace getWorkspace()
    {
        return ResourcesPlugin.getWorkspace();
    }

    /**
     * @return The resource bundle for this plugin.
     */
    public ResourceBundle getResourceBundle()
    {
        return resourceBundle;
    }

    public static void log(Throwable e)
    {
        log(new Status(IStatus.ERROR, getPluginId(), 0,
                SoarEditorMessages.SoarEditor_internal_error, e));
    }

    /**
     * @return this plugin's symbolic id
     */
    public static String getPluginId()
    {
        return getDefault().getBundle().getSymbolicName();
    }

    public static void log(Exception e)
    {
        getDefault().getLog().log(
                new Status(IStatus.ERROR, PLUGIN_ID, 0, e.getMessage(), e));
    }

    public static void log(IStatus status)
    {
        getDefault().getLog().log(status);
    }

    /**
     * @return the plugin's template store
     */
    public TemplateStore getTemplateStore()
    {
        if (templateStore == null)
        {
            templateStore = new ContributionTemplateStore(
                    getContextTypeRegistry(), SoarEditorUIPlugin.getDefault()
                            .getPreferenceStore(), CUSTOM_TEMPLATES_KEY);
            try
            {
                templateStore.load();
            }
            catch (IOException exception)
            {
                log(exception);
            }
        }
        return templateStore;
    }

    /**
     * @return the context type registry for this plug-in instance
     */
    public ContextTypeRegistry getContextTypeRegistry()
    {
        if (contextRegistry == null)
        {
            contextRegistry = new ContributionContextTypeRegistry();
            contextRegistry.addContextType(SoarContext.SOAR_CONTEXT_TYPE);
        }
        return contextRegistry;
    }

    public FormColors getFormColors(Display display)
    {
        if (formColors == null)
        {
            formColors = new FormColors(display);
            formColors.markShared();
        }
        return formColors;
    }
    
    
    /***********************For adding custom commands**********************/
    
    public void initializeDefaultPreferences(IPreferenceStore store)
    {
        store.setDefault(KEYWORDS_PREFERENCE, DEFAULT_CMDS);
    }
    /**
     * Return the keywords preference default
     * as an array of Strings.
     * @return String[]
     */
    public String[] getDefaultKeywordsPreference() {
        return convert(
            getPreferenceStore().getDefaultString(KEYWORDS_PREFERENCE));
    }

    /**
     * Return the keywords preference as an array of
     * Strings.
     * @return String[]
     */
    public String[] getKeywordsPreference() {
        
        return convert(getPreferenceStore().getString(KEYWORDS_PREFERENCE));
    }

    /**
     * Convert the supplied PREFERENCE_DELIMITER delimited
     * String to a String array.
     * @return String[]
     */
    private String[] convert(String preferenceValue) {
        StringTokenizer tokenizer =
            new StringTokenizer(preferenceValue, PREFERENCE_DELIMITER);
        int tokenCount = tokenizer.countTokens();
        String[] elements = new String[tokenCount];

        for (int i = 0; i < tokenCount; i++) {
            elements[i] = tokenizer.nextToken();
        }

        return elements;
    }

    /**
     * Set the commands preference
     * @param String [] elements - the Strings to be 
     *  converted to the preference value
     */
    public void setKeywordsPreference(String[] elements) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < elements.length; i++) {
            buffer.append(elements[i]);
            buffer.append(PREFERENCE_DELIMITER);
        }
        getPreferenceStore().setValue(KEYWORDS_PREFERENCE, buffer.toString());
    }
}
