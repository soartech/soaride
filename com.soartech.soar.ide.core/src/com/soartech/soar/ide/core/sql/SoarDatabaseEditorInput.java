package com.soartech.soar.ide.core.sql;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class SoarDatabaseEditorInput implements IStorageEditorInput {
	
    private SoarDatabaseRowStorage storage;
    
    public SoarDatabaseEditorInput(SoarDatabaseRow row) {
    	storage = new SoarDatabaseRowStorage(row);
    }
    
    public SoarDatabaseRowStorage getSoarDatabaseStorage() {
    	return storage;
    }
    
    public SoarDatabaseEditorInput(SoarDatabaseRowStorage storage) {
    	this.storage = storage;
    }
    
    public boolean exists() {
    	return true;
    }
    
    public ImageDescriptor getImageDescriptor() {
    	return null;
    }
    
    public String getName() {
       return storage.getName();
    }
    public IPersistableElement getPersistable() {
    	return null;
    }
    
    public IStorage getStorage() {
       return storage;
    }
    
    public String getToolTipText() {
		return "Soar database editor input";
	}

	public Object getAdapter(Class adapter) {
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SoarDatabaseEditorInput) {
			boolean ret =  ((SoarDatabaseEditorInput) obj).getSoarDatabaseStorage().equals(this.getSoarDatabaseStorage());
			return ret;
		}
		return false;
	}
}
