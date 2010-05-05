package com.soartech.soar.ide.ui.editors.text;

import org.eclipse.jface.text.source.IAnnotationModel;

import com.soartech.soar.ide.core.model.ISoarProblemReporter;

public interface IAnnotated {
	IAnnotationModel getAnnotationModel();
    ISoarProblemReporter getProblemReporter();
}
