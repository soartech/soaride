package com.soartech.soar.ide.core.sql;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.sqlite.SQLite;

import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ast.Action;
import com.soartech.soar.ide.core.model.ast.Condition;
import com.soartech.soar.ide.core.model.ast.PositiveCondition;
import com.soartech.soar.ide.core.model.ast.SoarProductionAst;
import com.soartech.soar.ide.core.model.ast.VarAttrValMake;
import com.soartech.soar.ide.core.model.impl.SoarAgent;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseConnection {

	private String framework = "embedded";
	private String driver = "org.sqlite.JDBC";
	private String protocol = "jdbc:sqlite::memory:";
	private String dbName = ""; // the name of the database
	private String[] sqlFiles = { "agent.sql" , "rule.sql" }; // , "datamap.sql" };
	private Connection conn;
	private boolean debug = true;

	private ArrayList<ISoarDatabaseEventListener> listeners = new ArrayList<ISoarDatabaseEventListener>();
	private boolean supressEvents = false;
	
	public SoarDatabaseConnection() {
		
		// Load the driver
		loadDriver();
		
		// Connect to the database
		try {
			conn = DriverManager.getConnection(protocol + dbName + ";create=true");
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Read the schema from files
		buildSchema();
		
		// Add items to schema for testing.
		test();
	}

	public void addListener(ISoarDatabaseEventListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	public void removeListener(ISoarDatabaseEventListener listener) {
		listeners.remove(listener);
	}

	public void fireEvent(SoarDatabaseEvent event) {
		if (!supressEvents) {
			for (ISoarDatabaseEventListener listener : listeners) {
				listener.onEvent(event, this);
			}
		}
	}
	
	public void setSupressEvents(boolean supress) {
		supressEvents = supress;
	}

	public boolean getSupressEvents() {
		return supressEvents;
	}
	
	private void test() {
		insert(Table.AGENTS, new String[][] {{"name", "\"Agent 0\""}});
	}

	/**
	 * Loads the appropriate JDBC driver for this environment/framework. For
	 * example, if we are in an embedded environment, we load Derby's embedded
	 * Driver, <code>org.apache.derby.jdbc.EmbeddedDriver</code>.
	 */
	private void loadDriver() {
		/*
		 * The JDBC driver is loaded by loading its class. If you are using JDBC
		 * 4.0 (Java SE 6) or newer, JDBC drivers may be automatically loaded,
		 * making this code optional.
		 * 
		 * In an embedded environment, this will also start up the Derby engine
		 * (though not any databases), since it is not already running. In a
		 * client environment, the Derby engine is being run by the network
		 * server framework.
		 * 
		 * In an embedded environment, any static Derby system properties must
		 * be set before loading the driver to take effect.
		 */
		try {
			Class.forName(driver).newInstance();
			System.out.println("Loaded the appropriate driver");
		} catch (ClassNotFoundException cnfe) {
			System.err.println("\nUnable to load the JDBC driver " + driver);
			System.err.println("Please check your CLASSPATH.");
			cnfe.printStackTrace(System.err);
		} catch (InstantiationException ie) {
			System.err.println("\nUnable to instantiate the JDBC driver "
					+ driver);
			ie.printStackTrace(System.err);
		} catch (IllegalAccessException iae) {
			System.err.println("\nNot allowed to access the JDBC driver "
					+ driver);
			iae.printStackTrace(System.err);
		}
	}

	/*
	 * Connects to the database
	 */
	private void buildSchema() {
		try {
			Statement s = conn.createStatement();
			for (String filename : sqlFiles) {
				executeFile(filename, s);
			}
			s.close();
			conn.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void executeFile(String filename, Statement s) {
		InputStream fileStream = getClass().getResourceAsStream(filename);
		InputStreamReader reader = new InputStreamReader(fileStream);
		StringBuilder builder = new StringBuilder();
		int c = 0;
		int lookahead = 0;
		boolean singleLineComment = false;
		boolean multiLineComment = false;
		boolean write = true;
		boolean writeNext = true;
		boolean execute = false;
		try {
			while (c != -1) {

				write = writeNext;
				writeNext = true;
				execute = false;

				if (c == 0) {
					write = false;
				}

				if ((char) c == '/' && (char) lookahead == '*') {
					multiLineComment = true;
					write = false;
				}
				if ((char) c == '-' && (char) lookahead == '-') {
					singleLineComment = true;
					write = false;
				}
				if ((char) c == '*' && (char) lookahead == '/') {
					multiLineComment = false;
					write = false;
					writeNext = false;
				}
				if (singleLineComment && (char) lookahead == '\n') {
					singleLineComment = false;
					write = false;
				}
				if ((char) c == '\n') {
					write = false;
				}

				/*
				 * System.out.println("c: " + (char)c);
				 * System.out.println("Write: " + write);
				 * System.out.println("singleLineComment: " +
				 * singleLineComment); System.out.println("multiLineComment: " +
				 * multiLineComment); System.out.println("command: " +
				 * builder.toString()); System.out.println();
				 */

				if (singleLineComment || multiLineComment) {
					write = false;
				}

				if (write && (char) c == ';' && (char) lookahead == '\n') {
					execute = true;
					write = false;
				}

				if (write) {
					builder.append((char) c);
				}

				if (execute) {
					String command = builder.toString();
					builder = new StringBuilder();
					execute(command);
				}

				c = lookahead;
				lookahead = reader.read();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Enters the production in the database.
	 * @param ast
	 */
	public void writeProductionToDatabase(ISoarProduction production, SoarAgent agent) {
		int agentID = 0;
		SoarProductionAst ast = production.getSyntaxTree();
		if (ast != null) {
			//writeAst(ast, agentID);
		}
	}
	
	private void writeAst(SoarProductionAst ast, int agentID) {
		System.out.println("DatabaseConnection.writeAst: " + ast);
		try {
			
			String sql = "insert into productions (agent_id, name, comment) values (?, ?, ?)";
			PreparedStatement s = conn.prepareStatement(sql);
			s.setInt(1, agentID);
			String name = ast.getName();
			s.setString(2, name);
			String comment = ast.getComment();
			if (comment == null) {
				comment = "";
			}
			s.setString(3, comment);
			s.executeUpdate();
			//sql = ""
			// 
			/*
			ResultSet result = s.getResultSet();
			result.next();
			int productionID = result.getInt("id");

			for (Condition condition : ast.getConditions()) {
				writeConditionFromProduction(condition, productionID);
			}

			for (Action action : ast.getActions()) {
				writeActionFromProduction(action, productionID);
			}
			*/
			
		} catch (SQLException e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeConditionFromProduction(Condition condition, int productionID) {
		System.out.println("DatabaseConnection.writeConditionFromProduction: " + condition);
		try {
			String sql = "insert into conditions (production_id) values (?)";
			PreparedStatement s = conn.prepareStatement(sql);
			s.setInt(1, productionID);
			if (s.execute()) {
				PositiveCondition positiveCondition = condition.getPositiveCondition();
				
			}
		} catch (SQLException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void writeActionFromProduction(Action action, int productionID) {
		System.out.println("DatabaseConnection.writeActionFromProduction: " + action);
		try {
			String sql = "insert into conditions (production_id) values (?)";
			PreparedStatement s = conn.prepareStatement(sql);
			s.setInt(1, productionID);
			if (s.execute()) {
				VarAttrValMake varAttrValueMake = action.getVarAttrValMake();
				
			}
		} catch (SQLException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<SoarDatabaseRow> selectAllFromTable(SoarDatabaseRow.Table table) {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		String tableName = table.toString().toLowerCase();
		String sql = "select * from " + tableName;
		ResultSet rs = getResultSet(sql);
		try {
			while(rs.next()) {
				SoarDatabaseRow row = new SoarDatabaseRow(table, rs.getInt("id"), this);
				ret.add(row);
			}
			rs.getStatement().close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	
	public void insert(Table table, String [][] fields) {
		String sql = "insert into " + table.tableName() + " (";
		for (int i = 0; i < fields.length; ++i) {
			String [] pair = fields[i];
			if (i == 0) {
				sql += pair[0];
			} else {
				sql += ", " + pair[0];
			}
		}
		sql += ") values (";
		for (int i = 0; i < fields.length; ++i) {
			String [] pair = fields[i];
			if (i == 0) {
				sql += pair[1];
			} else {
				sql += ", " + pair[1];
			}
		}
		sql += ")";
		execute(sql);
	}
	
	/**
	 * Adds a new row to the database.
	 * @param parent
	 * @param childTable
	 * @param fields An array of column name / value pairs.
	 */
	public void createChild(SoarDatabaseRow parent, Table childTable, String[][] fields) {
		String sql = "insert into " + childTable.tableName() + " (" + parent.getTable().idName();
		for (String[] pair : fields) {
			sql += ", " + pair[0];
		}
		sql += ") values (" + parent.getID();
		for (String[] pair : fields) {
			sql += ", " + pair[1];
		}
		sql += ")";
		execute(sql);
		
	}
	
	public void createChild(SoarDatabaseRow parent, Table childTable) {
		createChild(parent, childTable, new String[][] {});
	}
	
	public void createChild(SoarDatabaseRow parent, Table childTable, String name) {
		createChild(parent, childTable, new String[][] {{"name", "\"" + name + "\""}});
	}
	
	/**
	 * Caller is responsible for closing statement.
	 * e.g. "resultSet.getStatement().close();"
	 * 
	 * @param sql
	 * @return
	 */
	public ResultSet getResultSet(String sql) {
		if (debug) {
			System.out.print("Executing sql command: \"" + sql + "\" ... ");
		}
		ResultSet ret = null;
		Statement s;
		try {
			s = conn.createStatement();
			ret = s.executeQuery(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (debug) {
			System.out.println("done.");
		}
		return ret;
	}
	
	public void execute(String sql) {
		if (debug) {
			System.out.print("Executing sql command: \"" + sql + "\" ... ");
		}
		Statement s;
		try {
			s = conn.createStatement();
			s.execute(sql);
			s.close();
			conn.commit();
			fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (debug) {
			System.out.println("done.");
		}
	}
}
