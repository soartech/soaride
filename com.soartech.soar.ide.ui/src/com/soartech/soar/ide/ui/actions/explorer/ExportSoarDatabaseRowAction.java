package com.soartech.soar.ide.ui.actions.explorer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.sql.TraversalUtil;

import edu.umich.soar.debugger.jmx.SoarCommandLineMXBean;

public class ExportSoarDatabaseRowAction extends Action {
	
	SoarDatabaseRow row;
	SoarCommandLineMXBean proxy = null;
	
	public ExportSoarDatabaseRowAction(SoarDatabaseRow row) {
		super("Export to File...");
		this.row = row;
	}
	
	public ExportSoarDatabaseRowAction(SoarDatabaseRow row, SoarCommandLineMXBean proxy) {
		super("Export to Soar Debugger");
		this.row = row;
		this.proxy = proxy;
	}
	@Override
	public void run() {
		if (proxy != null) {
			runDebugger();
		} else {
			runFile();
		}
	}
	
	private void runDebugger() {
		Writer writer = new Writer() {
			
			@Override
			public void close() throws IOException {
			}

			@Override
			public void flush() throws IOException {
			}

			@Override
			public void write(char[] chars, int off, int len) throws IOException {
				String str = new String(chars, off, len);
				proxy.executeCommandLine(str);
			}
			
		};
		writeToOutput(writer);
	}
	
	private void runFile() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setText("Export Agent");
		String path = dialog.open();
		File file = new File(path);
		boolean write = true;
		if (file.exists()) {
			String title = "Overwrite?";
			String message = "The file at " + path + " already exists. Overwrite?";
			write = MessageDialog.openQuestion(shell, title, message);
		}
		if (write) {
			try {
				FileWriter writer = new FileWriter(file);
				writeToOutput(writer);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
	
	private void writeToOutput(Writer writer) {
		try {
			ArrayList<ISoarDatabaseTreeItem> children = TraversalUtil.getRelatedRules(row);
			//writer.write("# Begin export of " + row.getTable().englishName() + " \"" + row.getName() + "\"\n");

			if (row.getTable() == Table.AGENTS) {
				String agentText = row.getText();
				if (agentText.length() > 0) {
					//writer.write("# Begin Soar commands for agent \"" + row.getName() + "\"\n");
					writer.write(agentText);
					//writer.write("\n# End Soar commands for agent \"" + row.getName() + "\"\n");
				}
			}

			for (ISoarDatabaseTreeItem child : children) {
				assert child instanceof SoarDatabaseRow;
				SoarDatabaseRow childRow = (SoarDatabaseRow) child;
				assert childRow.getTable() == Table.RULES;
				//writer.write("# Begin rule \"" + childRow.getName() + "\"\n");
				writer.write(childRow.getText());
				//writer.write("\n# End rule \"" + childRow.getName() + "\"\n");
			}
			//writer.write("# End export of " + row.getTable().englishName() + " \"" + agentName + "\"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
