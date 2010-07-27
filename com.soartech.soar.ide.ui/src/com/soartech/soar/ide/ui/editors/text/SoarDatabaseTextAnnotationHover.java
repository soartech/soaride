package com.soartech.soar.ide.ui.editors.text;

import java.util.ArrayList;

import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;

import com.soartech.soar.ide.core.model.SoarProblem;

public class SoarDatabaseTextAnnotationHover implements IAnnotationHover {
	
	private ISoarDatabaseTextEditor editor;
	
	public SoarDatabaseTextAnnotationHover(ISoarDatabaseTextEditor editor) {
		super();
		this.editor = editor;
	}
	
	@Override
	public String getHoverInfo(ISourceViewer viewer, int line) {
		String document = viewer.getDocument().get();
		ArrayList<SoarProblem> problems = editor.getInput().problemsAtLine(line, document);
		StringBuffer buff = new StringBuffer();
		int size = problems.size();
		for (int i = 0; i < size; ++i) {
			SoarProblem problem = problems.get(i);
			buff.append(problem.message);
			if (i != size - 1) {
				buff.append('\n');
			}
		}
		return buff.toString();
	}

}
