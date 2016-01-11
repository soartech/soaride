package com.soartech.soar.ide.core.model.impl;


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
        
        String procName = null;
        if(Integer.parseInt(procLevel) > 0)
        {
             procName = agent.getInterpreter().eval("lindex [info level 1] 0");
             
             String expSource = "";
             if(soarAgent.getExpandedSourceMap().containsKey(procName))
             {
                 expSource = soarAgent.getExpandedSourceMap().get(procName) + "\n"; 
             }
             
             soarAgent.getExpandedSourceMap().put(procName, expSource + args[0] + " \"" + args[1] + "\"");
        }
        
        return command.execute(commandContext, args);
    }

}
