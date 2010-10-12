package com.soartech.soar.ide.core.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;

public class StatementWrapper {

	private PreparedStatement ps;
	SoarDatabaseConnection db;
	private String sql;
	private SoarDatabaseRow row;
	
	public StatementWrapper(PreparedStatement ps, SoarDatabaseConnection db, String sql) {
		this.ps = ps;
		this.db = db;
		this.sql = sql;
	}
	
	public void setBoolean(int index, boolean value) {
		try {
			ps.setBoolean(index, value);
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
	
	public void setFloat(int index, Float value) {
		try {
			ps.setFloat(index, value);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void setObject(int index, Serializable value) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(value);
			oos.flush();
			bos.flush();
			ps.setBytes(index, bos.toByteArray());
			oos.close();
			bos.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setRow(SoarDatabaseRow row) {
		this.row = row;
	}
	
	public SoarDatabaseRow getRow() {
		return row;
	}
	
	public String getSql() {
		return sql;
	}

	/**
	 * Also closes PreparedStatement.
	 */
	public void execute() {
		if (SoarDatabaseConnection.debug) {
			try {
				System.out.print("Executing statement: \"" + sql + "\", " +  ps.getParameterMetaData() + " ... ");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			ps.execute();
			close();
			db.fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED, row));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (SoarDatabaseConnection.debug) {
			System.out.println("done.");
		}
	}
	
	public ResultSet executeQuery() {
		if (SoarDatabaseConnection.debug) {
			try {
				System.out.print("Executing statement: \"" + sql + "\", " + ps.getParameterMetaData() + " ... ");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			ResultSet ret = ps.executeQuery();
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (SoarDatabaseConnection.debug) {
			System.out.println("done.");
		}
		return null;
	}
	
	public void close() {
		//if (db.reusedStatements.containsKey(sql)) return; // This is reusable, don't close it.
		try {
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
