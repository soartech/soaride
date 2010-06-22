package com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class Variable {
	// e.g. "<s>"
	public String name;

	public SoarDatabaseRow rule;

	public Variable(String name, SoarDatabaseRow rule) {
		assert rule == null || rule.getTable() == Table.RULES;
		this.name = name;
		this.rule = rule;
	}

	@Override
	public int hashCode() {
		if (rule != null) {
			return name.hashCode() + rule.hashCode();
		}
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Variable) {
			Variable other = (Variable) obj;
			if (this.rule == null && other.rule == null) {
				boolean ret = this.name.equals(other.name);
				return ret;
			}
			boolean ret = this.name.equals(other.name) && this.rule.equals(other.rule);
			return ret;
		}
		return false;
	}
	
	// return name without brackets
	public String strippedString() {
		int startIndex = 0;
		int endIndex = name.length() - 1;
		if (name.startsWith("<")) startIndex = 1;
		if (name.endsWith(">")) --endIndex;
		return name.substring(startIndex, endIndex);
	}

	@Override
	public String toString() {
		if (rule != null) {
			return name + " (" + rule.getName() + ")";
		}
		return name;
	}
}
