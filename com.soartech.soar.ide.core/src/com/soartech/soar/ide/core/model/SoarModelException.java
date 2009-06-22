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

import java.io.PrintStream;
import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.soartech.soar.ide.core.SoarCorePlugin;

/**
 * @author ray
 */
public class SoarModelException extends CoreException
{
    private static final long serialVersionUID = -5564073594102509415L;
    
    private CoreException nestedCoreException;
    
    public SoarModelException(CoreException exception)
    {
        super(exception.getStatus());
        this.nestedCoreException = exception;
    }
    
    public SoarModelException(Exception e)
    {
        super(new Status( IStatus.ERROR, SoarCorePlugin.PLUGIN_ID, 0, 
                          e.getMessage() != null ? e.getMessage() : "No exception message", 
                          e ));
    }
    
    public SoarModelException(String message)
    {
        super(new Status( IStatus.ERROR, SoarCorePlugin.PLUGIN_ID, 0, 
                          message, 
                          null ));
    }
    
    /**
     * Returns the underlying <code>Throwable</code> that caused the failure.
     *
     * @return the wrappered <code>Throwable</code>, or <code>null</code> if the
     *   direct case of the failure was at the Java model layer
     */
    public Throwable getException() {
        if (this.nestedCoreException == null) {
            return getStatus().getException();
        } else {
            return this.nestedCoreException;
        }
    }
    /**
     * Prints this exception's stack trace to the given print stream.
     * 
     * @param output the print stream
     */
    public void printStackTrace(PrintStream output) {
        synchronized(output) {
            super.printStackTrace(output);
            Throwable throwable = getException();
            if (throwable != null) {
                output.print("Caused by: "); //$NON-NLS-1$
                throwable.printStackTrace(output);
            }
        }
    }

    /**
     * Prints this exception's stack trace to the given print writer.
     * 
     * @param output the print writer
     */
    public void printStackTrace(PrintWriter output) {
        synchronized(output) {
            super.printStackTrace(output);
            Throwable throwable = getException();
            if (throwable != null) {
                output.print("Caused by: "); //$NON-NLS-1$
                throwable.printStackTrace(output);
            }
        }
    }
    /*
     * Returns a printable representation of this exception suitable for debugging
     * purposes only.
     */
    public String toString() {
        StringBuffer buffer= new StringBuffer();
        buffer.append("Java Model Exception: "); //$NON-NLS-1$
        if (getException() != null) {
            if (getException() instanceof CoreException) {
                CoreException c= (CoreException)getException();
                buffer.append("Core Exception [code "); //$NON-NLS-1$
                buffer.append(c.getStatus().getCode());
                buffer.append("] "); //$NON-NLS-1$
                buffer.append(c.getStatus().getMessage());
            } else {
                buffer.append(getException().toString());
            }
        } else {
            buffer.append(getStatus().toString());
        }
        return buffer.toString();
    }
}
