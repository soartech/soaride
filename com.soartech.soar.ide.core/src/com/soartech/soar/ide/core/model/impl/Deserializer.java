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

import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;
import com.soartech.soar.ide.core.model.impl.serialization.FileAgentProxyMemento;
import com.soartech.soar.ide.core.model.impl.serialization.ProductionMemento;
import com.soartech.soar.ide.core.model.impl.serialization.TclCommentMemento;
import com.soartech.soar.ide.core.model.impl.serialization.TclFileReferenceMemento;
import com.soartech.soar.ide.core.model.impl.serialization.TclProcedureMemento;

/**
 * @author ray
 */
public class Deserializer
{
    public static AbstractSoarElement deserialize(AbstractSoarElement parent, ElementMemento memento) throws SoarModelException
    {
        if(memento instanceof FileAgentProxyMemento)
        {
            checkType(parent, SoarFile.class);
            return new SoarFileAgentProxy((SoarFile) parent, (FileAgentProxyMemento) memento);
        }
        else if(memento instanceof ProductionMemento)
        {
            checkType(parent, SoarFileAgentProxy.class);
            return new SoarProduction((SoarFileAgentProxy) parent, (ProductionMemento) memento);
        }
        else if(memento instanceof TclProcedureMemento)
        {
            checkType(parent, SoarFileAgentProxy.class);
            return new TclProcedure((SoarFileAgentProxy) parent, (TclProcedureMemento) memento);
        }
        else if(memento instanceof TclCommentMemento)
        {
            checkType(parent, TclCommand.class);
            return new TclComment((TclCommand) parent, (TclCommentMemento) memento);
        }
        else if(memento instanceof TclFileReferenceMemento)
        {
            checkType(parent, SoarFileAgentProxy.class);
            return new TclFileReference((SoarFileAgentProxy) parent, (TclFileReferenceMemento) memento);
        }
        throw new SoarModelException("Unsupported memento object type: " + memento.getClass().getName());
    }
    
    private static void checkType(Object o, Class<?> type) throws SoarModelException
    {
        if(!type.isInstance(o))
        {
            throw new SoarModelException(o + " is not of type " + type);
        }
    }
}
