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
package com.soartech.soar.ide.core.model;

/**
 * @author ray
 */
public interface ITclFileReferenceConstants
{
    
    // Standard Soar source statements
    public static final String CD = "cd";
    public static final String PUSHD = "pushd";
    public static final String SOURCE = "source";
    public static final String POPD = "popd";
    
    public static String[] STANDARD_COMMANDS = { CD, PUSHD, SOURCE, POPD }; 
    
    // NGS file management
    public static final String NGS_LOAD_DIR = "NGS_load-soar-dir";
    public static final String NGS_LOAD_SETTINGS = "NGS_load-settings";
    public static final String NGS_SOURCE = "NGS_echo-source";
    public static final String NGS_PUSHD = "NGS_echo-pushd";
    
    public static String[] NGS_COMMANDS = { NGS_LOAD_DIR, NGS_LOAD_SETTINGS, NGS_SOURCE, NGS_PUSHD }; 
    
    // xxx-load-procs.tcl file management (Mike's stuff from Helo-Soar, etc)
    public static final String LP_SOURCE_FILE = "source_file";
    public static final String LP_DONT_SOURCE_FILE = "dont_source_file";
    public static final String LP_SOURCE_DIR = "source_soardir";

    public static String[] LP_COMMANDS = { LP_SOURCE_DIR, LP_DONT_SOURCE_FILE, LP_SOURCE_FILE };
}
