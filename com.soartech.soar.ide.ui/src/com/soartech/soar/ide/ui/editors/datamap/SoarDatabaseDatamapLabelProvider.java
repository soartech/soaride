package com.soartech.soar.ide.ui.editors.datamap;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class SoarDatabaseDatamapLabelProvider extends LabelProvider implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		String ret = null;
		if (element instanceof SoarDatabaseRow) {
			SoarDatabaseRow row = (SoarDatabaseRow) element;
			ret = row.getPathName();
		}
		return ret;
	}

}
