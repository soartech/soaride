package com.soartech.soar.ide.core.sql.datamap;

import java.util.ArrayList;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.Triple;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

/**
 * Represents a single correction to be made to the existing datamap.
 * The user can choose which corrections to apply and which to ignore.
 * @author miller
 *
 */
public class Correction {
	SoarDatabaseRow row;
	public ArrayList<Triple> addition;
	ArrayList<Triple> links = new ArrayList<Triple>();
	
	// Assigned during apply()
	SoarDatabaseRow tail = null;

	/**
	 * Class constructor.
	 * @param row
	 * @param addition
	 * @param links
	 */
	public Correction(SoarDatabaseRow row, ArrayList<Triple> addition, ArrayList<Triple> links) {
		this.row = row;
		this.addition = addition;
		this.links = links;
	}

	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append(row.getPathName());
		for (Triple triple : addition) {
			buff.append("." + triple.attribute);
		}
		Triple last = addition.get(addition.size() - 1); 
		if (last.valueIsConstant()) {
			buff.append(" " + last.value);
		}
		if (links != null && links.size() > 0) {
			buff.append(", link with:");
			for (Triple triple : links) {
				buff.append(" " + triple);
			}
		}
		return buff.toString();
	}

	/**
	 * Applys this correction to its datamap.
	 */
	public void apply() {
		SoarDatabaseRow currentRow = row;
		for (int i = 0; i < addition.size(); ++i) {
			Triple triple = addition.get(i);
			if (triple.valueIsVariable()) {
				currentRow = createJoinedChildIfNotExists(currentRow, Table.DATAMAP_IDENTIFIERS, triple.attribute);
			} else if (triple.valueIsInteger()) {
				currentRow = createJoinedChildIfNotExists(currentRow, Table.DATAMAP_INTEGERS, triple.attribute);
				editMinMaxValues(currentRow, triple);
			} else if (triple.valueIsFloat()) {
				currentRow = createJoinedChildIfNotExists(currentRow, Table.DATAMAP_FLOATS, triple.attribute);
				editMinMaxValues(currentRow, triple);
			} else if (triple.valueIsString()) {
				if (triple.value.equals(Triple.STRING_VALUE)) {
					currentRow = createJoinedChildIfNotExists(currentRow, Table.DATAMAP_STRINGS, triple.attribute);
				} else {
					ArrayList<ISoarDatabaseTreeItem> enumerations = currentRow.getDirectedJoinedChildrenOfType(Table.DATAMAP_ENUMERATIONS, false, false);
					SoarDatabaseRow enumeration = null;
					for (ISoarDatabaseTreeItem enumItem : enumerations) {
						SoarDatabaseRow enumRow = (SoarDatabaseRow) enumItem;
						if (enumRow.getName().equals(triple.value)) {
							enumeration = enumRow;
							break;
						}
					}

					if (enumeration == null) {
						enumeration = createJoinedChildIfNotExists(currentRow, Table.DATAMAP_ENUMERATIONS, triple.attribute);
					}
					ArrayList<SoarDatabaseRow> enumValues = enumeration.getChildrenOfType(Table.DATAMAP_ENUMERATION_VALUES);
					boolean hasValue = false;
					for (SoarDatabaseRow valueRow : enumValues) {
						if (valueRow.getName().equals(triple.value)) {
							hasValue = true;
							break;
						}
					}
					if (!hasValue) {
						enumeration.createChild(Table.DATAMAP_ENUMERATION_VALUES, triple.value);
					}
					currentRow = enumeration;
				}
			}
			if (triple.comment != null) {
				currentRow.setComment(triple.comment);
			}
		}
		tail = currentRow;
	}
	
	/**
	 * Edits min_value and max_value to include the value of the triple.
	 * @param row
	 * @param triple
	 */
	private static void editMinMaxValues(SoarDatabaseRow row, Triple triple) {
		assert row.getTable() == Table.DATAMAP_FLOATS || row.getTable() == Table.DATAMAP_INTEGERS;
		Object minVal = row.getColumnValue("min_value");
		Object maxVal = row.getColumnValue("max_value");
		if (triple.valueIsFloat()) {
			Double minValue = (Double) minVal;
			Double maxValue = (Double) maxVal;
			double value = Double.parseDouble(triple.value);
			if (minValue == null) {
				minValue = value;
			}
			if (maxValue == null) {
				maxValue = value;
			}
			if (value < minValue) {
				minValue = value;
			}
			if (value > maxValue) {
				maxValue = value;
			}
			if (!minValue.equals(minVal)) {
				row.updateValue("min_value", "" + minValue);
			}
			if (!maxValue.equals(maxVal)) {
				row.updateValue("max_value", "" + maxValue);
			}
		}
		else if (triple.valueIsInteger()) {
			Integer minValue = (Integer) minVal;
			Integer maxValue = (Integer) maxVal;
			int value = Integer.parseInt(triple.value);
			if (minValue == null) {
				minValue = value;
			}
			if (maxValue == null) {
				maxValue = value;
			}
			if (value < minValue) {
				minValue = value;
			}
			if (value > maxValue) {
				maxValue = value;
			}
			if (!minValue.equals(minVal)) {
				row.updateValue("min_value", "" + minValue);
			}
			if (!maxValue.equals(maxVal)) {
				row.updateValue("max_value", "" + maxValue);
			}
		}
	}
	
	/**
	 * Once all corrections have been applied, this is called to link items in the corrections to each other where needed.
	 */
	public void applyLinks() {
		for (Triple link : links) {
			ArrayList<SoarDatabaseRow> rows = link.getDatamapRowsFromProblemSpace(row.getAncestorRow(Table.PROBLEM_SPACES));
			for (SoarDatabaseRow row : rows) {
				SoarDatabaseRow.joinRows(row, tail, row.getDatabaseConnection());
			}
		}
	}

	/**
	 * Looks for a child of the given row, of the given type. If none exists, creates a new row and returns that.
	 * @param currentRow
	 * @param table
	 * @param named
	 * @return
	 */
	private SoarDatabaseRow createJoinedChildIfNotExists(SoarDatabaseRow currentRow, Table table, String named) {
		ArrayList<ISoarDatabaseTreeItem> childItems = currentRow.getDirectedJoinedChildrenOfType(table, false, false);
		for (ISoarDatabaseTreeItem childItem : childItems) {
			SoarDatabaseRow childRow = (SoarDatabaseRow) childItem;
			if (childRow.getName().equals(named)) {
				return childRow;
			}
		}
		return currentRow.createJoinedChild(table, named);
	}
}
