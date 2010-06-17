package com.soartech.soar.ide.core.sql;

import java.util.HashSet;

public class SoarDatabaseEvent {
	
	public enum Type {
		DATABASE_CHANGED,
		DATABASE_PATH_CHANGED,
	}
	
	public Type type;
	public SoarDatabaseRow row;
	
	public SoarDatabaseEvent(Type type) {
		this.type = type;
		row = null; 
	}
	
	public SoarDatabaseEvent(Type type, SoarDatabaseRow row) {
		this.type = type;
		this.row = row;
	}
}