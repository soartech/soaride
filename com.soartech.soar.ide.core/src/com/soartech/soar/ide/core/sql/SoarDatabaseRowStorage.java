package com.soartech.soar.ide.core.sql;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseRowStorage implements IStorage {

	private SoarDatabaseRow row;
	
	public SoarDatabaseRowStorage(SoarDatabaseRow row) {
		this.row = row;
	}
	
	public SoarDatabaseRow getRow() {
		return row;
	}
	
	@Override
	public InputStream getContents() throws CoreException {
		Table rowTable = row.getTable();
		if (rowTable == Table.RULES || rowTable == Table.AGENTS) {
			return new ByteArrayInputStream(row.getText().getBytes());
		}
		else if (rowTable == Table.OPERATORS) {
			StringBuffer buff = new StringBuffer();
			ArrayList<ISoarDatabaseTreeItem> rules = row.getJoinedRowsFromTable(Table.RULES);
			for (ISoarDatabaseTreeItem item : rules) {
				if (item instanceof SoarDatabaseRow) {
					SoarDatabaseRow rule = (SoarDatabaseRow) item;
					buff.append(rule.getText());
					buff.append("\n\n");
				}
			}
			if (buff.length() > 2) {
				return new ByteArrayInputStream(buff.substring(0, buff.length() - 2).getBytes());
			} else {
				return new ByteArrayInputStream(buff.toString().getBytes());
			}
		}
		return null;
	}

	@Override
	public IPath getFullPath() {
		return null;
	}

	@Override
	public String getName() {
		return row.toString();
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SoarDatabaseRowStorage) {
			boolean ret = ((SoarDatabaseRowStorage) obj).getRow().equals(this.getRow());
			return ret;
		}
		return false;
	}
}
