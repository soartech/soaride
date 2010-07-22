package com.soartech.soar.ide.ui.views.search;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal.TraversalUtil;
import com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal.Triple;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

public class SoarDatabaseSearchResultsView extends ViewPart {

	public static final String ID = "com.soartech.soar.ide.ui.views.SoarDatabaseSearchResultsView";
	
	TableViewer table;

	@Override
	public void createPartControl(Composite parent) {
		table = new TableViewer(parent);
		table.setContentProvider(new ArrayContentProvider());
		table.setLabelProvider(SoarLabelProvider.createFullLabelProvider(null));
		table.setInput(null);
	}

	@Override
	public void setFocus() {
		
	}
	
	public void setSearchResults(Object[] results) {
		table.setInput(results);
	}
	
	public static void searchForRulesWithDatamapAttribute(SoarDatabaseRow attribute) {
		assert attribute.getTable().isDatamapTable();
		String[] fullPath = attribute.getPathName().split("\\.");
		String[] path = new String[fullPath.length - 1];
		for (int i = 0; i < path.length; ++i) {
			path[i] = fullPath[i + 1];
		}
		SoarDatabaseRow problemSpace = attribute.getAncestorRow(Table.PROBLEM_SPACES);
		ArrayList<ISoarDatabaseTreeItem> allRules = TraversalUtil.getRelatedRules(problemSpace);
		ArrayList<SoarDatabaseRow> result = new ArrayList<SoarDatabaseRow>();
		for (ISoarDatabaseTreeItem item : allRules) {
			SoarDatabaseRow rule = (SoarDatabaseRow) item;
			for (Triple triple : TraversalUtil.getTriplesForRule(rule)) {
				if (triple.matchesPath(path)) {
					result.add(rule);
					break;
				}
			}
		}
		try {
			SoarDatabaseSearchResultsView view = (SoarDatabaseSearchResultsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ID);
			view.setSearchResults(result.toArray());
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}
}
