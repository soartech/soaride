package com.soartech.soar.ide.ui.actions.explorer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class ExportProjectActionDelegate implements IViewActionDelegate {

	StructuredSelection ss;
	SoarDatabaseRow selectedRow;
	
	@Override
	public void init(IViewPart view) {
		
	}

	@Override
	public void run(IAction action) {
		if (!action.isEnabled()) {
			return;
		}
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
				ArrayList<ISoarDatabaseTreeItem> children = selectedRow.getChildrenOfType(Table.RULES);
				String agentName = selectedRow.getName();
				writer.write("# Begin agent \"" + agentName + "\"\n\n");
				for (ISoarDatabaseTreeItem child : children) {
					assert child instanceof SoarDatabaseRow;
					SoarDatabaseRow childRow = (SoarDatabaseRow) child;
					assert childRow.getTable() == Table.RULES;
					String ruleText = childRow.getText();
					String ruleName = childRow.getName();
					writer.write("# Begin rule \"" + ruleName + "\"\n\n");
					writer.write(ruleText);
					writer.write("\n\n# End rule \"" + ruleName + "\"\n");
				}
				writer.write("# End agent \"" + agentName + "\"\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {		
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			ss = (StructuredSelection) selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseRow) {
				selectedRow = (SoarDatabaseRow) obj;
				if (selectedRow.getTable() == Table.AGENTS) {
					action.setEnabled(true);
				}
			}
		}
	}

}
