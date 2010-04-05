package com.soartech.soar.ide.core.sql;

public class SoarDatabaseEvent {
	
	public enum Type {
		DATABASE_CHANGED,
	}
	
	private Type type;
	
	public SoarDatabaseEvent(Type type) {
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
}
