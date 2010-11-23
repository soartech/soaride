package com.soartech.soar.ide.core.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import tcl.lang.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.tcl.SoarModelTclCommands;
import com.soartech.soar.ide.core.tcl.TclSpCommand;

public class SoarDatabaseUtil {

	private static RelocatableTclInterpreter makeInterp(File file, boolean countOnly, boolean soarCallbacks, ArrayList<String> comments) {
		RelocatableTclInterpreter ret = new RelocatableTclInterpreter();
		String filePath = file.getParent();
		if (filePath != null) {
			try {
				ret.setWorkingDir(filePath);
			} catch (TclException e) {
				e.printStackTrace();
			}
		}
		SoarModelTclCommands.installSoarCommands(ret, countOnly, soarCallbacks, comments);
		String tkLibrary = System.getenv("TK_LIBRARY");
		String tclLibrary = System.getenv("TCL_LIBRARY");
		try {
			if (tkLibrary != null) {
				ret.setVar("tk_library", TclString.newInstance(tkLibrary), 0);
			}
			if (tclLibrary != null) {

				ret.setVar("tcl_library", TclString.newInstance(tclLibrary), 0);
			}
		} catch (TclException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	/**
	 * Reads a text file of soar rules, creating a new database entry
	 * for each rule and placing as children of the specified row.
	 * @param file
	 * @param folder
	 * @return list of error messages
	 */
	public static ArrayList<String> importRules(File firstFile, SoarDatabaseRow agent, IProgressMonitor monitor) {
		SoarDatabaseConnection db = agent.getDatabaseConnection();
		db.pushSuppressEvents();
		ArrayList<String> comments = new ArrayList<String>();
		RelocatableTclInterpreter interp = makeInterp(firstFile, false, true, comments);
		TclSpCommand spCommand = (TclSpCommand) interp.getCommand("sp");
		SoarModelTclCommands modelCommands = (SoarModelTclCommands) interp.getCommand("watch");
		spCommand.setAgent(agent);
		spCommand.setMonitor(monitor);
		ArrayList<String> errors = importRules(firstFile, interp, comments);
		monitor.beginTask("Parsing Rules", spCommand.getNumRulesCollected());
		errors.addAll(readSpCommands(agent, spCommand, monitor));
		readAgentCommands(agent, modelCommands);
		interp.dispose();
		db.popSuppressEvents();
		db.fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
		return errors;
	}
	
	private static ArrayList<String> readSpCommands(SoarDatabaseRow agent, TclSpCommand spCommand, IProgressMonitor monitor) {
		ArrayList<String> errors = new ArrayList<String>();
		for (String ruleText : spCommand.getRules()) {
			String ruleName = getNameFromRule(ruleText);
			if (monitor != null) {
				monitor.subTask(ruleName);
			}
			SoarDatabaseRow child = agent.createChild(Table.RULES, ruleName);
			errors.addAll(child.save(ruleText, null, null));

			if (monitor != null) {
				monitor.worked(1);
			}
		}
		return errors;
	}
	
	private static void readAgentCommands(SoarDatabaseRow agent, SoarModelTclCommands modelCommands) {
		if (modelCommands.getCalls().size() > 0) {
			StringBuffer agentText = new StringBuffer();
			for (ArrayList<String> args : modelCommands.getCalls()) {
				int argsSize = args.size();
				for (int i = 0; i < argsSize; ++i) {
					agentText.append(args.get(i).toString());
					if (i + 1 < argsSize) {
						agentText.append(' ');
					}
				}
				agentText.append("\n");
			}
			modelCommands.resetCalls();
			String oldText = agent.getText();
			String newText = null;
			if (oldText.length() > 0) {
				newText = oldText + '\n';
			} else {
				newText = agentText.toString();
			}
			agent.setText(newText);
		}
	}
	
	public static ArrayList<String> importRules(File file, RelocatableTclInterpreter interp, ArrayList<String> comments) {
		TclSpCommand spCommand = (TclSpCommand) interp.getCommand("sp");
		SoarDatabaseRow agent = spCommand.getAgent();

		ArrayList<String> errors = new ArrayList<String>();
		

		ArrayList<String> newErrors = new ArrayList<String>();
		try {
			// System.out.println(file.getPath());

			// FileReader reader = new FileReader(file);
			String basePath = file.getPath();
			int lastSlashIndex = basePath.lastIndexOf(File.separatorChar);
			basePath = basePath.substring(0, lastSlashIndex);
			// System.out.println("About to read file: " + file.getPath());
			Scanner scan = new Scanner(file);

			StringBuffer buff = new StringBuffer();
			try {
				interp.setVar("AGENT_HOME", TclString.newInstance(basePath), 0);
			} catch (TclException e1) {
				e1.printStackTrace();
			}
			while (scan.hasNext()) {
				try {
					String line = scan.nextLine();
					// System.out.println(line);
					if (line.trim().startsWith("#")) {
						comments.add(line);
					} else {
						buff.append(line + '\n');
					}
					if (buff.toString().trim().length() > 0 && Interp.commandComplete(buff.toString())) {
						String command = buff.toString();
						buff = new StringBuffer();
						interp.eval(command);
					}
				} catch (TclException e) {
					System.out.println("ERROR: " + interp.getResult());
				}
			}

			scan.close();
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
			String error = "In " + file.getPath() + ": file not found at "
					+ file.getPath();
			newErrors.add(error);
		}
		for (String error : newErrors) {
			System.out.println(error);
		}
		errors.addAll(newErrors);
		
		return errors;
	}

	/*
	public static int countRulesFromFile(File firstFile, ArrayList<String> errors) {
		numRules = 0;
		RelocatableTclInterpreter interp = makeInterp(firstFile, true, false);

		try {
			String basePath = firstFile.getPath();
			int lastSlashIndex = basePath.lastIndexOf(File.separatorChar);
			basePath = basePath.substring(0, lastSlashIndex);
			System.setProperty("user.dir", basePath);
			interp.setVar("AGENT_HOME", TclString.newInstance(basePath), 0);
		} catch (TclException e) {
			System.out.println("ERROR: " + interp.getResult());
		}
		// System.out.println("About to read file: " + file.getPath());
		countRulesFromFile(firstFile, interp);
		interp.dispose();
		return numRules;
	}
	
	private static int numRules;
	
	public static void countRulesFromFile(File file, RelocatableTclInterpreter interp) {
		TclSpCommand spCommand = (TclSpCommand) interp.getCommand("sp");

		ArrayList<String> newErrors = new ArrayList<String>();
		try {
			Scanner scan = new Scanner(file);
			StringBuffer buff = new StringBuffer();
			while (scan.hasNext()) {
				try {
					String line = scan.nextLine();
					//System.out.println(line);
					if (line.trim().startsWith("#")) {
						spCommand.addComment(line);
					} else {
						buff.append(line + '\n');
					}
					if (buff.toString().trim().length() > 0
							&& Interp.commandComplete(buff.toString())) {
						String command = buff.toString();
						buff = new StringBuffer();
						interp.eval(command);
					}
				} catch (TclException e) {
					System.out.println("ERROR: " + interp.getResult());
				}
			}
			numRules += spCommand.getNumRulesCollected();
			spCommand.resetRules();
			scan.close();
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
			String error = "In " + file.getPath() + ": file not found at " + file.getPath();
			newErrors.add(error);
		}
		for (String error : newErrors) {
			System.out.println(error);
		}
		//errors.addAll(newErrors);
	}
		*/
	private static File fileForFilename(String basePath, String filename, ArrayList<String> directoryStack) {
		StringBuffer buff = new StringBuffer(basePath);
		buff.append(File.separatorChar);
		for (String dir : directoryStack) {
			buff.append(dir);
			buff.append(File.separatorChar);
		}
		buff.append(filename);
		File ret = new File(buff.toString());
		return ret;
	}
	
	public static String getNameFromRule(String rule) {
		int nameIndex = findChar('{', rule, 0);
		if (nameIndex == -1) return null;
		nameIndex = findWhitespace(rule, nameIndex + 1, true);
		int endNameIndex = findWhitespace(rule, nameIndex, false);
		if (endNameIndex == -1) return null;
		return rule.substring(nameIndex, endNameIndex);
	}
	
	/**
	 * 
	 * @param str The string to search in.
	 * @param start The starting point of the search.
	 * @param invert If true, search for non-whitespace instead of whitespace.
 	 * @return The index of the first whitespace (or non-whitespace) character
 	 * that isn't on a Soar-commented line, beginning at startIndex.
	 */
	private static int findWhitespace(String str, int startIndex, boolean invert) {
		boolean comment = false;
		for (int i = 0; i < str.length(); ++i) {
			char current = str.charAt(i);
			if (current == '#') comment = true;
			if (current == '\n') comment = false;
			if (comment) continue;
			if (invert) {
				if ((!Character.isWhitespace(current)) && i >= startIndex) {
					return i;
				}
			} else {
				if (Character.isWhitespace(current) && i >= startIndex) {
					return i;
				}
			}
		}
		
		return -1;
	}
	
	/**
	 * @param c The character to look for.
	 * @param str The string to look in.
	 * @param start The index to begin searching in (use 0 to search the whole string).
	 * @return The first index of the character <code>c</code> that doesn't occur on a Soar-commented line,
	 * or -1 if the character isn't found.
	 */
	private static int findChar(char c, String str, int start) {
		
		boolean comment = false;
		for (int i = 0; i < str.length(); ++i) {
			char current = str.charAt(i);
			if (current == '#') comment = true;
			if (current == '\n') comment = false;
			if (comment) continue;
			if (current == c && i >= start) {
				return i;
			}
		}
		
		return -1;
	}
	
	public static void alertError(String error, String filename, int line) {
		System.out.println("Error: " + error + ", " + filename + ":" + line);
	}
	
	public static void transferDatabase(final SoarDatabaseConnection from, final SoarDatabaseConnection to) {
		final boolean debug = false;
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		
		to.pushSuppressEvents();
		
		try {
			new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						DatabaseMetaData md = from.getConnectionMetadata();
						ResultSet tables = md.getTables(null, null, "%", null); // return
																				// all
																				// tables
						int numTables = 0;
						while (tables.next()) {
							++numTables;
						}
						
						monitor.beginTask("Saving project", numTables);
						tables = md.getTables(null, null, "%", null);
						while (tables.next()) {
							String tableName = tables.getString("TABLE_NAME");
							String tableType = tables.getString("TABLE_TYPE");

							if (tableType.equalsIgnoreCase("TABLE")) {
								String sql = "select * from " + tableName;
								ResultSet rs = from.getResultSet(sql);
								ResultSetMetaData rsmd = rs.getMetaData();
								ArrayList<String> columnNames = new ArrayList<String>();
								ArrayList<Integer> columnTypes = new ArrayList<Integer>();
								int numColumns = rsmd.getColumnCount();
								for (int column = 1; column <= numColumns; ++column) {
									String columnName = rsmd.getColumnName(column);
									int columnType = rsmd.getColumnType(column);
									String columnTypeName = rsmd.getColumnTypeName(column);
									if (debug)
										System.out.println(columnName + " (" + columnType + ", " + columnTypeName + ")");
									columnTypes.add(new Integer(columnType));
									columnNames.add(columnName);
								}

								StringBuffer buff = new StringBuffer();
								buff.append("insert into " + tableName + " (");
								for (int i = 0; i < numColumns; ++i) {
									buff.append(columnNames.get(i));
									if (i + 1 < columnNames.size()) {
										buff.append(",");
									}
								}
								buff.append(") values (");
								for (int i = 0; i < numColumns; ++i) {
									buff.append("?");
									if (i + 1 < numColumns) {
										buff.append(",");
									}
								}
								buff.append(")");
								PreparedStatement ps = to.getConnection().prepareStatement(buff.toString());

								boolean execute = false;

								while (rs.next()) {
									execute = true;
									for (int i = 0; i < numColumns; ++i) {
										int columnIndex = i + 1;
										int columnType = columnTypes.get(i);
										if (columnType == Types.INTEGER) {
											ps.setInt(columnIndex, rs.getInt(columnIndex));
										} else if (columnType == Types.VARCHAR) {
											ps.setString(columnIndex, rs.getString(columnIndex));
										} else if (columnType == Types.DOUBLE) {
											ps.setDouble(columnIndex, rs.getDouble(columnIndex));
										} else if (columnType == Types.FLOAT) {
											ps.setFloat(columnIndex, rs.getFloat(columnIndex));
										} else if (columnType == Types.NULL) {
											ps.setString(columnIndex, rs.getString(columnIndex));
										} else {
											System.out.println("Unknown type: " + columnType);
										}
									}
									ps.addBatch();
								}

								if (execute) {
									System.out.println("Executing: " + buff.toString());
									ps.executeBatch();
								}

								/*
								 * ArrayList<String> columnTypeNames = new
								 * ArrayList<String>(); for (int column = 0;
								 * column < columnNames.size(); ++column) {
								 * String columnName = columnNames.get(column);
								 * String type = null; // figure out type from
								 * name of field. if
								 * (columnName.equalsIgnoreCase("id")) { type =
								 * "integer primary key"; } else if
								 * (columnName.toLowerCase().endsWith("_id")) {
								 * type = "integer"; } else if
								 * (columnName.toLowerCase().startsWith("has_")
								 * ||
								 * columnName.toLowerCase().startsWith("is_")) {
								 * type = "boolean"; } else if
								 * (columnName.equalsIgnoreCase("raw_text")) {
								 * type = "text"; } else { type =
								 * "varchar(100)"; } columnTypeNames.add(type);
								 * }
								 */

							} else {
								System.out.println("No table");
							}
							monitor.worked(1);
						}
						monitor.done();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}

			});
		} catch (InvocationTargetException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		to.popSuppressEvents();
		to.fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
	}

	/**
	 * This may cause problems if text in rules or item names contains sql characters
	 * -- this method might make the database vulnerable to SQL injection problems.
	 * @param conn
	 * @return
	 */
	public static String sqlDump(SoarDatabaseConnection conn) {

		final boolean debug = false;

		StringBuffer buff = new StringBuffer();
		try {
			DatabaseMetaData md = conn.getConnectionMetadata();
			ResultSet tables = md.getTables(null, null, "%", null); // return all tables
			String[] tableColumns = { "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS", "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION" };
			while (tables.next()) {
				if (debug) {
					System.out.println("\n***NEW TABLE***\n");
				
				  for (String tableColumn : tableColumns) { String value =
				  tables.getString(tableColumn); System.out.println(tableColumn
				  + ": " + value); }
				}
				 

				String tableName = tables.getString("TABLE_NAME");
				String tableType = tables.getString("TABLE_TYPE");

				if (tableType.equalsIgnoreCase("TABLE")) {
					if (debug)
						System.out.println(tableName);
					String sql = "select * from " + tableName;
					ResultSet rs = conn.getResultSet(sql);
					ResultSetMetaData rsmd = rs.getMetaData();
					ArrayList<String> columnNames = new ArrayList<String>();
					ArrayList<Integer> columnTypes = new ArrayList<Integer>();
					for (int column = 1; column <= rsmd.getColumnCount(); ++column) {
						String columnName = rsmd.getColumnName(column);
						int columnType = rsmd.getColumnType(column);
						String columnTypeName = rsmd.getColumnTypeName(column);
						if (debug)
							System.out.println(columnName + " (" + columnType + ", " + columnTypeName + ")");
						columnTypes.add(new Integer(columnType));
						columnNames.add(columnName);
					}
					
					ArrayList<String> columnTypeNames = new ArrayList<String>();
					for (int column = 0; column < columnNames.size(); ++column) {
						String columnName = columnNames.get(column);
						String type = null;
						// figure out type from name of field.
						if (columnName.equalsIgnoreCase("id")) {
							type = "integer primary key";
						} else if (columnName.toLowerCase().endsWith("_id")) {
							type = "integer";
						} else if (columnName.toLowerCase().startsWith("has_") || columnName.toLowerCase().startsWith("is_")) {
							type = "boolean";
						} else if (columnName.equalsIgnoreCase("raw_text")) {
							type = "text";
						} else {
							type = "varchar(100)";
						}
						/*
						 * buff.append(columnName + " " + type); if (column + 1
						 * < columnNames.size()) { buff.append(",\n"); } else {
						 * buff.append("\n"); }
						 */
						columnTypeNames.add(type);
					}
					// buff.append(");\n");

					while (rs.next()) {
						buff.append("insert into " + tableName + " (");
						for (int i = 0; i < columnNames.size(); ++i) {
							buff.append(columnNames.get(i));
							if (i + 1 < columnNames.size()) {
								buff.append(",");
							}
						}
						buff.append(") values (");
						if (debug)
							System.out.println();
						for (int i = 0; i < columnNames.size(); ++i) {
							String columnName = columnNames.get(i);
							Object value = rs.getObject(columnName);
							if (debug)
								System.out.println(columnName + ": " + (value == null ? "NULL" : value + " (" + value.getClass().getSimpleName()) + ")");
							String valueSql = (value == null ? "NULL" : "" + value);
							if (value != null && (columnTypeNames.get(i).equalsIgnoreCase("text") || columnTypeNames.get(i).startsWith("varchar"))) {
								buff.append("\"" + valueSql + "\"");
							} else {
								buff.append("" + valueSql);
							}
							if (i + 1 < columnNames.size()) {
								buff.append(",");
							}
						}
						buff.append(");\n");
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return buff.toString();
	}
	
	/**
	 * Sorts the items alphabetically and returns the sorted list.
	 * This calls Collections.sort, which will change the list passed in.
	 * @param list
	 * @return
	 */
	public static ArrayList<? extends ISoarDatabaseTreeItem> sortRowsByName(ArrayList<? extends ISoarDatabaseTreeItem> list) {
		Collections.sort(list, new Comparator<ISoarDatabaseTreeItem>() {

			@Override
			public int compare(ISoarDatabaseTreeItem first, ISoarDatabaseTreeItem second) {
				String firstString = (first instanceof SoarDatabaseRow ? ((SoarDatabaseRow)first).getName() : first.toString());
				String secondString = (second instanceof SoarDatabaseRow ? ((SoarDatabaseRow)second).getName() : second.toString());
				return firstString.compareTo(secondString);
			}
		
		});
		return list;
	}
}
