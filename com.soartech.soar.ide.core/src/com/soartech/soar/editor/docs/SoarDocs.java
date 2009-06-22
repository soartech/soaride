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
package com.soartech.soar.editor.docs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * <code>SoarDocs</code> stores the text indexed by keyword that will be 
 * displayed by content assist .
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision$ $Date$
 */
public class SoarDocs extends Properties {
    
    /**
     * Required serial version id
     */
    private static final long serialVersionUID = -8489589935151800750L;
    
    private static SoarDocs instance = null;

    /**
     * Constructor for a <code>SoarDocs</code> object.
     */
    private SoarDocs() {

        super();        
    }
    
    public static SoarDocs getInstance() {
        
        if ( instance == null )
            instance = new SoarDocs();
        return instance;
    }
    
    /**
     * Build the <code>SoarDocs</code> by parsing properties 
     * from the given text.
     * 
     * @param text The text from which to load the properties
     */
    public void loadFromString( String text ) {
        
        try {
            load( new ByteArrayInputStream( text.getBytes() ) );
        } catch ( IOException e ) {
            e.printStackTrace();
        }        
    }
    
    /**
     * Build the <code>SoarDocs</code> by parsing properties 
     * from the given URL.
     * 
     * @param url The url to the file from which to load the properties
     */    
    public void loadFromURL( URL url ) {
        
        try {
            load( new BufferedInputStream( url.openStream() ) );
        } catch ( IOException e ) {
            e.printStackTrace();
        }       
    } 

    /**
     * @param keyword A command or function keyword.
     * @param maxWidth The display width ( wrap if text is too wide )
     * @param maxHeight The display height ( truncate if text is too long )
     * @return The information to be displayed in the content assist for the
     * given keyword.
     */
    public String getInfo( String keyword, int maxWidth, int maxHeight ) {

        boolean truncated = false;
        
        String info = getProperty( keyword );
        
        if ( info == null ) return "N/A";
        
        if ( info.length() > (maxWidth * maxHeight) ) {
            truncated = true;
            // truncate and leave room for ellipse
            info = info.substring( 0, maxWidth * (maxHeight-1) ); 
        }
        
        int infoLength = info.length();
        int numRows = infoLength / maxWidth;
        int remainder = infoLength % maxWidth;
        if ( remainder > 0 ) numRows++;

        StringBuffer infoBuffer = new StringBuffer();
        int startIdx = 0;
        int endIdx = 0;
        
        // TODO this logic isn't very intelligent right now and
        // does not wrap on word boundaries.
        for ( int i = 0; i < numRows; i++ ) {
            
            int infoBufferLen = infoBuffer.length();
            int paddedLength = infoLength + i; // account for newlines
            if ( paddedLength - infoBufferLen < maxWidth ) {
                endIdx = infoLength;
            } else {
                endIdx = endIdx + maxWidth;
            }
            infoBuffer.append( info.substring(startIdx, endIdx) );
            if  ( i < numRows - 1 ) infoBuffer.append( "\n" );
            startIdx = endIdx;
        }
        
        if ( truncated ) infoBuffer.append( "\n..." );
        
        return infoBuffer.toString();
    }   
}
