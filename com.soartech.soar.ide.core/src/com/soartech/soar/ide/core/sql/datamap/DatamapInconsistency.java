package com.soartech.soar.ide.core.sql.datamap;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

/**
 * Represents an inconsistancy between a row and
 * a datamap.
 * 
 * @author miller
 *
 */
public class DatamapInconsistency {
	public SoarDatabaseRow problemSpace;
	public SoarDatabaseRow rule;
	public String message;
	public int offset; // 0-indexed
	public int line;
	public int column;
	
	public DatamapInconsistency(SoarDatabaseRow rule, SoarDatabaseRow problemSpace, String message, int offset) {
		this.problemSpace = problemSpace;
		this.rule = rule;
		this.message = message;
		this.offset = offset - 1; // Convert from 1-indexed
		setColumnAndRow();
	}
	
	private void setColumnAndRow() {
		
		String text = rule.getText();
		String trimmed = text.trim();
		int beginIndex = 0;
		if (trimmed.startsWith("sp") || trimmed.startsWith("gp")) {
			beginIndex = text.indexOf("sp") + 2;
			if (beginIndex == 1) beginIndex = text.indexOf("gp") + 2;
			trimmed = text.substring(beginIndex).trim();
			if (trimmed.startsWith("{")) {
				beginIndex = text.indexOf("{", beginIndex) + 1;
				trimmed = text.substring(beginIndex).trim();
			} else {
				// error
				column = -1;
				line = -1;
				return;
			}
		} else {
			// error
			column = -1;
			line = -1;
			return;
		}
		offset += beginIndex;
		
		column = 0;
		line = 1;
		int i = 0;
		while (i < offset) {
			int next = text.indexOf('\n', i + 1);
			if (next == -1 || next > offset) {
				break;
			}
			++line;
			i = next;
		}
		column = offset - i - 1;
	}
	
	public String toString() {
		return rule.getName() + ":" + line + " (" + problemSpace.getName() + ") " + message;
	}
}