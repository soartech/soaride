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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;


/**
 * <code>SoarEditorPluginImages</code> initializes and loads the icon files for use 
 * by the <code>SoarCorePlugin</code>.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2009-06-22 13:05:30 -0400 (Mon, 22 Jun 2009) $
 */
public class SoarEditorPluginImages {

    /**
     * Constructor for a <code>SoarEditorPluginImages</code> object.
     */
    public SoarEditorPluginImages() {

    }
    
    //set to the package name
    private static final String NAME_PREFIX = SoarEditorUIPlugin.PLUGIN_ID;
        
    private static URL baseIconURL = null;
    
    static
    {
        try 
        {
            //get the file path to where the plugin is installed
            baseIconURL = new URL(
                  Platform.getBundle(SoarEditorUIPlugin.PLUGIN_ID).getEntry("/"),
                  "icons/");
        } catch (MalformedURLException e) 
        {
            System.out.print("Malformed URL");
        }
    }
    
       
    //paths to the icon files
    public static final String IMG_COMMAND = NAME_PREFIX + "command.ico";
    public static final String IMG_FUNCTION = NAME_PREFIX + "function.ico";
    public static final String IMG_VARIABLE = NAME_PREFIX + "variable.ico";
    public static final String IMG_TCL_VARIABLE = NAME_PREFIX + "tcl-variable.ico";
    public static final String IMG_SOAR = NAME_PREFIX + "soar.gif";
    public static final String IMG_CLEAR = NAME_PREFIX + "clear.gif";
    public static final String IMG_COMMAND_OUTLINE = NAME_PREFIX + "command-outline.ico";
    public static final String IMG_PRODUCTION = NAME_PREFIX + "production.gif";
    public static final String IMG_PROCEDURE = NAME_PREFIX + "procedure.gif";
    public static final String IMG_FILE_REFERENCE = NAME_PREFIX + "filereference.gif";
    public static final String IMG_PACKAGE = NAME_PREFIX + "package.gif";
    public static final String IMG_PROJECT = NAME_PREFIX + "prj_obj.gif";
    public static final String IMG_AGENT = NAME_PREFIX + "soaragent.gif";
    public static final String IMG_THIS_AGENT = NAME_PREFIX + "thisagent.gif";
    public static final String IMG_HEADER = NAME_PREFIX + "header.gif";
    public static final String IMG_EXPAND = NAME_PREFIX + "expand.gif";
    public static final String IMG_PACKAGE_OBJ = NAME_PREFIX + "package_obj.gif";
    public static final String IMG_ATTRIBUTE = NAME_PREFIX + "attribute.gif";
    public static final String IMG_ALPHAB_SORT = NAME_PREFIX + "alphab_sort_co.gif";
    public static final String IMG_SOAR_FILE = NAME_PREFIX + "soar_file_16.gif";
    public static final String IMG_START_FILE_OVERLAY = NAME_PREFIX + "startfileoverlay.gif";
    public static final String IMG_ERROR_OVERLAY = NAME_PREFIX + "error_co.gif";
    public static final String IMG_WARNING_OVERLAY = NAME_PREFIX + "warning_co.gif";
    public static final String IMG_HORIZONTAL_ORIENTATION = NAME_PREFIX + "th_horizontal.gif";
    public static final String IMG_VERTICAL_ORIENTATION = NAME_PREFIX + "th_vertical.gif";
    public static final String IMG_AUTOMATIC_ORIENTATION = NAME_PREFIX + "th_automatic.gif";
    public static final String IMG_FOLDING_REGION = NAME_PREFIX + "foldingregion.gif";
    public static final String IMG_DATAMAP_FILTER = NAME_PREFIX + "filter.gif";
    public static final String IMG_DATAMAP_REMOVE_FILTER = NAME_PREFIX + "remove_filter.gif";
    public static final String IMG_TAG = NAME_PREFIX + "tag.gif";
    public static final String IMG_SP = NAME_PREFIX + "sp.gif";
    
    /**
     * Returns the image managed under the given key in this registry.
     * 
     * @param key the image's key
     * @return the image managed under the given key
     */ 
    public static Image get(String key) 
    {
        return SoarEditorUIPlugin.getDefault().getImageRegistry().get(key);
    }
    
    public static ImageDescriptor getDescriptor(String key)
    {
        return SoarEditorUIPlugin.getDefault().getImageRegistry().getDescriptor(key);
    }
    
    static void registerImages(ImageRegistry registry)
    {
        registerImage(registry, IMG_COMMAND);
        registerImage(registry, IMG_FUNCTION);
        registerImage(registry, IMG_VARIABLE);
        registerImage(registry, IMG_TCL_VARIABLE);
        registerImage(registry, IMG_SOAR);
        registerImage(registry, IMG_COMMAND_OUTLINE);
        registerImage(registry, IMG_PRODUCTION);
        registerImage(registry, IMG_PROCEDURE);
        registerImage(registry, IMG_FILE_REFERENCE);
        registerImage(registry, IMG_PACKAGE);
        registerImage(registry, IMG_PROJECT);
        registerImage(registry, IMG_AGENT);
        registerImage(registry, IMG_THIS_AGENT);
        registerImage(registry, IMG_HEADER);
        registerImage(registry, IMG_EXPAND);
        registerImage(registry, IMG_PACKAGE_OBJ);
        registerImage(registry, IMG_ATTRIBUTE);
        registerImage(registry, IMG_ALPHAB_SORT);
        registerImage(registry, IMG_SOAR_FILE);
        registerImage(registry, IMG_ERROR_OVERLAY);
        registerImage(registry, IMG_WARNING_OVERLAY);
        registerImage(registry, IMG_START_FILE_OVERLAY);
        registerImage(registry, IMG_HORIZONTAL_ORIENTATION);
        registerImage(registry, IMG_VERTICAL_ORIENTATION);
        registerImage(registry, IMG_AUTOMATIC_ORIENTATION);
        registerImage(registry, IMG_FOLDING_REGION);
        registerImage(registry, IMG_DATAMAP_FILTER);
        registerImage(registry, IMG_DATAMAP_REMOVE_FILTER);
        registerImage(registry, IMG_TAG);
        registerImage(registry, IMG_SP);
    }
    
    /**
     * Creates the image descriptors for the given image and puts the
     * filename and descriptor in the image registry.
     * 
     * @param filename the path to the icon file
     * @return the image descriptor for the given icon
     */
    private static void registerImage(ImageRegistry registry, String filename)
    {
        ImageDescriptor desc;
        try 
        {
            //create the descriptor
            desc = ImageDescriptor.createFromURL(
                    new URL(baseIconURL, filename.substring(NAME_PREFIX.length())));
            //put the descriptor and the filename into the registry
            registry.put(filename, desc);
        }
        catch (MalformedURLException e) 
        {
            desc = ImageDescriptor.getMissingImageDescriptor();
        }
    }
}
