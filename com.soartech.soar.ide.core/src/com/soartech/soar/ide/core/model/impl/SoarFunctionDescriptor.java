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

import java.util.HashMap;
import java.util.Map;

import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarProblem;
import com.soartech.soar.ide.core.model.ast.FunctionCall;
import com.soartech.soar.ide.core.model.ast.Pair;

/**
 * A basic description for a Soar RHS function.
 * 
 * @author ray
 */
public class SoarFunctionDescriptor
{
    public static final Map<String, SoarFunctionDescriptor> DEFAULTS = new HashMap<String, SoarFunctionDescriptor>();
    static
    {
        addDefault("write");
        addDefault("crlf", 0, 0);
        addDefault("halt", 0, 0);
        addDefault("interrupt", 0, 0);
        addDefault("make-constant-symbol");
        addDefault("timestamp", 0, 0);
        addDefault("accept", 0, 0);
        addDefault("capitalize-symbol", 1, 1);
        addDefault("ifeq", 4, 4);
        addDefault("strlen", 1, 1);
        addDefault("dont-learn", 1, 1);
        addDefault("force-learn", 1, 1);
        addDefault("deep-copy", 1, 1);
        
        addDefault("+");
        addDefault("*");
        addDefault("-", 1);
        addDefault("/", 1);
        addDefault("div", 2, 2);
        addDefault("mod", 2, 2);
        addDefault("sin", 1, 1);
        addDefault("cos", 1, 1);
        addDefault("sqrt", 1, 1);
        addDefault("atan2", 2, 2);
        addDefault("abs", 1, 1);
        addDefault("int", 1, 1);
        addDefault("float", 1, 1);
        addDefault("min", 1);
        addDefault("max", 1);

        addDefault("tcl", 1);
        addDefault("exec", 1); // TODO: Handle exec more thoroughly
        addDefault("cmd", 1);  // TODO: Handle cmd more thoroughly
    }
    
    private String name;
    private int minArgs = 0;
    private int maxArgs = Integer.MAX_VALUE;
    
    /**
     * @param name
     * @param minArgs
     * @param maxArgs
     */
    public SoarFunctionDescriptor(String name, int minArgs, int maxArgs)
    {
        this.name = name;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    public void validate(FunctionCall call, ISoarProblemReporter reporter) throws SoarModelException
    {
        int numArgs = call.getRHSValues().size();
        String message = null;
        if(numArgs < minArgs)
        {
            message = "Too few arguments to function '" + name + "'. Expected " + getArgDescription();
        }
        else if(numArgs > maxArgs)
        {
            message = "Too many arguments to function '" + name + "'. Expected " + getArgDescription();
        }
        
        if(message != null)
        {
            Pair name = call.getFunctionName(); 
            reporter.report(SoarProblem.createError(message, name.getOffset(), name.getLength()));
        }
    }
    
    private String getArgDescription()
    {
        if(minArgs == 0 && maxArgs == 0)
        {
            return "no arguments";
        }
        else if(minArgs == maxArgs)
        {
            return "exactly " + minArgs + " argument" + (minArgs == 1 ? "" : "s");
        }
        else if(minArgs == 0 && maxArgs == Integer.MAX_VALUE)
        {
            return "any number of arguments";
        }
        else if(minArgs > 0 && maxArgs == Integer.MAX_VALUE)
        {
            return "at least " + minArgs + " argument" + (minArgs == 1 ? "" : "s");
        }
        else
        {
            return minArgs + " to " + maxArgs + " arguments";
        }
    }
    
    private static void addDefault(String name)
    {
        addDefault(name, 0, Integer.MAX_VALUE);
    }
    
    private static void addDefault(String name, int minArgs)
    {
        addDefault(name, minArgs, Integer.MAX_VALUE);
    }
    
    private static void addDefault(String name, int minArgs, int maxArgs)
    {
        SoarFunctionDescriptor d = new SoarFunctionDescriptor(name, minArgs, maxArgs);
        DEFAULTS.put(name, d);
    }
}
