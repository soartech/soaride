package com.soartech.soar.ide.core.sql;

import java.util.List;


public interface ISoarDatabaseRow {
	List<ISoarDatabaseRow> getChildren();

	boolean hasChildren();
}
