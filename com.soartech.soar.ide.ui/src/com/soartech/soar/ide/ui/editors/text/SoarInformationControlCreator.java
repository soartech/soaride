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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.soartech.soar.editor.docs.SoarDocs;
import com.soartech.soar.ide.core.SoarCorePlugin;

/**
 * <code>SoarInformationControlCreator</code> displays additional information
 * on the selected item displayed by <code>SoarContentAssistant</code>.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2009-06-22 13:05:30 -0400 (Mon, 22 Jun 2009) $
 */
public class SoarInformationControlCreator
    implements IInformationControlCreator {

    private static final DefaultInformationControl.IInformationPresenter
    presenter = new DefaultInformationControl.IInformationPresenter() {

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.DefaultInformationControl$IInformationPresenter#updatePresentation(org.eclipse.swt.widgets.Display, java.lang.String, org.eclipse.jface.text.TextPresentation, int, int)
         */
        public String updatePresentation(Display display, String infoText,
          TextPresentation presentation, int maxWidth, int maxHeight) {

            SoarDocs docs = SoarDocs.getInstance();

            int len = infoText.length();
            StyleRange range = new StyleRange( 0, len, null, null,
                                               SWT.BOLD );
            presentation.addStyleRange( range );
            if ( infoText.startsWith( "<" ) ) {
                String var = "variable";
                range = new StyleRange( len+3, var.length(), null, null,
                                        SWT.ITALIC );
                presentation.addStyleRange( range );
                return infoText + " : " + var;
            } else {
                return infoText + " :\n" +
                       docs.getInfo( infoText, 160, maxHeight );
            }
       }
    };

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.IInformationControlCreator#createInformationControl(org.eclipse.swt.widgets.Shell)
     */
    public IInformationControl createInformationControl( Shell parent ) {

        initSoarDocs();
        return new DefaultInformationControl(parent, presenter);
    }

    private void initSoarDocs() {

        try {
            SoarDocs docs = SoarDocs.getInstance();
            URL baseURL = new URL( Platform.getBundle( SoarCorePlugin.PLUGIN_ID ).getEntry("/"),
                               "conf/" );
            URL url = new URL( baseURL, "keywords.info" );
            docs.loadFromURL( url );
        }
        catch( MalformedURLException e ) {
            e.printStackTrace();
        }

    }
}
