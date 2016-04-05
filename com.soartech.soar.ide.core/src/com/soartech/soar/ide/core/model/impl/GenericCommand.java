package com.soartech.soar.ide.core.model.impl;

import java.util.List;
import java.util.Map;

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
    
    private String commandWithArgsExpanded = "";
    
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
                String nonexpandedCmd = "";
                try {
                    nonexpandedCmd = getSource(commandArgRange);
                    
                    String expandedCmd = soarAgent.getJsoarAgent().getInterpreter().eval("subst {" + nonexpandedCmd + "}");
                    
                    commandArgs += " " + expandedCmd;
                    
                    System.out.println("TCL SUCCESS: expanded " + nonexpandedCmd + " to " + expandedCmd + " in the global namespace");
                    
                } catch (SoarException e) {
//                    e.printStackTrace();
                    System.out.println("TCL ERROR: could not expand " + nonexpandedCmd + " in the global namespace");
                    
                    String childrenNamespaces = soarAgent.getInterpreter().getChildrenNamespaces();
                    String[] cnsArray = childrenNamespaces.split(" ");
                    for(String ns:cnsArray)
                    {
                        ns = ns.replaceAll("::", "");
                        try {
                            String expandedCmd = soarAgent.getJsoarAgent().getInterpreter().eval("namespace eval " + ns + " {subst {" + nonexpandedCmd + "}}");
                            commandArgs += " " + expandedCmd;
                            System.out.println("TCL SUCCESS: expanded " + nonexpandedCmd + " to " + expandedCmd + " in the " + ns + " namespace");
                        } catch (SoarException e1) {
//                            e1.printStackTrace();
                            System.out.println("TCL ERROR: could not expand " + nonexpandedCmd + " in the " + ns + " namespace");
                        }
                    }
                    
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
        
        String nameAndArgs = commandName + commandArgs;
        
        String key = nameAndArgs.replace("\"", "").replace("{", "").replace("}", "") + "__" + filename;
//        System.out.println("GenericCommand Adding key: " + key);
        
        String ret = soarAgent.getExpandedSourceMap().get(key);
        
        //try again to get an expanded code from the map
        //this is a workaround to jsoar sometimes giving the wrong file
        if(ret == null || ret.length() == 0)
        {
            Map<String,String> sourceMap = soarAgent.getExpandedSourceMap();
            
            for(String mapKey : sourceMap.keySet())
            {
                String tempKey = nameAndArgs.replace("\"", "").replace("{", "").replace("}", "");
                
                if(mapKey.contains(tempKey))
                {
                    ret = soarAgent.getExpandedSourceMap().get(mapKey);
                    break;
                }
            }
        }
        
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
