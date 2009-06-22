/*
 *Copyright (c) 2009, Soar Technology, Inc.
 *All rights reserved.
 *
 *Redistribution and use in source and binary forms, with or without modification,  *are permitted provided that the following conditions are met:
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
 *THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY  *EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED  *WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  *IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,  *INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT   *NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR   *PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,   *WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)   *ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE   *POSSIBILITY OF SUCH *DAMAGE. 
 *
 * 
 */
package com.soartech.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

/**
 * <code>EclipseToJavaConverter</code> convert Eclipse to
 * Java types.
 * 
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 1.3 $ $Date: 2005/12/02 15:00:06 $
 */
public class EclipseToJavaConverter {


    /**
     * Convert from org.eclipse.ui.IEditorInput to java.io.Reader.
     * 
     * @param input The IEditorInput
     * @return Reader from IEditorInput
     * @throws FileNotFoundException
     */
    public static Reader convertToReader( IEditorInput input )
            throws FileNotFoundException {

        File file = convertToFile( input );
        
        if( file != null && file.exists() ) { 
            
            return new BufferedReader( new FileReader( file ) ); 
        }
        
        return null;
    }
        
    /**
     * Convert from org.eclipse.ui.IEditorInput to java.lang.String.
     * 
     * @param input The IEditorInput
     * @return String from IEditorInput
     * @throws IOException
     */
    public static String convertToString( IEditorInput input )
            throws IOException {

        Reader reader = convertToReader( input );

        if ( reader != null ) {
        
            BufferedReader buffReader = (BufferedReader) ( reader );
            StringBuffer inputStr = new StringBuffer();
            String line = null;
            while( ( line = buffReader.readLine() ) != null ) {
                inputStr.append( line + System.getProperty( "line.separator") );
            }
            return inputStr.toString();
        }
        
        return null;
    }
    
    /**
     * Convert from org.eclipse.ui.IEditorInput to java.io.File.
     * 
     * @param input The IEditorInput
     * @return File from IEditorInput
     */
    public static File convertToFile( IEditorInput input ) {
        
        if( input instanceof FileEditorInput ) {

            FileEditorInput fileInput = (FileEditorInput) input;
            IPath path = fileInput.getPath();
            return path.toFile();
        }
        
        return null;
    }
}
