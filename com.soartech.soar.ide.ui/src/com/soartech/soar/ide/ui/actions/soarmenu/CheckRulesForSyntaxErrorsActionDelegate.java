package com.soartech.soar.ide.ui.actions.soarmenu;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.SoarProblem;
import com.soartech.soar.ide.core.ast.ParseException;
import com.soartech.soar.ide.core.ast.SoarParser;
import com.soartech.soar.ide.core.ast.SoarProductionAst;
import com.soartech.soar.ide.core.ast.Token;
import com.soartech.soar.ide.core.ast.TokenMgrError;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.sql.datamap.DatamapInconsistency;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.views.search.SoarDatabaseSearchResultsView;

public class CheckRulesForSyntaxErrorsActionDelegate implements
		IWorkbenchWindowActionDelegate {

	@Override
	public void run(IAction action) {
		final SoarDatabaseRow agent = SoarUiModelTools.selectAgent();
		if (agent == null) return;
		//final ArrayList<String> errors = new ArrayList<String>();
		final ArrayList<DatamapInconsistency> problems = new ArrayList<DatamapInconsistency>();

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		try {
			new ProgressMonitorDialog(shell).run(true, false,
					new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							ArrayList<SoarDatabaseRow> rules = agent.getChildrenOfType(Table.RULES);
							monitor.beginTask("Checking rules for syntax errors",rules.size());
							for (SoarDatabaseRow rule : rules) {
								String ruleName = rule.getName();
								monitor.subTask(ruleName);
								String text = rule.getText();
								text = SoarDatabaseRow.removeComments(text);
								String trimmed = text.trim();
								boolean error = false;
								if (trimmed.length() > 0) {
									int beginIndex = 0;
									int endIndex = 0;
									if (!trimmed.startsWith("sp") && !trimmed.startsWith("gp")) {
										error = true;
										//errors.add("" + rule.getName() + ": Rule doesn't contain sp or gp production.");
									} else {
										beginIndex = text.indexOf("sp") + 2;
										if (beginIndex == 1) beginIndex = text.indexOf("gp") + 2;
										trimmed = text.substring(beginIndex) .trim();
										if (!trimmed.startsWith("{")) {
											error = true;
											//errors.add("" + rule.getName() + ": Production doesn't begin with \"sp {\" or \"gp {\"");
										} else {
											beginIndex = text.indexOf("{", beginIndex) + 1;
											trimmed = text.substring(beginIndex).trim();
											if (!trimmed.endsWith("}")) {
												error = true;
												//errors.add("" + rule.getName() + ": Production doesn't end with \"}\"");
											} else {
												endIndex = text.lastIndexOf("}");
											}
										}
									}
									// </hacky>

									if (monitor != null) {
										monitor.worked(1);
									}

									if (!error) {
										String parseText = text.substring(beginIndex, endIndex);
										StringReader reader = new StringReader(parseText);
										SoarParser parser = new SoarParser(reader);
										try {
											SoarProductionAst ast = parser.soarProduction();
											// System.out.println("Parsed rule:\n"
											// + ast);

											// insert into database
											/*
											 * try {
											 * createChildrenFromAstNode(ast); }
											 * catch (Exception e) {
											 * e.printStackTrace(); }
											 */
										} catch (ParseException e) {
											// e.printStackTrace();
											String message = e.getLocalizedMessage();
											Token currentToken = e.currentToken;
											Token errorToken = currentToken.next;

											// Get the range of the error, based
											// on the string
											// being parsed and the given column
											// and row
											/*
											int start = 0;
											for (int i = 1; i < errorToken.beginLine;) {
												char c = parseText.charAt(start);
												if (c == '\n') {
													++i;
												}
												++start;
											}
											

											start += beginIndex;
											// -1 for columns counting starting
											// with 1
											start += errorToken.beginColumn - 1;
											*/

											//int length = 1;
											problems.add(new DatamapInconsistency(rule, message, errorToken.beginLine));
											//errors.add(ruleName + ", " + errorToken.beginLine + ": " + message);
										} catch (TokenMgrError e) {
											//e.printStackTrace();
											problems.add(new DatamapInconsistency(rule, e.getLocalizedMessage(), -1));
											/*
											 * String message =
											 * e.getLocalizedMessage();
											 * 
											 * // Get the range of the error,
											 * based on the string // being
											 * parsed and the given column and
											 * row int start = 0; for (int i =
											 * 1; i < e.getErrorLine();) { char
											 * c = parseText.charAt(start); if
											 * (c == '\n') { ++i; } ++start; }
											 * 
											 * start += beginIndex; // -1 for
											 * columns counting starting with 1
											 * start += e.getErrorColumn() - 1;
											 * 
											 * int length = 2; //
											 * (errorToken.endOffset - //
											 * errorToken.beginOffset) + 1; if
											 * (input != null) {
											 * input.addProblem
											 * (SoarProblem.createError(message,
											 * start, length)); }
											 */

										}
									}
								}
							}
							monitor.done();
						}
					});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (problems.size() > 0) {
			MessageDialog dialog = new MessageDialog(shell,
					"Errors Found", null,
					"Found " + problems.size() + " syntax " + (problems.size() == 1 ? "error" : "errors") + ".",
					MessageDialog.INFORMATION, new String[] { "OK" }, 0);
			dialog.open();
			SoarDatabaseSearchResultsView.setResults(problems.toArray());
		} else {
			MessageDialog dialog = new MessageDialog(shell,
					"No Errors Found", null,
					"No syntax errors found.",
					MessageDialog.INFORMATION, new String[] { "OK" }, 0);
			dialog.open();
			SoarDatabaseSearchResultsView.setResults(new Object[0]);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
	}
}
