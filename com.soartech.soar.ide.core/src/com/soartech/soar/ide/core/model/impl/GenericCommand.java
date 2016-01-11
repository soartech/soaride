package com.soartech.soar.ide.core.model.impl;

import java.util.List;

import org.eclipse.core.resources.IResource;

import com.soartech.soar.ide.core.model.BasicSoarSourceRange;
import com.soartech.soar.ide.core.model.IExpandableElement;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarModelConstants;
import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.impl.serialization.TclCommandMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;
import com.soartech.soar.ide.core.tcl.TclAstNodeSourceRange;

import javafx.scene.Parent;

public class GenericCommand extends AbstractSourceReferenceElement implements IExpandableElement
{
    private SoarFileAgentProxy soarFile;
    private TclAstNode astNode;
    private String commandName;
    private ISoarSourceRange commandNameRange;
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
        String ret = soarAgent.getExpandedSourceMap().get(commandName);
        
        return ret;
    }
    
}
