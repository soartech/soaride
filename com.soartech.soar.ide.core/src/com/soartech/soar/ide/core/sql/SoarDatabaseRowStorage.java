package com.soartech.soar.ide.core.sql;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

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
		return new ByteArrayInputStream(row.getText().getBytes());
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
