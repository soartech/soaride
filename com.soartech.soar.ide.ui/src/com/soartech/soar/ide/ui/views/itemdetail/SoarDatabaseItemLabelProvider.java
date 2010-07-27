package com.soartech.soar.ide.ui.views.itemdetail;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

public class SoarDatabaseItemLabelProvider extends LabelProvider implements ITableLabelProvider {

	public static ILabelProvider getLabelProvider() {
		return new DecoratingLabelProvider(new SoarDatabaseItemLabelProvider(),
				PlatformUI.getWorkbench().getDecoratorManager()
						.getLabelDecorator());
	}
	
	
	SoarLabelProvider provider = new SoarLabelProvider();
	

	public SoarDatabaseItemLabelProvider() {
		super();
	}
	
	@Override
	public Image getImage(Object element) {
		return provider.getImage(element);
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof SoarDatabaseRow) {
			SoarDatabaseRow row = (SoarDatabaseRow) element;
			if (row.isDatamapNode()) {
				return row.getPathName();
			}
		}
		return provider.getText(element);
	}
	
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return getImage(element);
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		return getText(element);
	}
}