package com.soartech.soar.ide.core.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;

public class StatementWrapper {

	private PreparedStatement ps;
	SoarDatabaseConnection db;

	public StatementWrapper(PreparedStatement ps, SoarDatabaseConnection db) {
		this.ps = ps;
		this.db = db;
	}

	public void setInt(int index, int value) {
		try {
			ps.setInt(index, value);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void setString(int index, String value) {
		try {
			ps.setString(index, value);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Also closes PreparedStatement.
	 */
	public void execute() {
		try {
			ps.execute();
			ps.close();
			db.fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public ResultSet executeQuery() {
		try {
			ResultSet ret = ps.executeQuery();
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void close() {
		try {
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
