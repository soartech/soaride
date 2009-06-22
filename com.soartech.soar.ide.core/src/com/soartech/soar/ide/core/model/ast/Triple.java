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
package com.soartech.soar.ide.core.model.ast;

public class Triple
{
    // DataMembers
    private Pair d_variable;
    private Pair d_attribute;
    private Pair d_value;
    private boolean d_hasState = false;
    private boolean d_condition = true; // for keeping track of whether a condition or
                                // action default is condition

    // Constructors
    public Triple(Pair variable, Pair attribute, Pair value)
    {
        d_variable = variable;
        d_attribute = attribute;
        d_value = value;
    }

    public Triple(Pair variable, Pair attribute, Pair value, boolean hasState)
    {
        this(variable, attribute, value);
        d_hasState = hasState;
    }

    public Triple(Pair variable, Pair attribute, Pair value, boolean hasState,
            boolean in_condition)
    {
        this(variable, attribute, value, hasState);
        d_condition = in_condition;
    }

    // Accessors
    public boolean hasState()
    {
        return d_hasState;
    }

    public Pair getVariable()
    {
        return d_variable;
    }

    public Pair getAttribute()
    {
        return d_attribute;
    }

    public Pair getValue()
    {
        return d_value;
    }

    public boolean isCondition()
    {
        return d_condition;
    }

    public boolean isAction()
    {
        return !d_condition;
    }

    void setAsCondition()
    {
        d_condition = true;
    }

    void setAsAction()
    {
        d_condition = false;
    }

    public String toString()
    {
        return (isAction() ? "+" : "") + "(" + d_variable.getString() + ","
                + d_attribute.getString() + "," + d_value.getString() + ")";
    }
}
