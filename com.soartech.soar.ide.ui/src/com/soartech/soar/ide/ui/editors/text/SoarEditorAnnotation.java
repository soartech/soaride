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
package com.soartech.soar.ide.ui.editors.text;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

/**
 * @author ray
 */
public class SoarEditorAnnotation extends Annotation
{
    /*
     * These constants must match the strings in the 
     * org.eclipse.ui.editors.annotationTypes extension points defined in
     * plugin.xml!
     */
    public static final String ERROR = "com.soartech.soar.ide.ui.error";
    public static final String WARNING = "com.soartech.soar.ide.ui.warning";
    public static final String INFO = "com.soartech.soar.ide.ui.warning";
    
    public static void addError(IAnnotationModel model, String message, Position position)
    {
        SoarEditorAnnotation a = new SoarEditorAnnotation(ERROR, message);
        model.addAnnotation(a, position);
    }
    
    public static void addWarning(IAnnotationModel model, String message, Position position)
    {
        SoarEditorAnnotation a = new SoarEditorAnnotation(WARNING, message);
        model.addAnnotation(a, position);
    }
    
    /**
     * @param type
     * @param isPersistent
     * @param text
     */
    public SoarEditorAnnotation(String type, String text)
    {
        super(type, false, text);
    }
}
