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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import com.soartech.soar.ide.core.SoarCorePlugin;

/**
 * Entry point for memento serialization and other helper methods.
 * 
 * @author ray
 */
public class Mementos
{
    /**
     * Serialize the given serializable object to the given file
     * 
     * @param e The object to serialize
     * @param file The file to write to
     */
    public static void serialize(Serializable e, File file)
    {
        ObjectOutputStream out = null;
        try
        {
            OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file));
            out = new ObjectOutputStream(fileOut);
            
            out.writeObject(e);
        }
        catch(IOException exception)
        {
        }
        finally
        {
            if(out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException e1)
                {
                    SoarCorePlugin.log(e1);
                }
            }
        }
    }
    
    /**
     * Deserialize an object from a file
     * 
     * @param file The file to deserialize from
     * @return The object or null if an error occurred
     */
    public static Object deserialize(File file)
    {
    	System.out.println("Mementos.deserialize(" + file.getPath() + ")");
        ObjectInputStream in = null;
        
        try
        {
            InputStream fileIn = new BufferedInputStream(new FileInputStream(file));
            in = new ObjectInputStream(fileIn);
            
            return (Object) in.readObject();
        }
        catch(IOException e)
        {
            return null;
        }
        catch (ClassNotFoundException e)
        {
            return null;
        }
        finally
        {
            try
            {
                if(in != null)
                {
                    in.close();
                }
            }
            catch(IOException e1)
            {
                SoarCorePlugin.log(e1);
            }
        }
    }
    
}
