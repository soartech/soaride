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
package com.soartech.soar.ide.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent;
import com.soartech.soar.ide.core.sql.SoarDatabaseUtil;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;

import edu.umich.soar.debugger.jmx.SoarCommandLineMXBean;

/**
 * <code>SoarCorePlugin</code> The main plugin class to be used in the desktop.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision$ $Date$
 */
public class SoarCorePlugin extends Plugin {

    /**
     * Id of this plugin
     */
    public static final String PLUGIN_ID = "com.soartech.soar.ide";

    public static final String PLUGIN_PATH = "com.soartech.soar.ide.core";

    public static final String MARKER_TYPE_PREFIX = PLUGIN_ID;

    /**
     * Id for general problem markers created by the Soar plugin.
     */
    public static final String PROBLEM_MARKER_ID = MARKER_TYPE_PREFIX + ".problemmarker";

    /**
     * Id for problem markers created during the Tcl pre-processing phase.
     * This is a separate type to make it easier to manage the markers since
     * they are added in a separate phase from the others.
     */
    public static final String TCL_PREPROCESSOR_PROBLEM_MARKER_ID = MARKER_TYPE_PREFIX + ".tclpreprocessorproblemmarker";

    public static final String TASK_MARKER_ID = MARKER_TYPE_PREFIX + ".taskmarker";


	//The shared instance.
	private static SoarCorePlugin plugin;

    //private SoarModelAdapterFactory modelAdapters;
    //private SoarModel soarModel;
	
	private SoarDatabaseConnection databaseConnection;
	private SoarCommandLineMXBean proxy;

	/**
	 * The constructor.
	 */
	public SoarCorePlugin()
    {
		plugin = this;
		
		// These have to be initialized AFTER the plugin is set.
		//modelAdapters = new SoarModelAdapterFactory();
	    //soarModel = new SoarModel();
		databaseConnection = new SoarDatabaseConnection();
		}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception
    {
		super.start(context);

        //modelAdapters.register();

        //getSoarModel().open(new NullProgressMonitor());
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception
    {
		super.stop(context);
        //modelAdapters.unregister();
		plugin = null;
	}


	/* (non-Javadoc)
     * @see org.eclipse.core.runtime.Plugin#initializeDefaultPluginPreferences()
     */
/*
	@Override
    protected void initializeDefaultPluginPreferences()
    {
        // getPluginPreferences().setDefault(ISoarCorePluginConstants.SOURCE_CHANGES_DIRECTORY, false);
    }
    */

    /**
	 * Returns the shared instance.
	 */
	public static SoarCorePlugin getDefault()
    {
        assert plugin != null;
		return plugin;
	}

    /**
     * Get the handle to the ISoarModel.
     *
     * @return The model.
     */
	/*
    public ISoarModel getSoarModel()
    {
        return soarModel;
    }
    */
    
    public SoarDatabaseConnection getDatabaseConnection() {
    	return databaseConnection;
    }

    /**
     * Get the handle to the SoarModel. This method should only be called by
     * Soar model code!
     *
     * @return The model.
     */
    /*
    public SoarModel getInternalSoarModel()
    {
        return soarModel;
    }
    */

    public static void log( IStatus status )
    {
        getDefault().getLog().log( status );
    }

    public static void log(Exception e)
    {
        getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, 0, e.getMessage(), e));
    }

	public void saveDatabaseAs(String path) {
		String dump = SoarDatabaseUtil.sqlDump(databaseConnection);
		System.out.println("**************************DUMP");
		System.out.println(dump);
		
		String[] commands = dump.split(";");
		
		boolean eventsSupressed = databaseConnection.getSupressEvents();
		databaseConnection.setSupressEvents(true);
		databaseConnection.loadDatabaseConnection(path);
		for (String command : commands) {
			command = command.trim();
			if (command.length() > 0) {
				databaseConnection.execute(command);
			}
		}
		databaseConnection.setSupressEvents(eventsSupressed);
		databaseConnection.fireEvent(new SoarDatabaseEvent(Type.DATABASE_PATH_CHANGED));
	}

	/**
	 * Replaces the current database connection with one that reads
	 * from the file at the specified path, and fires a
	 * <code>SoarDatabaseEvent</code> of type
	 * <code>DATABASE_PATH_CHANGED</code>.
	 * @param path
	 */
	public void openProject(String path) {
		databaseConnection.loadDatabaseConnection(path);
		databaseConnection.fireEvent(new SoarDatabaseEvent(Type.DATABASE_PATH_CHANGED));
	}
	
	/**
	 * Replaces the current database connection with one that reads
	 * from the file at the specified path, and fires a
	 * <code>SoarDatabaseEvent</code> of type
	 * <code>DATABASE_PATH_CHANGED</code>.
	 */
	public void newProject() {
		openProject(":memory:"); 
	}

	public void setCommandLineProxy(SoarCommandLineMXBean proxy) {
		this.proxy = proxy;
	}

	public SoarCommandLineMXBean getCommandLineProxy() {
		return proxy;
	}

}
