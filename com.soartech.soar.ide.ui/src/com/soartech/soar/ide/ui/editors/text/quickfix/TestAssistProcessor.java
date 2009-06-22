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
package com.soartech.soar.ide.ui.editors.text.quickfix;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.soartech.soar.ide.ui.SoarEditorPluginImages;

public class TestAssistProcessor implements IQuickAssistProcessor
{
	public boolean hasAssists(IInvocationContext context)
	{
		return true;
	}
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
	{
		IJavaCompletionProposal[] assists = {new TextCompletionProposal(context,locations)}; 
		return assists;
	}
	private class TextCompletionProposal implements IJavaCompletionProposal {
		private IInvocationContext context;
		private IProblemLocation[] locations;
		public TextCompletionProposal(IInvocationContext context, IProblemLocation[] locations)
		{
			this.context=context;
			this.locations=locations;
		}
		public void apply(IDocument document)
		{
			
		}

		public Point getSelection(IDocument document)
		{
			return null;
		}

		public String getAdditionalProposalInfo()
		{
			return "additional information";
		}

		public String getDisplayString()
		{
			return "TEST NOTHING";
		}

		public Image getImage()
		{
		      return SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_FILE_REFERENCE);
		}

		public IContextInformation getContextInformation()
		{
			return null;
		}
		
		public int getRelevance()
		{
			return 100;
		}
	}
}