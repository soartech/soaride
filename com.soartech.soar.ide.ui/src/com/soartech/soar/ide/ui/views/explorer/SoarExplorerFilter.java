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
package com.soartech.soar.ide.ui.views.explorer;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ITclProcedure;

/**
 * Implementation of the filter for the soar package explorer view.
 * 
 * @author aron
 *
 */
public class SoarExplorerFilter extends ViewerFilter 
{
	/**
	 * The current filter string.
	 */
	private String filterString = "";
	
	private boolean showProductions = true;
	
	private boolean showProcedures = true;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) 
	{
		if(element instanceof ISoarProduction)
		{
			if(!showProductions)
			{
				return false;
			}
			
			ISoarProduction production = (ISoarProduction) element;
			String name = production.getProductionName();
			
			if(filterString.length() > 0 && !name.contains(filterString))
			{
				return false;
			}
		}
		else if(element instanceof ITclProcedure)
		{
			if(!showProcedures)
			{
				return false;
			}
			
			ITclProcedure procedure = (ITclProcedure) element;
			String name = procedure.getProcedureName();
			
			if(filterString.length() > 0 && !name.contains(filterString))
			{
				return false;
			}
		}
		
		return true;
	}

	/**
	 * @return the filterString
	 */
	public String getFilterString() 
	{
		return filterString;
	}

	/**
	 * @param filterString the filterString to set
	 */
	public void setFilterString(String filterString) 
	{
		this.filterString = filterString;
	}

	/**
	 * Toggle show procedures.
	 */
	public void showProcedures(boolean show) 
	{
		showProcedures = show;
//		if(showProcedures)
//		{
//			showProcedures = false;
//		}
//		else
//		{
//			showProcedures = true;
//		}
	}

	/**
	 * Toggle show productions.
	 */
	public void showProductions(boolean show) 
	{
		showProductions = show;
		
//		if(showProductions)
//		{
//			showProductions = false;
//		}
//		else
//		{
//			showProductions = true;
//		}
	}
	
	

}
