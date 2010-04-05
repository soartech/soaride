package com.soartech.soar.ide.ui.views.datamap;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Item;

import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapValue;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

public class SoarDatamapCellModifier implements ICellModifier {

	private TreeViewer treeViewer;
	
	public SoarDatamapCellModifier(TreeViewer treeViewer)
	{
		this.treeViewer = treeViewer;
	}
	
	@Override
	public boolean canModify(Object element, String property) {
		System.out.println("SoarDatamapCellModifier.canModify( , " + property + ")");
		return element instanceof ISoarDatamapAttribute && property == "Values";
	}

	@Override
	public Object getValue(Object element, String property) {
		System.out.println("SoarDatamapCellModifier.getValue( , " + property + ")");
		if (element instanceof ISoarDatamapAttribute)
		{
			return SoarLabelProvider.getValueLabel(element);
		}
		
		return "";
	}

	@Override
	public void modify(Object element, String property, Object value) {
		
		if (element instanceof Item) {
	         element = ((Item) element).getData();
	     }
		
		System.out.println("CellModifier.modify( , " + property + ", " + value + ")");
		
		// If this is a new 'Values' entry for an attribute, change the attribute in the Datamap.
		if (element instanceof ISoarDatamapAttribute && property == "Values")
		{
			// Split the string 'value' into trimmed tokens.
			String[] newValues = value.toString().split(",");
			for (int i = 0; i < newValues.length; ++i)
			{
				newValues[i] = newValues[i].trim();
			}
			
			// Remove the current values.
			ISoarDatamapAttribute attr = (ISoarDatamapAttribute) element;
			ISoarDatamapNode target = attr.getTarget();
			target.clearValues();
			
			// Add new values.
			for (String s : newValues)
			{
				ISoarDatamapValue v = target.addValue(null, s);
				//v.
			}
			
			// Refresh the tree.
			treeViewer.refresh();
			
			// TODO: Save Datamap.
		}
	}
}
