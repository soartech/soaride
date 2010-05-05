package com.soartech.soar.ide.core.sql;

import java.util.ArrayList;
import java.util.List;

public class EditableColumn {
	
	public enum Type {
		INTEGER,
		FLOAT,
		STRING,
	};
	
	private String name;
	private Type type;
	
	public EditableColumn(String name, Type type) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return name;
	}
	
	public Type getType() {
		return type;
	}
	
	public boolean objectIsRightType(Object obj) {
		Type objectType = typeForObject(obj);
		if (objectType != null) {
			if (objectType == this.type) {
				return true;
			}
		}
		return false;
	}
	
	public static Type typeForObject(Object obj) {		
		if (obj instanceof Integer) {
			return Type.INTEGER;
		}
		if (obj instanceof Float) {
			return Type.FLOAT;
		}
		if (obj instanceof String) {
			return Type.STRING;
		}
		return null;
	}
}
