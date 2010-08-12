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
import java.util.Scanner;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import sun.net.ProgressMonitor;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseUtil {
	
	/**
	 * Reads a text file of soar rules, creating a new database entry
	 * for each rule and placing as children of the specified row.
	 * @param file
	 * @param folder
	 * @return list of error messages
	 */
	public static ArrayList<String> importRules(File firstFile, SoarDatabaseRow agent, IProgressMonitor monitor) {

		ArrayList<String> errors = new ArrayList<String>();
		
		// The stack of pushed and popped directories.
		ArrayList<String> directoryStack = new ArrayList<String>();
		
		// This is the list of files to read.
		ArrayList<File> files = new ArrayList<File>(); 
		files.add(firstFile);

		// This is the list of files that have been read, to avoid recursive souce commands.
		//ArrayList<File> readFiles = new ArrayList<File>();
		
		ArrayList<String> agentLines = new ArrayList<String>();
		
		for (int filesIndex = 0; filesIndex < files.size(); ++filesIndex) {
			ArrayList<String> newErrors = new ArrayList<String>();
			File file = files.get(filesIndex);
			try {
				//System.out.println(file.getPath());

				//FileReader reader = new FileReader(file);
				String basePath = file.getPath();
				int lastSlashIndex = basePath.lastIndexOf(File.separatorChar);
				basePath = basePath.substring(0, lastSlashIndex);
				int lineNumber = 0;
				//System.out.println("About to read file: " + file.getPath());
				Scanner scan = new Scanner(file);
				ArrayList<String> lines = new ArrayList<String>();
				while (scan.hasNext()) {
					++lineNumber;
					String line = scan.nextLine();
					String trimmedLine = line.trim();
					if (trimmedLine.startsWith("pushd ") && trimmedLine.length() > 6) {
						//String[] tokens = trimmedLine.split("\\s");
						//directoryStack.add(tokens[1]);						
						String pushdPath = trimmedLine.substring(6);
						if (pushdPath.startsWith("\"")) pushdPath = pushdPath.substring(1);
						if (pushdPath.endsWith("\"")) pushdPath = pushdPath.substring(0, pushdPath.length() - 1);
						directoryStack.add(pushdPath);
					} else if (trimmedLine.startsWith("popd")) {
						directoryStack.remove(directoryStack.size() - 1);
					} else if (trimmedLine.startsWith("source ")) {
						String source = trimmedLine.substring(7);
						File newFile = fileForFilename(basePath, source, directoryStack);
						if (!newFile.exists()) {
							String error = "File not found:" + newFile.getPath();
							System.out.println(error);
							errors.add(error);
						} else {
							files.add(newFile);
						}
					} else if (trimmedLine.startsWith("sp ") || trimmedLine.startsWith("sp{")) {
						ArrayList<String> ruleLines = new ArrayList<String>();
						int braceDepth = 0;
						boolean string = false;
						boolean hasBraces = false;
						boolean finished = false;
						while (true) {
							ruleLines.add(line);
							char[] chars = line.toCharArray();
							char cc = '\0';
							for (char c : chars) {
								if (!string) {
									if (c == '{') {
										++braceDepth;
										hasBraces = true;
									}
									else if (c == '}') --braceDepth;
									else if (c == '|') string = true;
									else if (c == '#') break; // Comment, ignore the rest of the line
								} else {
									if (c == '|' && cc != '\\') string = false;
								}

								cc = c;
							}
							
							if (hasBraces && braceDepth <= 0) {
								finished = true;
							}
							
							if (finished || !scan.hasNextLine()) break;
							line = scan.nextLine();
						}
						
						if (finished) {
							// This was a rule.
							// Find preceding comment lines and use them and 'ruleLines' to create a new rule.
							
							int firstCommentLine = lines.size();
							for ( ; firstCommentLine > 0; --firstCommentLine) {
								String commentLine = lines.get(firstCommentLine - 1);
								if (commentLine.length() > 0 && !commentLine.trim().startsWith("#")) {
									break;
								}
							}
							
							for ( ; firstCommentLine < lines.size(); ++firstCommentLine) {
								if (lines.get(firstCommentLine).length() > 0) break;
							}
							
							ArrayList<String> finalRuleLines = new ArrayList<String>();
							
							for (int i = firstCommentLine; i < lines.size(); ++i) {
								finalRuleLines.add(lines.get(i));
							}
							finalRuleLines.addAll(ruleLines);
							
							for (int i = lines.size() - 1; i >= firstCommentLine; --i) {
								lines.remove(i);
							}
							
							StringBuffer ruleBuffer = new StringBuffer();
							for (String ruleLine : finalRuleLines) {
								ruleBuffer.append(ruleLine + '\n');
							}
							
							String ruleString = ruleBuffer.toString();
							String ruleName = getNameFromRule(ruleString);
							SoarDatabaseRow child = agent.createChild(Table.RULES, ruleName);
							errors.addAll(child.save(ruleString, null));
							
							if (monitor != null) {
								monitor.subTask("Imported rule: " + ruleName);
							}
							
						} else {
							// Oops, this wasn't really a rule
							// Add collected lines to 'lines'
							lines.addAll(ruleLines);
						}
						
					} else {
						lines.add(line);
					}
				}
				scan.close();
				agentLines.addAll(lines);
			} catch (FileNotFoundException e) {
				//e.printStackTrace();
				String error = "In " + file.getPath() + ": file not found at " + files.get(filesIndex).getPath();
				newErrors.add(error);
			}
			for (String error : newErrors) {
				System.out.println(error);
			}
			errors.addAll(newErrors);
		}
		
		// Add commands to agent
		StringBuffer agentText = new StringBuffer();
		for (String command : agentLines) {
			agentText.append(command + '\n');
		}
		
		agent.setText(agentText.toString(), true);
		
		return errors;
	}
		
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
		try {
			new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// TODO Auto-generated method stub
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
								System.out.println("No ttable");
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
}
