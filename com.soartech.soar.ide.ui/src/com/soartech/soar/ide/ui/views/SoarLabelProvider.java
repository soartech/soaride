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
package com.soartech.soar.ide.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.ITclCommand;
import com.soartech.soar.ide.core.model.ITclFileReference;
import com.soartech.soar.ide.core.model.ITclProcedure;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapValue;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseJoinFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRowFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarEditorPluginImages;
import com.soartech.soar.ide.ui.editors.datamap.SoarDatabaseDatamapSuperstateAttribute;
import com.soartech.soar.ide.ui.editors.text.SoarFoldingRegion;
import com.soartech.soar.ide.ui.views.explorer.SoarExplorerFullViewContentProvider.SoarFolderHeader;

/**
 * The label provider for all the views in the plugin.
 * 
 * @author aron
 * 
 */
public class SoarLabelProvider extends LabelProvider implements
		ITableLabelProvider {
	private ISoarAgent thisAgent;

	public static ILabelProvider createFullLabelProvider(ISoarAgent thisAgent) {
		return new DecoratingLabelProvider(new SoarLabelProvider(thisAgent),
				PlatformUI.getWorkbench().getDecoratorManager()
						.getLabelDecorator());
	}

	public static ILabelProvider createFastLabelProvider(ISoarAgent thisAgent) {
		return new SoarLabelProvider(thisAgent);
	}

	/**
	 * @param thisAgent
	 */
	public SoarLabelProvider(ISoarAgent thisAgent) {
		this.thisAgent = thisAgent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof ISoarProject) {
			return SoarEditorPluginImages
					.get(SoarEditorPluginImages.IMG_PROJECT);
		} else if (element instanceof ISoarFile) {
			return SoarEditorPluginImages
					.get(SoarEditorPluginImages.IMG_SOAR_FILE);
		} else if (element instanceof SoarFolderHeader) {
			return SoarEditorPluginImages
					.get(SoarEditorPluginImages.IMG_PACKAGE_OBJ);
		} else if (element instanceof ISoarProduction) {
			return SoarEditorPluginImages
					.get(SoarEditorPluginImages.IMG_PRODUCTION);
		} else if (element instanceof ITclProcedure) {
			return SoarEditorPluginImages
					.get(SoarEditorPluginImages.IMG_PROCEDURE);
		} else if (element instanceof ITclFileReference) {
			return SoarEditorPluginImages
					.get(SoarEditorPluginImages.IMG_FILE_REFERENCE);
		} else if (element instanceof ISoarDatamapAttribute) {
			return SoarEditorPluginImages
					.get(SoarEditorPluginImages.IMG_ATTRIBUTE);
		} else if (element instanceof ISoarAgent) {
			ISoarAgent soarAgent = (ISoarAgent) element;
			if (thisAgent != null
					&& thisAgent.getFile().equals(soarAgent.getFile())) {
				return SoarEditorPluginImages
						.get(SoarEditorPluginImages.IMG_THIS_AGENT);
			} else {
				return SoarEditorPluginImages
						.get(SoarEditorPluginImages.IMG_AGENT);
			}
		} else if (element instanceof ISoarFileAgentProxy) {
			ISoarFileAgentProxy proxy = (ISoarFileAgentProxy) element;

			return getImage(proxy.getAgent());
		} else if (element instanceof SoarFoldingRegion) {
			return SoarEditorPluginImages
					.get(SoarEditorPluginImages.IMG_FOLDING_REGION);
		} else if (element instanceof SoarDatabaseRow) {
			Table table = ((SoarDatabaseRow) element).getTable(); 
			if (table == Table.AGENTS) {
				return SoarEditorPluginImages
						.get(SoarEditorPluginImages.IMG_AGENT);
			} else if (table == Table.RULES) {
				return SoarEditorPluginImages
						.get(SoarEditorPluginImages.IMG_SOAR_FILE);
			} else if (table == Table.PROBLEM_SPACES) {
				return SoarEditorPluginImages
						.get(SoarEditorPluginImages.IMG_SOAR);
			} else if (table == Table.OPERATORS) {
				return SoarEditorPluginImages
						.get(SoarEditorPluginImages.IMG_OPERATOR);
			} else if (table == Table.DATAMAP_IDENTIFIERS) {
				return SoarEditorPluginImages
					.get(SoarEditorPluginImages.IMG_ATTRIBUTE);
			} else if (table == Table.DATAMAP_ENUMERATIONS) {
				return SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_ENUMERATION);
			} else if (table == Table.DATAMAP_STRINGS) {
				return SoarEditorPluginImages
					.get(SoarEditorPluginImages.IMG_STRING);
			} else if (table == Table.DATAMAP_FLOATS) {
				return SoarEditorPluginImages
				.get(SoarEditorPluginImages.IMG_FLOAT);		
			} else if (table == Table.DATAMAP_INTEGERS) {
				return SoarEditorPluginImages
				.get(SoarEditorPluginImages.IMG_INTEGER);
			} else if (table == Table.DATAMAP_ENUMERATION_VALUES) {
				return SoarEditorPluginImages
				.get(SoarEditorPluginImages.IMG_ENUMERATION_VALUE);
			} else if (table == Table.TAGS) {
				return SoarEditorPluginImages
				.get(SoarEditorPluginImages.IMG_TAG);
			}
		} else if (element instanceof SoarDatabaseRowFolder || element instanceof SoarDatabaseJoinFolder) {
			return SoarEditorPluginImages
				.get(SoarEditorPluginImages.IMG_PROJECT);
		}
		return super.getImage(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof ISoarDatabaseTreeItem) {
			String ret = element.toString();
			
			// Maybe add asterix if element has linked items
			if (element instanceof SoarDatabaseRow) {
				SoarDatabaseRow row = (SoarDatabaseRow) element;
				if (row.getUndirectedJoinedRowsFromTable(row.getTable()).size() > 0) {
					ret += "*";
				}
			}
			return ret;
			
		} else if (element instanceof ISoarProject) {
			ISoarProject project = (ISoarProject) element;

			return project.getProject().getName();
		} else if (element instanceof ISoarAgent) {
			ISoarAgent agent = (ISoarAgent) element;

			return agent.getName();
		} else if (element instanceof ISoarFile) {
			ISoarFile file = (ISoarFile) element;

			return file.getPath().lastSegment();
		} else if (element instanceof ISoarFileAgentProxy) {
			ISoarFileAgentProxy proxy = (ISoarFileAgentProxy) element;

			return proxy.getAgent().getName();
		} else if (element instanceof SoarFolderHeader) {
			SoarFolderHeader header = (SoarFolderHeader) element;

			return header.getLabel();
		} else if (element instanceof ITclCommand) {
			if (element instanceof ITclProcedure) {
				ITclProcedure procedure = (ITclProcedure) element;
				return procedure.getProcedureName();
			} else if (element instanceof ISoarProduction) {
				ISoarProduction production = (ISoarProduction) element;
				return production.getProductionName();
			}
		} else if (element instanceof ITclFileReference) {
			ITclFileReference ref = (ITclFileReference) element;
			IPath relative = ref.getWorkspacePath();

			return relative != null ? relative.toString() : ref
					.getReferencedLocation().toString();
		} else if (element instanceof ISoarDatamapAttribute) {
			ISoarDatamapAttribute a = (ISoarDatamapAttribute) element;
			String name = a.getName();
			if (name == null) {
				name = "<*>";
			}
			String mods = "";
			int usage = a.getOverallUsage();
			if ((usage & ISoarDatamapAttribute.USAGE_TEST) != 0) {
				mods = "?";
			}
			if ((usage & ISoarDatamapAttribute.USAGE_ADD) != 0) {
				mods += "+";
			}
			if ((usage & ISoarDatamapAttribute.USAGE_REMOVE) != 0) {
				mods += "-";
			}
			return name + (mods.length() > 0 ? " (" + mods + ")" : mods);
		} else if (element instanceof SoarFoldingRegion) {
			return ((SoarFoldingRegion) element).getName();
		}  else if (element instanceof SoarDatabaseDatamapSuperstateAttribute) {
			return element.toString(); 
		} else if (element instanceof String) {
			return (String) element;
		}

		return "<Unknown Label>";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang
	 * .Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return getImage(element);
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang
	 * .Object, int)
	 */
	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return getText(element);
		} else {
			return getValueLabel(element);
		}
	}

	/*
	 * Made this public static so there's a consistent way to get the value
	 * label for an Attribute.
	 * 
	 * Miller Tinkerhess 3/18/2010
	 */
	public static String getValueLabel(Object element) {
		if (element instanceof ISoarDatamapAttribute) {
			// Collect the values as strings
			ISoarDatamapAttribute a = (ISoarDatamapAttribute) element;
			List<String> strings = new ArrayList<String>();
			for (ISoarDatamapValue v : a.getTarget().getValues()) {
				strings.add(v.toString());
			}

			// Sort them
			Collections.sort(strings);

			// Join them with commas
			StringBuilder b = new StringBuilder();
			boolean first = true;
			for (String s : strings) {
				if (!first) {
					b.append(", ");
				}
				b.append(s);
				first = false;
			}
			return b.toString();
		}
		return "";
	}
}
