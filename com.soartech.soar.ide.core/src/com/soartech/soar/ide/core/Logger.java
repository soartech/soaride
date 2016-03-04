package com.soartech.soar.ide.core;

import org.eclipse.core.runtime.Status;

public class Logger {
    
    public static void log(String msg) {
        log(msg, null);
     }
    
     public static void log(String msg, Exception e) {
        SoarCorePlugin.getDefault().getLog().log(new Status(Status.INFO, SoarCorePlugin.PLUGIN_ID, Status.OK, msg, e));
     }
}
