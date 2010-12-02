package com.soartech.soar.ide.ui.editors.text;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.SoarProblem;
import com.soartech.soar.ide.core.sql.ISoarDatabaseEventListener;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;

public class SoarDatabaseTextEditor extends AbstractSoarDatabaseTextEditor implements ISoarDatabaseTextEditor, ISoarDatabaseEventListener {

	public static final String ID = "com.soartech.soar.ide.ui.editors.text.SoarDatabaseTextEditor";
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
		doSoarSave(progressMonitor, false);
	}
	
	public void markErrors() {
		doSoarSave(null, true);
	}
	
	private void doSoarSave(IProgressMonitor progressMonitor, final boolean forceSave) {
		if (input != null) {
			input.clearProblems();
			clearAnnotations();
			final SoarDatabaseRow row = input.getSoarDatabaseStorage().getRow();
			final IDocument doc = getDocumentProvider().getDocument(input);
			try {
				new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()).run(true, true, new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						ArrayList<SoarProblem> problems = new ArrayList<SoarProblem>();
						row.save(doc, problems, monitor, forceSave);
						for (SoarProblem problem : problems) {
							input.addProblem(problem);
						}
					}
				});
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ArrayList<SoarProblem> problems = input.getProblems();
			for (SoarProblem problem : problems) {
				SoarDatabaseTextAnnotation annotation = new SoarDatabaseTextAnnotation();
				Position position = new Position(problem.start, problem.length);
				addAnnotation(annotation, position);
			}
			getVerticalRuler().update();
		}
	}
	
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (input != null) {
			this.input.getRow().getDatabaseConnection().addListener(this);
		}
	}
	
	@Override
	public void onEvent(SoarDatabaseEvent event, SoarDatabaseConnection db) {
		if (event.type == Type.DATABASE_CHANGED) {
			if (event.row != null && event.row.equals(this.input.getRow())) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						SoarDatabaseTextEditor.this.doRevertToSaved();
					}
				});
			}
		}
	}
}
