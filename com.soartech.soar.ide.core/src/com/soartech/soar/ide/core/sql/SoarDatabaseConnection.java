package com.soartech.soar.ide.core.sql;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseConnection {

	private String driver = "org.sqlite.JDBC";
	private String protocol = "jdbc:sqlite:";
	private String[] sqlFiles = { "agent.sql" , "rule.sql", "datamap.sql" };
	private Connection connection = null;
	public static final boolean debug = false;

	private ArrayList<ISoarDatabaseEventListener> listeners = new ArrayList<ISoarDatabaseEventListener>();
	private int suppressEvents = 0;
	private String currentPath;
	
	private boolean firingEvent = false;
	private ArrayList<ISoarDatabaseEventListener> toRemove = new ArrayList<ISoarDatabaseEventListener>();

	public HashMap<String, PreparedStatement> reusedStatements = new HashMap<String, PreparedStatement>();

	// Keep track of how often each statement is called.
	static HashMap<String, Integer> statementFrequency = new HashMap<String, Integer>();
	
	private static String[] sqls = {								
		"select * from directed_join_datamap_identifiers_datamap_integers where parent_id=?",		// Used 2440 times in a test run.
		"select * from join_datamap_identifiers_datamap_identifiers where (first_id=? and second_id=?) or (first_id=? and second_id=?)", // Used 415 times in a test run.
		"delete from directed_join_datamap_identifiers_datamap_enumerations where parent_id=? and child_id=?", // Used 5 times in a test run.
		"insert into triples (rule_id, variable_string, variable_offset, attribute_string, attribute_offset, value_string, value_offset, has_state) values (?,?,?,?,?,?,?,?)", // Used 2308 times in a test run.
		"select * from directed_join_problem_spaces_problem_spaces where parent_id=?",			// Used 679 times in a test run.
		"select (name) from datamap_enumeration_values where id=?",								// Used 5631 times in a test run.
		"select join_type from directed_join_operators_problem_spaces where parent_id=? and child_id=?", // Used 133 times in a test run.
		"select * from problem_spaces where agent_id=?",											// Used 287 times in a test run.
		"select * from directed_join_problem_spaces_rules where parent_id=? and child_id=?",		// Used 15 times in a test run.
		"select * from problem_spaces where id=?",												// Used 152 times in a test run.
		"select * from directed_join_operators_problem_spaces where child_id=?",					// Used 16 times in a test run.
		"select * from problem_spaces where agent_id=? order by name",							// Used 19 times in a test run.
		"select * from datamap_enumeration_values where id=(last_insert_rowid())",				// Used 154 times in a test run.
		"select * from agents where id = (select agent_id from operators where id=?)",			// Used 18 times in a test run.
		"select * from operators where agent_id=?",												// Used 263 times in a test run.
		"select * from directed_join_datamap_identifiers_datamap_enumerations where parent_id=?",	// Used 2866 times in a test run.
		"select (name) from agents where id=?",													// Used 22 times in a test run.
		"select * from directed_join_problem_spaces_problem_spaces where child_id=?",				// Used 24 times in a test run.
		"select * from directed_join_operators_problem_spaces where parent_id=?",					// Used 4877 times in a test run.
		"select * from directed_join_problem_spaces_operators where child_id=?",					// Used 26 times in a test run.
		"select * from directed_join_operators_problem_spaces where parent_id=? and child_id=?",	// Used 151 times in a test run.
		"insert into directed_join_problem_spaces_operators (parent_id, child_id) values (?,?)",	// Used 34 times in a test run.
		"select * from directed_join_problem_spaces_operators where parent_id=?",					// Used 649 times in a test run.
		"select * from directed_join_datamap_identifiers_datamap_enumerations where child_id=?",	// Used 36 times in a test run.
		"select * from join_datamap_integers_datamap_integers where (first_id=?) or (second_id=?)", // Used 37 times in a test run.
		"select * from operators where id=(last_insert_rowid())",									// Used 47 times in a test run.
		"select * from directed_join_operators_rules where parent_id=? and child_id=?",			// Used 318 times in a test run.
		"select * from triples where rule_id=? and variable_string=? and attribute_string=? and value_string=?", // Used 4797 times in a test run.
		"select * from datamap_identifiers where problem_space_id=?",								// Used 429 times in a test run.
		"insert into join_datamap_identifiers_datamap_identifiers (first_id, second_id) values (?,?)", // Used 179 times in a test run.
		"select * from join_datamap_enumerations_datamap_enumerations where (first_id=?) or (second_id=?)", // Used 177 times in a test run.
		"select * from datamap_identifiers where id=(last_insert_rowid())",						// Used 182 times in a test run.
		"select (name) from problem_spaces where id=?",											// Used 1860 times in a test run.
		"select * from datamap_enumeration_values where datamap_enumeration_id=?",				// Used 1996 times in a test run.
		"select * from problem_spaces where id = (select problem_space_id from datamap_identifiers where id=?)", // Used 478 times in a test run.
		"select * from directed_join_datamap_identifiers_datamap_identifiers where child_id=?",	// Used 479 times in a test run.
		"select * from directed_join_datamap_identifiers_datamap_strings where parent_id=?",		// Used 2811 times in a test run.
		"select * from join_datamap_identifiers_datamap_identifiers where (first_id=?) or (second_id=?)", // Used 609 times in a test run.
		"select * from datamap_enumerations where id=?",											// Used 76 times in a test run.
		"select * from triples where id=?",														// Used 29197 times in a test run.
		"select * from datamap_identifiers where id=?",											// Used 194 times in a test run.
		"select * from directed_join_problem_spaces_rules where parent_id=?",						// Used 463 times in a test run.
		"select * from rules where id=(last_insert_rowid())",										// Used 222 times in a test run.
		"select (name) from datamap_integers where id=?",											// Used 1717 times in a test run.
		"select (name) from operators where id=?",												// Used 17428 times in a test run.
		"select * from directed_join_operators_rules where parent_id=?",							// Used 3894 times in a test run.
		"select * from datamap_integers where id=?",												// Used 753 times in a test run.
		"select * from directed_join_datamap_identifiers_datamap_identifiers where parent_id=?",	// Used 3735 times in a test run.
		"select (name) from datamap_strings where id=?",											// Used 101 times in a test run.
		"select (raw_text) from rules where id=?",												// Used 233 times in a test run.
		"select * from directed_join_problem_spaces_rules where child_id=?",						// Used 506 times in a test run.
		"select * from directed_join_datamap_identifiers_datamap_floats where parent_id=?",		// Used 2412 times in a test run.
		"select * from directed_join_problem_spaces_operators where parent_id=? and child_id=?",	// Used 107 times in a test run.
		"select * from rules where id = (select rule_id from triples where id=?)",				// Used 4171 times in a test run.
		"select (name) from rules where id=?",													// Used 68075 times in a test run.
		"select (name) from datamap_identifiers where id=?",										// Used 29613 times in a test run.
		"select * from triples where rule_id=?",													// Used 600 times in a test run.
		"select (name) from datamap_enumerations where id=?"										// Used 10186 times in a test run.
	};
	
	public static void printFrequencies() {
		HashMap<Integer, ArrayList<String>> reverse = new HashMap<Integer, ArrayList<String>>();
		for (String s : statementFrequency.keySet()) {
			Integer f = statementFrequency.get(s);
			ArrayList<String> l = reverse.get(f);
			if (l == null) l = new ArrayList<String>();
			l.add(s);
			reverse.put(f, l);
		}
		for (Integer i : reverse.keySet()) {
			System.out.println("\"" + reverse.get(i) + "\", // Used " + i + " times in a test run.");
		}
	}
		
	public SoarDatabaseConnection() throws FileNotFoundException {
		this(":memory:");
	}
	
	public SoarDatabaseConnection(String path) throws FileNotFoundException {
		loadDriver();
		if (!loadDatabaseConnection(path)) {
			throw new FileNotFoundException(path);
		}
		prepareStatements();
		// test();
	}
	
	private void prepareStatements() {
		for (String sql : sqls) {
			addReusedStatement(sql);
		}
	}
	
	void addReusedStatement(String sql) {
		try {
			PreparedStatement ps = connection.prepareStatement(sql);
			reusedStatements.put(sql, ps);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param path
	 * @return True on success, false otherwise
	 */
	public boolean loadDatabaseConnection(String path) {
		currentPath = path;
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		try {
			connection = DriverManager.getConnection(protocol + path);
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
			// Connection didn't load - bad path?
			// Don't build the schema.
			return false;
		}
		
		buildSchema();
		return true;
	}

	public void addListener(ISoarDatabaseEventListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	public void removeListener(ISoarDatabaseEventListener listener) {
		if (!firingEvent) {
			listeners.remove(listener);
		} else {
			toRemove.add(listener);
		}
	}

	public void fireEvent(SoarDatabaseEvent event) {
		if (suppressEvents > 0) return;
		firingEvent = true;
		for (ISoarDatabaseEventListener listener : listeners) {
			listener.onEvent(event, this);
		}
		firingEvent = false;
		for (ISoarDatabaseEventListener listener : toRemove) {
			listeners.remove(listener);
		}
		toRemove.clear();
	}
	
	public void pushSuppressEvents() {
		if (suppressEvents == 0) {
			try {
				connection.setAutoCommit(false);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		++suppressEvents;
	}
	
	public void popSuppressEvents() {
		--suppressEvents;
		if (suppressEvents == 0) {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (suppressEvents < 0) {
			new Exception("Popped suppress events too many times.").printStackTrace();
		}
	}
	
	private void test() {
		insert(Table.AGENTS, new String[][] {{"name", "\"Agent 0\""}});
	}

	/**
	 * Loads the appropriate JDBC driver ffor this environment/framework. For
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
			//System.out.println("Loaded the appropriate driver");
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
		pushSuppressEvents();
		try {
			Statement s = connection.createStatement();
			for (String filename : sqlFiles) {
				executeFile(filename, s);
			}
			s.close();
			// conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		popSuppressEvents();
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
					
					// debug
					/*
					  System.out.println("c: " + (char)c);
					  System.out.println("Write: " + write);
					  System.out.println("singleLineComment: " +
					  singleLineComment); System.out.println("multiLineComment: " +
					  multiLineComment); System.out.println("command: " +
					  builder.toString()); System.out.println();
					*/
					
					String command = builder.toString();
					builder = new StringBuilder();
					execute(command);
				}

				c = lookahead;
				lookahead = reader.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<SoarDatabaseRow> selectAllFromTable(SoarDatabaseRow.Table table, String extraSql) {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		String tableName = table.toString().toLowerCase();
		String sql= null;
		if (extraSql != null && extraSql.length() > 0) {
			sql = "select * from " + tableName + " " + extraSql;
		} else {
			sql = "select * from " + tableName;
		}
		ResultSet rs = getResultSet(sql);
		try {
			while(rs.next()) {
				SoarDatabaseRow row = new SoarDatabaseRow(table, rs.getInt("id"), this);
				ret.add(row);
			}
			rs.getStatement().close();
		} catch (SQLException e) {
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
	 * WARNING:
	 * Doesn't parameterize or sql-escape parameters.
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
			s = connection.createStatement();
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
			s = connection.createStatement();
			s.execute(sql);
			s.close();
			fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (debug) {
			System.out.println("done.");
		}
	}
	
	/**
	 * 
	 * @param commands
	 * @return Errors
	 */
	public ArrayList<String> executeBatch(String[] commands) {
		Statement s;
		ArrayList<String> errors = new ArrayList<String>();
		try {
			s = connection.createStatement();
			for (String sql : commands) {
				s.addBatch(sql);
			}
			int[] results = s.executeBatch();
			s.close();
			fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
		
			for (int i = 0; i < results.length; ++i) {
				int result = results[i];
				if (result == Statement.EXECUTE_FAILED) {
					String command = commands[i];
					errors.add("Command #" + i + " failed: " + command);
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return errors;
	}
	
	public StatementWrapper prepareStatement(String sql) {
		
		/*
		if (reusedStatements.containsKey(sql)) {
			PreparedStatement reuse = reusedStatements.get(sql);
			return new StatementWrapper(preparedStatemnt, this, sql);
		}
		*/
		
		// Keep track of frequency
		Integer num = statementFrequency.get(sql);
		if (num == null) num = 0;
		++num;
		statementFrequency.put(sql, num);
		
		try {
			PreparedStatement ps = connection.prepareStatement(sql);
			StatementWrapper ret = new StatementWrapper(ps, this, sql);
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return null;
	}

	public DatabaseMetaData getConnectionMetadata() {
		try {
			return connection.getMetaData();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public void closeConnection() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @return True if this connection is to a sqlite database file.
	 */
	public boolean isSavedToDisk() {
		return !currentPath.equals(":memory:");
	}
	
	public String getPath() {
		return currentPath;
	}
}
