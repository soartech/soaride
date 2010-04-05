package com.soartech.soar.ide.core.sql;

public interface ISoarDatabaseEventListener {
	
	void onEvent(SoarDatabaseEvent event, SoarDatabaseConnection db);
	
}
