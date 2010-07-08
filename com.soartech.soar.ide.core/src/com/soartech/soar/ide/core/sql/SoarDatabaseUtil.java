package com.soartech.soar.ide.core.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseUtil {
	
	/**
	 * Reads a text file of soar rules, creating a new database entry
	 * for each rule and placing as chidlren of the specified row.
	 * @param file
	 * @param folder
	 */
	public static ArrayList<SoarDatabaseRow> importRules(File firstFile, SoarDatabaseRow row) {
		
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		
		// The stack of pushed and popped directories.
		ArrayList<String> directoryStack = new ArrayList<String>();
		
		// This is the list of files to read.
		ArrayList<File> files = new ArrayList<File>(); 
		files.add(firstFile);

		// This is the list of files that have been read, to avoid recursive souce commands.
		ArrayList<File> readFiles = new ArrayList<File>();

		for (int filesIndex = 0; filesIndex < files.size(); ++filesIndex) {
			try {
				File file = files.get(filesIndex);
				System.out.println(file.getPath());

				FileReader reader = new FileReader(file);
				String basePath = file.getPath();
				int lastSlashIndex = basePath.lastIndexOf(File.separatorChar);
				basePath = basePath.substring(0, lastSlashIndex);

				// The current character
				char c;

				// The previous character
				char last = ' ';

				StringBuffer buffer = new StringBuffer();

				int bracesDepth = 0;

				boolean insideProduction = false;
				boolean comment = false;
				int lineNumber = 1;
				String error;
				int i = 0;

				while ((i = reader.read()) != -1) {
					
					c = (char) i;
					buffer.append(c);
					error = null;
					
					System.out.print(c);

					if (c == '\n') {
						++lineNumber;
					}
					if (c == '#') {
						comment = true;
					}
					if (comment) {
						if (c == '\n') {
							comment = false;
						}
						continue;
					}

					// look for rules

					if (c == '{') {
						// If we're not reading a rule, see if the last
						// characters in the buffer
						// are like "sp {"
						if (!insideProduction && bracesDepth == 0) {
							String lastBuffer = buffer.substring(0, buffer.length() - 1).trim();
							if (lastBuffer.endsWith("sp")) {
								insideProduction = true;
							}
						}

						++bracesDepth;
					}
					if (c == '}') {
						--bracesDepth;

						if (bracesDepth < 0) {
							error = "Too many closed braces";
						}
						if (bracesDepth == 0 && insideProduction) {
							insideProduction = false;
							SoarDatabaseRow newRow = importRule(buffer.toString().trim(), row);
							ret.add(newRow);
							buffer = new StringBuffer();
						}
					}

					// look for other commands
					// pushd, popd, source
					// These are newline-delimited
					if (c == '\n') {
						String line = buffer.toString().trim();
						String[] tokens = line.split("\\s"); // "\s" for whitespace

						if (tokens.length > 0) {
							boolean consumed = true;
							if (tokens[0].equalsIgnoreCase("pushd") && tokens.length > 1) {
								directoryStack.add(tokens[1]);
							} else if (tokens[0].equalsIgnoreCase("popd")) {
								directoryStack.remove(directoryStack.size() - 1);
							} else if (tokens[0].equalsIgnoreCase("source") && tokens.length > 1) {
								File newFile = fileForFilename(basePath, tokens[1], directoryStack);
								files.add(newFile);
							} else {
								consumed = false;
							}
							if (consumed) {
								buffer = new StringBuffer();
							}
						}
					}

					if (error != null) {
						alertError(error, file.getName(), lineNumber);
					}
					last = c;
				}
				reader.close();
			} catch (FileNotFoundException e) {
				//e.printStackTrace();
				System.out.println("FILENOTFOUND");
			} catch (IOException e) {
				//e.printStackTrace();
				System.out.println("FILENOTFOUND");
			}
		}
		return ret;
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
	
	public static SoarDatabaseRow importRule(String rule, SoarDatabaseRow row) {
		// Get the name of the rule
		String name = getNameFromRule(rule);
		SoarDatabaseRow child = row.createChild(Table.RULES, name);
		child.setText(rule, true);
		child.save(rule, null);
		return child;
	}
	
	private static String getNameFromRule(String rule) {
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

	public static String sqlDump(SoarDatabaseConnection conn) {

		final boolean debug = false;

		StringBuffer buff = new StringBuffer();
		try {
			DatabaseMetaData md = conn.getConnectionMetadata();
			ResultSet tables = md.getTables(null, null, "%", null); // return
																	// all
																	// tables
			String[] tableColumns = { "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS", "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION" };
			while (tables.next()) {
				if (debug)
					System.out.println("\n***NEW TABLE***\n");
				/*
				 * for (String tableColumn : tableColumns) { String value =
				 * tables.getString(tableColumn); System.out.println(tableColumn
				 * + ": " + value); }
				 */

				String tableName = tables.getString("TABLE_NAME");
				String tableType = tables.getString("TABLE_TYPE");

				if (tableType.equalsIgnoreCase("TABLE")) {
					if (debug || true)
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

					// Don't need to do this now that
					// SoarDatabaseConnection.loadDatabaseConnection()
					// handles building the schema.
					// buff.append("drop table if exists " + tableName + ";\n");
					// buff.append("create table if not exists " + tableName +
					// " (\n");
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
