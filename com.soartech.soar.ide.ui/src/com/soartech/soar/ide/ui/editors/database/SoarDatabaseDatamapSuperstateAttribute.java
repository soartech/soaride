package com.soartech.soar.ide.ui.editors.database;

import java.util.ArrayList;
import java.util.List;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseDatamapSuperstateAttribute {

	SoarDatabaseRow substate;
	SoarDatabaseRow superstate;
	
	public SoarDatabaseDatamapSuperstateAttribute(SoarDatabaseRow substate, SoarDatabaseRow superstate) {
		this.substate = substate;
		this.superstate = superstate;
	}

	public List<Object> getChildren(SoarDatabaseDatamapContentProvider contentProvider) {
		
		assert substate.getTable() == Table.PROBLEM_SPACES;
		
		ArrayList<Object> ret = new ArrayList<Object>();
		
		if (superstate == null) {
			return ret;
		}
		
		ArrayList<ISoarDatabaseTreeItem> superstateChildren = superstate.getChildrenOfType(Table.DATAMAP_IDENTIFIERS);
		for (ISoarDatabaseTreeItem item : superstateChildren) {
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow superstateChild = (SoarDatabaseRow) item;
				// this is superstate's <s>
				Object[] ar = contentProvider.getChildren(superstateChild);
				for (Object obj : ar) {
					ret.add(obj);
				}
			}
		}
		return ret;
	}

	public boolean hasChildren(SoarDatabaseDatamapContentProvider contentProvider) {
		return getChildren(contentProvider).size() > 0;
	}
	
	@Override
	public String toString() {
		if (superstate == null) {
			return "superstate (none)";
		}
		return "superstate (" + superstate.getName() + ")";
	}
}
