package com.soartech.soar.ide.core.sql;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.sun.imageio.plugins.common.InputStreamAdapter;

public class SoarDatabaseUtil {
	
	/**
	 * Reads a text file of soar rules, creating a new database entry
	 * for each rule and placing as chidlren of the specified row.
	 * @param file
	 * @param folder
	 */
	public static ArrayList<SoarDatabaseRow> importRules(File file, SoarDatabaseRow row) {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		try {
			FileReader reader = new FileReader(file);

			// The current character
			char c;
			
			// The previous character
			char last = ' ';
			 
			StringBuffer buffer = new StringBuffer();
			
			int bracesDepth = 0;
			
			boolean insideProduction = false;
			boolean comment = false;
			int line = 1;
			String error;
			int i = 0;
			
			while ((i = reader.read()) != -1) {
				c = (char) i;
				buffer.append(c);
				error = null;

				if (c == '\n') {
					++line;
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
				
				if (c == '{') {
					// If we're not reading a rule, see if the last characters in the buffer
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
				
				if (error != null) {
					alertError(error, file.getName(), line);
				}
				last = c;
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static SoarDatabaseRow importRule(String rule, SoarDatabaseRow row) {
		// Get the name of the rule
		int nameIndex = rule.indexOf('{') + 1;
		int parenIndex = rule.indexOf('(');
		String name = rule.substring(nameIndex, parenIndex).trim();
		SoarDatabaseRow child = row.createChild(Table.RULES, name);
		child.setText(rule);
		child.save(rule, null);
		return child;
	}
	
	public static void alertError(String error, String filename, int line) {
		System.out.println("Error: " + error + ", " + filename + ":" + line);
	}
}
