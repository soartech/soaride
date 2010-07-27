package com.soartech.soar.ide.ui.editors.text;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

public class SoarDatabaseTextAnnotation extends Annotation implements IAnnotationPresentation {

	public static final String type = "com.soartech.soar.ide.ui.error";
	
	public SoarDatabaseTextAnnotation() {
		super("com.soartech.soar.ide.ui.error", true, "ANNOTATION");
	}
	
	@Override
	public void paint(GC gc, Canvas canvas, Rectangle bounds) {
		gc.drawText("x", bounds.x, bounds.y);
	}

	@Override
	public int getLayer() {
		return 0;
	}
}
