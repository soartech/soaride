package com.soartech.soar.ide.core.model.impl;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jsoar.kernel.SoarException;

import com.soartech.soar.ide.core.model.BasicSoarSourceRange;
import com.soartech.soar.ide.core.model.IExpandableElement;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ITclCommand;
import com.soartech.soar.ide.core.model.ITclComment;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.impl.serialization.TclCommandMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;
import com.soartech.soar.ide.core.tcl.TclAstNodeSourceRange;


public class GenericCommand extends AbstractSourceReferenceElement implements IExpandableElement, ITclCommand
{
    private SoarFileAgentProxy soarFile;
    private TclAstNode astNode;
    private String commandName;
    private ISoarSourceRange commandNameRange;
    
    private String commandArgs = "";
    private ISoarSourceRange commandArgRange;
    
    private TclComment comment;
    
    public GenericCommand(SoarFileAgentProxy parent, ISoarProblemReporter reporter, TclAstNode astNode) throws SoarModelException 
    {
        super(parent);
        
        this.soarFile = parent;
        this.astNode = astNode;
        
        int startOffset = astNode.getStart();
        
        int length = (astNode.getStart() + astNode.getLength()) - startOffset;
        setSourceRange(new BasicSoarSourceRange(startOffset, length));
        
        List<TclAstNode> words = astNode.getWordChildren();
        if(!words.isEmpty())
        {
            commandNameRange = new TclAstNodeSourceRange(words.get(0));
            commandName = getSource(commandNameRange);
            
            for(int i = 1; i < words.size(); i++)
            {
                commandArgRange = new TclAstNodeSourceRange(words.get(i));
                
                SoarAgent soarAgent = (SoarAgent) soarFile.getAgent();
                try {
                    String nonexpandedCmd = getSource(commandArgRange);
                    
                    String expandedCmd = soarAgent.getJsoarAgent().getInterpreter().eval("subst {" + nonexpandedCmd + "}");
                    
                    commandArgs += " " + expandedCmd;
                    
                } catch (SoarException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
        }
    }
    
    public GenericCommand(SoarFileAgentProxy parent, TclCommandMemento memento) throws SoarModelException
    {
        super(parent, memento);
        this.soarFile = parent;
        this.astNode = null;
        this.commandName = memento.getCommandName();
        this.commandNameRange = new BasicSoarSourceRange(memento.getCommandNameRange());
        
        for(ISoarElement kid : getChildren())
        {
            if(kid instanceof TclComment)
            {
                this.comment = (TclComment) kid;
            }
        }
    }

    @Override
    public IResource getCorrespondingResource() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getExpandedSource() throws SoarModelException 
    {
        SoarAgent soarAgent = (SoarAgent) soarFile.getAgent();
        
        String filename = soarFile.getFile().getPath().toOSString();
        
        //get object which represents the workspace  
        IWorkspace workspace = ResourcesPlugin.getWorkspace();  
        
        String key = commandName + commandArgs + "-" + filename;
//        System.out.println("GenericCommand Adding key: " + key);
        
        String ret = soarAgent.getExpandedSourceMap().get(key);
        
        return ret;
    }
    
    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public ISoarSourceRange getCommandNameRange() {
        return null;
    }

    @Override
    public ITclComment getAssociatedComment() {
        return null;
    }

    @Override
    public TclAstNode getTclSyntaxTree() {
        return null;
    }
    
}
