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
package com.soartech.soar.ide.core.model.impl.serialization;

import java.io.Serializable;

/**
 * Serializable memento for a SoarDatamapAttribute instance
 * 
 * @author ray
 */
@SuppressWarnings("serial")
public class DatamapAttributeMemento implements Serializable
{
    public static final DatamapAttributeMemento[] EMPTY_ATTRIBUTE_ARRAY = {};
    
    private String name;
    private boolean persistent;
    private DatamapProductionReference[] productions;
    private DatamapNodeMemento source;
    private DatamapNodeMemento target;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
    
    public DatamapProductionReference[] getProductions() { return productions != null ? productions : DatamapProductionReference.EMPTY_PRODUCTION_ARRAY; }
    public void setProductions(DatamapProductionReference[] productions) { this.productions = productions; }
    
    public DatamapNodeMemento getSource() { return source; }
    public void setSource(DatamapNodeMemento source) { this.source = source; }
    
    public DatamapNodeMemento getTarget() { return target; }
    public void setTarget(DatamapNodeMemento target) { this.target = target; }
}
