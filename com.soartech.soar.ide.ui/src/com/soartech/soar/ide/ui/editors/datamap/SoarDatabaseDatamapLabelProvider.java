package com.soartech.soar.ide.ui.editors.datamap;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

public class SoarDatabaseDatamapLabelProvider extends LabelProvider implements ITableLabelProvider {

	ILabelProvider provider;
	
	public SoarDatabaseDatamapLabelProvider() {
		provider = SoarLabelProvider.createFullLabelProvider();
	}
	
	public static ILabelProvider create() {
		return new DecoratingLabelProvider(new SoarDatabaseDatamapLabelProvider(),
				PlatformUI.getWorkbench().getDecoratorManager()
						.getLabelDecorator());
	}
	
	@Override
	public Image getImage(Object element) {
		return provider.getImage(element);
	}
	
	@Override
	public String getText(Object element) {
		String ret = null;
		if (element instanceof SoarDatabaseRow) {
			SoarDatabaseRow row = (SoarDatabaseRow) element;
			ret = row.getName();
		}
		return ret;
	}
	
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return this.getImage(element);
		}
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return this.getText(element);
		}
		if (columnIndex == 1) {
			if (element instanceof SoarDatabaseRow) {
				SoarDatabaseRow row = (SoarDatabaseRow) element;
				return row.getComment();
			}
		}
		return null;
	}

}
