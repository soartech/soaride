package com.soartech.soar.ide.ui.actions.explorer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal.TraversalUtil;

public class ExportSoarDatabaseRowAction extends Action {
	
	SoarDatabaseRow row;
	
	public ExportSoarDatabaseRowAction(SoarDatabaseRow row) {
		super("Export " + row.getName());
		this.row = row;
	}
	
	@Override
	public void run() {
		super.run();
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
				ArrayList<ISoarDatabaseTreeItem> children = TraversalUtil.getRelatedRules(row);
				String agentName = row.getName();
				writer.write("# Begin export of " + row.getTable().englishName() + " \"" + agentName + "\"\n");
				
				if (row.getTable() == Table.AGENTS) {
					String agentText = row.getText();
					if (agentText.length() > 0) {
						writer.write("# Begin Soar commands for agent \"" + row.getName() +"\"\n");
						writer.write(agentText);
						writer.write("\n# End Soar commands for agent \"" + row.getName() +"\"\n");
					}
				}
				
				for (ISoarDatabaseTreeItem child : children) {
					assert child instanceof SoarDatabaseRow;
					SoarDatabaseRow childRow = (SoarDatabaseRow) child;
					assert childRow.getTable() == Table.RULES;
					String ruleText = childRow.getText();
					String ruleName = childRow.getName();
					writer.write("# Begin rule \"" + ruleName + "\"\n");
					writer.write(ruleText);
					writer.write("\n# End rule \"" + ruleName + "\"\n");
				}
				writer.write("# End export of " + row.getTable().englishName() + " \"" + agentName + "\"\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
