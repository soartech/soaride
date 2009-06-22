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
package com.soartech.soar.ide.core.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarOpenable;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;

/**
 * Very basic implementation of ISoarElement interface. Implements parent/child
 * handling as well as some simple methods.
 * 
 * @author ray
 */
public abstract class AbstractSoarElement extends PlatformObject implements ISoarElement
{
    private static final List<ISoarElement> NO_CHILDREN = Collections.emptyList();
    private AbstractSoarElement parent;
    private List<ISoarElement> children;
    private boolean detached = false;
    
    // TODO: Think about locking more. See getLock() below
    //private Object lock = new Object();
    
    public AbstractSoarElement(AbstractSoarElement parent)
    {
        this.parent = parent;
    }
    
    public AbstractSoarElement(AbstractSoarElement parent, ElementMemento memento) throws SoarModelException
    {
        this.parent = parent;
       
        if(memento.getChildren().length != 0)
        {
            List<AbstractSoarElement> tempKids = new ArrayList<AbstractSoarElement>();
            for(ElementMemento m : memento.getChildren())
            {
                tempKids.add(Deserializer.deserialize(this, m));
            }
            addChildren(tempKids);
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getLock()
     */
    public Object getLock()
    {
        // This is fairly draconian. Essentially there is one lock for the
        // entire system. It's also safer.
        // TODO: Think about locking more.
        return SoarCorePlugin.getDefault().getSoarModel();
    }
    
    /**
     * Called when this element is removed from its parent. This should be 
     * overridden by subclasses as necessary. The detachment process is depth
     * first so sub-classes can assume that all parent pointers are valid
     * until they call super.detach(). Thus, super.detach() should be called
     * at the end.
     */
    protected void detach()
    {
        synchronized (getLock())
        {
            this.detached = true;
            // If there are children, detach them first.
            if(children != null)
            {
                for(ISoarElement e : children)
                {
                    ((AbstractSoarElement) e).detach();
                }
            }
            
            this.children = null;
        }
    }
    
    /**
     * Overridable by sub-classes to selectively enable/disable event firing.
     * 
     * @return True if events should be fired when this element changes
     */
    protected boolean shouldFireEvents()
    {
        return true;
    }
    
    /**
     * Add the given child as a child of this element.  The child's parent 
     * should already be set to this. Fire appropriate SoarModelEvent.
     * 
     * @param element The child to add.
     * @throws SoarModelException
     */
    protected void addChild(AbstractSoarElement element) throws SoarModelException
    {
        synchronized(getLock())
        {
            openWhenClosed(null);
            assert element.parent == this;
            if(children == null)
            {
                children = new ArrayList<ISoarElement>();
            }
            assert !children.contains(element);
            children.add(element);
            fireEvent(SoarModelEvent.createAdded(element));
        }
    }
    
    protected void addChildren(List<AbstractSoarElement> elements) throws SoarModelException
    {
        synchronized (getLock())
        {
            openWhenClosed(null);
            if(children == null)
            {
                children = new ArrayList<ISoarElement>();
            }
            children.addAll(elements);
            ISoarElement[] elementArray = elements.toArray(new ISoarElement[elements.size()]);
            fireEvent(SoarModelEvent.createAdded(elementArray));
        }
    }
    
    /**
     * Remove and detach the given child from this element. Fires appropriate 
     * SoarModelEvent
     * 
     * @param element The removed child
     * @throws SoarModelException
     */
    protected void removeChild(AbstractSoarElement element) throws SoarModelException
    {
        synchronized (getLock())
        {
            openWhenClosed(null);
            assert element.parent == this;
            assert children.contains(element);
            if(children == null)
            {
                return;
            }
            element.detach();
            children.remove(element);
            fireEvent(SoarModelEvent.createRemoved(element));
        }
    }
    
    /**
     * Remove and detach the given children. Fires a consolidated remove event.
     * 
     * @param elements The elements to remove
     * @throws SoarModelException
     */
    protected void removeChildren(List<AbstractSoarElement> elements) throws SoarModelException
    {
        synchronized (getLock())
        {
            openWhenClosed(null);
            if(children != null)
            {
                children.removeAll(elements);
                for(ISoarElement child : elements)
                {
                    ((AbstractSoarElement) child).detach();
                }
                ISoarElement[] removed = elements.toArray(new ISoarElement[elements.size()]);
                fireEvent(SoarModelEvent.createRemoved(removed));
            }
        }
    }
    
    /**
     * Replaces the current set of children with a new set of children without
     * detaching the current children or firing events
     * 
     * @param elements New children
     * @throws SoarModelException
     */
    protected void replaceChildrenFast(List<AbstractSoarElement> elements) throws SoarModelException
    {
        synchronized (getLock())
        {
            openWhenClosed(null);
            if(children != null)
            {
                children.clear();
                children.addAll(elements);
            }
            else
            {
                children = new ArrayList<ISoarElement>(elements);
            }
        }
    }
    
    /**
     * Remove and detach all children of this element. Fires a consolidated remove
     * event.
     * 
     * @throws SoarModelException
     */
    protected void clearChildren() throws SoarModelException
    {
        synchronized (getLock())
        {
            openWhenClosed(null);
            if(children != null)
            {
                for(ISoarElement child : children)
                {
                    ((AbstractSoarElement) child).detach();
                }
                ISoarElement[] removed = children.toArray(new ISoarElement[children.size()]);
                children.clear();
                fireEvent(SoarModelEvent.createRemoved(removed));
            }
        }
    }
    
    protected void openWhenClosed(IProgressMonitor monitor) throws SoarModelException
    {
        synchronized (getLock())
        {
            ISoarOpenable openable = getOpenable();
            if(!openable.isOpen())
            {
                openable.open(monitor);
            }
        }
    }
    
    /**
     * @return The SoarModel implementation that owns this element. 
     */
    protected SoarModel getInternalSoarModel()
    {
        ISoarElement current = this;
        
        do
        {
            if(current instanceof SoarModel)
            {
                return (SoarModel) current;
            }
            current = current.getParent();
        } while(current != null);
        
        return null;
        
    }
    
    /**
     * Fire the given event if event firing is enabled
     * 
     * @param e The event to fire
     */
    protected void fireEvent(SoarModelEvent e)
    {
        if(shouldFireEvents())
        {
            getInternalSoarModel().fireEvent(e);
        }
    }
    
    /**
     * Create a memento of this element.  This method should be overridden by
     * sub-classes to create the correct memento type, making sure to call
     * saveState on the super class.
     * 
     * @return A new memento representing the current state of this object.
     */
    public ElementMemento createMemento()
    {
        return saveState(new ElementMemento());
    }
    
    /**
     * Saves this elements state into the given memento which must be cast as
     * necessary for subclasses.  Sub-classes <b>must</b> call the super-class
     * implementation.
     * 
     * @param memento Object to store state in.
     * @return The memento passed in
     */
    protected ElementMemento saveState(ElementMemento memento)
    {
        synchronized (getLock())
        {
            if(children != null)
            {
                ElementMemento[] kidMementos = new ElementMemento[children.size()];
                int i = 0;
                for(ISoarElement kid : children)
                {
                    kidMementos[i++] = ((AbstractSoarElement) kid).createMemento();
                }
                
                memento.setChildren(kidMementos);
            }
        }
        return memento;
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getSoarProject()
     */
    public ISoarProject getSoarProject()
    {
        ISoarElement current = this;
        do
        {
            if(current instanceof ISoarProject)
            {
                return (ISoarProject) current;
            }
            current = current.getParent();
        } while(current != null);
        
        return null;
    }
    
	/* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#isDetached()
     */
    public boolean isDetached()
    {
        synchronized (getLock())
        {
            return detached;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getChildren()
     */
    public List<ISoarElement> getChildren() throws SoarModelException
    {
        synchronized (getLock())
        {
            openWhenClosed(null);
            return children != null ? children : NO_CHILDREN;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getParent()
     */
    public ISoarElement getParent()
    {
        return parent;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getOpenable()
     */
    public ISoarOpenable getOpenable()
    {
        if(this instanceof ISoarOpenable)
        {
            return (ISoarOpenable) this;
        }
        return parent != null ? parent.getOpenable() : null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getSoarModel()
     */
    public ISoarModel getSoarModel()
    {
        return getInternalSoarModel();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#hasChildren()
     */
    public boolean hasChildren()
    {
        synchronized (getLock())
        {
            return children != null ? !children.isEmpty() : false;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#hasErrors()
     */
    public boolean hasErrors()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#hasWarnings()
     */
    public boolean hasWarnings()
    {
        return false;
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        // If requesting a resource, return containing resource
        if(adapter.equals(IResource.class))
        {
            IResource resource = getCorrespondingResource();
            if(resource != null)
            {
                return resource;
            }
        }
        return super.getAdapter(adapter);
    }

}
