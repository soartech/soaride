package com.soartech.soar.ide.ui.actions.explorer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.Triple;

public class GenerateDatamapFromVisualSoarFileAction extends Action {

	SoarDatabaseRow problemSpace;
	
	public GenerateDatamapFromVisualSoarFileAction(SoarDatabaseRow problemSpace) {
		super("Generate Datamap From Visual Soar Datamap...");
		this.problemSpace = problemSpace;
	}
	
	@Override
	public void run() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		String path = dialog.open();
		if (path == null || path.length() == 0) return;
		File file = new File(path);
		if (!file.exists()) {
			ErrorDialog error = new ErrorDialog(shell,
					"File Not Found",
					"The file was not found at " + path,
					Status.OK_STATUS,
					SWT.NONE);
			error.open();
		}
		String commentPath = path.substring(0, path.lastIndexOf(File.separatorChar) + 1) + "comment.dm";
		File commentFile = new File(commentPath);
		ArrayList<String> comments = new ArrayList<String>();
		if (commentFile.exists()) {
			Scanner s = null;
			try {
				s = new Scanner(commentFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			while(s.hasNextLine()) {
				String line = s.nextLine();
				if (line.startsWith("0 ")) {
					String comment = line.substring(2);
					comments.add(comment);
				}
			}
		}
		Scanner s = null;
		try {
			s = new Scanner(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		StringBuffer buff = new StringBuffer();
		int section = 0;
		int commentIndex = 0;
		HashMap<String, String> idToType = new HashMap<String, String>();
		HashMap<String, String> idToLine = new HashMap<String, String>();
		HashMap<String, ArrayList<String>> enumIdToListOfValues = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<Triple>> triplesWithValueVariable = new HashMap<String, ArrayList<Triple>>();
		ArrayList<Triple> triples = new ArrayList<Triple>();
		try {
			while (s.hasNextLine()) {
				String line = s.nextLine();
				buff = new StringBuffer();
				if (Pattern.matches("^\\d+$", line)) {
					++section;
				} else if (section == 1) {
					// Declaring items
					String[] tokens = line.split(" ");
					if (tokens.length < 2)
						continue;
					String type = tokens[0];
					String id = tokens[1];
					idToType.put(id, type);
					idToLine.put(id, line);
					if (type.equals("ENUMERATION")) {
						ArrayList<String> values = new ArrayList<String>();
						for (int i = 3; i < tokens.length; ++i) {
							values.add(tokens[i]);
						}
						enumIdToListOfValues.put(id, values);
					}
				} else if (section == 2) {
					// Declaring joins
					String[] tokens = line.split(" ");
					if (tokens.length < 3)
						continue;
					String variable = "<" + tokens[0] + ">";
					String attribute = tokens[1];
					String valueId = tokens[2];
					String valueType = idToType.get(valueId);

					if (valueType.equals("ENUMERATION")) {
						ArrayList<String> enumValues = enumIdToListOfValues.get(valueId);
						for (String enumValue : enumValues) {
							Triple t = new Triple(variable, attribute, enumValue, null);
							if (commentIndex < comments.size())
								t.comment = comments.get(commentIndex);
							triples.add(t);
						}
					} else if (valueType.equals("INTEGER_RANGE") || (valueType.equals("FLOAT_RANGE"))) {
						String[] idTokens = idToLine.get(valueId).split(" ");
						for (int i = 2; i < idTokens.length; ++i) {
							Triple t = new Triple(variable, attribute, idTokens[i], null);
							if (commentIndex < comments.size())
								t.comment = comments.get(commentIndex);
							triples.add(t);
						}
					} else if (valueType.equals("STRING")) {
						Triple t = new Triple(variable, attribute, Triple.STRING_VALUE, null);
						if (commentIndex < comments.size())
							t.comment = comments.get(commentIndex);
						triples.add(t);
					} else { // valueType.equals("SOAR_ID")
						String value = "<" + valueId + ">";
						Triple t = new Triple(variable, attribute, value, null);
						if (commentIndex < comments.size())
							t.comment = comments.get(commentIndex);
						triples.add(t);
						ArrayList<Triple> triplesWithVaraible = triplesWithValueVariable.get(value);
						if (triplesWithVaraible == null) triplesWithVaraible = new ArrayList<Triple>();
						triplesWithVaraible.add(t);
						triplesWithValueVariable.put(value, triplesWithVaraible);
					}
					++commentIndex;
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Set parent triples
		for (Triple triple : triples) {
			String variable = triple.variable;
			ArrayList<Triple> parentTriples = triplesWithValueVariable.get(variable);
			if (parentTriples == null) parentTriples = new ArrayList<Triple>();
			triple.parentTriples = parentTriples;
			
			// Also set hasState
			if (triple.variable.equals("<0>")) {
				triple.hasState = true;
			}
		}
		
		GenerateDatamapAction.runWithProblemSpaceForTriples(problemSpace, triples, false);
	}
}
