package com.soartech.soar.ide.core.model.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.SpCommand;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

public class SpInternalCommand implements SoarCommand {

    private final Agent agent;
    private SoarAgent soarAgent;
    
    private SpCommand command;

    public SpInternalCommand(Agent agent, SoarAgent soarAgent)
    {
        this.agent = agent;
        this.soarAgent = soarAgent;
        
        this.command = new SpCommand(agent);
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        //save the command name
        String procLevel = agent.getInterpreter().eval("info level");
        
        String procNameAndArgs = null;
        if(Integer.parseInt(procLevel) > 0)
        {
             String procName = agent.getInterpreter().eval("lindex [info level 1] 0");
             System.out.println("*** SpInternalCommand *** " + procName);
             
             
             procNameAndArgs = agent.getInterpreter().eval("info level 1");
             
             String filename = commandContext.getSourceLocation().getFile();
             
             IWorkspace workspace= ResourcesPlugin.getWorkspace();    
             IPath location= Path.fromOSString(filename); 
             IFile ifile= workspace.getRoot().getFileForLocation(location);
             String procKey = procNameAndArgs.replace("\"", "") + "-" + ifile.getFullPath().toOSString();
//             System.out.println("SpInternalCommand Adding key: " + procKey);
             
             String expSource = "";
             if(soarAgent.getExpandedSourceMap().containsKey(procKey) && 
                procKey.equals(soarAgent.getPreviousExpandedSourceKey()))
             {
                 //concat to the existing source if we're still adding to the same source key
                 expSource = soarAgent.getExpandedSourceMap().get(procKey) + "\n"; 
             }
             
             //save the expanded source for this production
             soarAgent.getExpandedSourceMap().put(procKey, expSource + args[0] + " \"" + args[1] + "\"");
             soarAgent.setPreviousExpandedSourceKey(procKey);
             
             //save each procedure according to its file
             Map<String, List<String>> fileSourceMap = soarAgent.getFileSourceMap();
             if(!fileSourceMap.containsKey(filename))
             {
                 fileSourceMap.put(filename, new ArrayList<String>());
             }
             fileSourceMap.get(filename).add(procKey);
        }
        else
        {
            soarAgent.setPreviousExpandedSourceKey("");
        }
        
        return command.execute(commandContext, args);
    }

}
