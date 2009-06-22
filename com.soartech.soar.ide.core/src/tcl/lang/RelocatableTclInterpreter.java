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
package tcl.lang;

import java.io.File;

/**
 * Extended interpreter that converts working directory methods from package
 * private to public. That's why it's in the tcl.lang package.
 * 
 * @author ray
 */
public class RelocatableTclInterpreter extends Interp
{
    public interface ProgressHandler
    {
        void onProgress(RelocatableTclInterpreter interp);
    }
    
    public static class InterruptedException extends RuntimeException
    {
        private static final long serialVersionUID = 8406042835724419511L;

        public InterruptedException()
        {
            super("Tcl processing interrupted");
        }
        
    }
    
    private ProgressHandler progressHandler;
    
    /**
     * 
     */
    public RelocatableTclInterpreter(ProgressHandler handler)
    {
        this.progressHandler = handler;
    }
    
    /* (non-Javadoc)
     * @see tcl.lang.Interp#getWorkingDir()
     */
    @Override
    public File getWorkingDir()
    {
        return super.getWorkingDir();
    }

    /* (non-Javadoc)
     * @see tcl.lang.Interp#setWorkingDir(java.lang.String)
     */
    @Override
    public void setWorkingDir(String arg0) throws TclException
    {
        super.setWorkingDir(arg0);
    }

    /* (non-Javadoc)
     * @see tcl.lang.Interp#getCommand(java.lang.String)
     */
    @Override
    public Command getCommand(String arg0)
    {
        // In Jacl 1.3 this is the easiest way I found to handle progress while
        // Tcl is executing. This method is called every time a Tcl command is
        // executed so it's a good place to hook in and handle cancellation of
        // infinite loops. In Jacl 1.4 (which as of 4/20/2007 hasn't been 
        // called stable yet), the ready() method of the interpreter may be
        // a better choice to override.
        if(progressHandler != null)
        {
            progressHandler.onProgress(this);
        }
        return super.getCommand(arg0);
    }

}
