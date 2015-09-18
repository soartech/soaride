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
public class TclExpansionError
{
    private String message;
    private String summary;
    private int offset;
    private int length;
    private String file;
    
    /**
     * @param file
     * @param message
     * @param summary
     * @param offset
     * @param length
     */
    public TclExpansionError(String file, String message, String summary, int offset, int length)
    {
        this.file = file;
        this.summary = summary;
        this.message = message;
        this.offset = offset;
        this.length = length;
    }
    /**
     * @param file
     * @param message
     * @param offset
     * @param length
     */
    public TclExpansionError(String file, String message, int offset, int length)
    {
        this.file = file;
        this.summary = message;
        this.message = message;
        this.offset = offset;
        this.length = length;
    }
    /**
     * @param message
     * @param offset
     * @param length
     */
    public TclExpansionError(String message, int offset, int length)
    {
        this(null, message, message, offset, length);
    }
    /**
     * @return the length
     */
    public int getLength()
    {
        return length;
    }

    /**
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }
    
    public String getSummary()
    {
        return summary;
    }
    /**
     * @return the offset
     */
    public int getOffset()
    {
        return offset;
    }
    
    /**
     * @return the file
     */
    public String getFile()
    {
        return file;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return file + ":" + message;
    }
}
