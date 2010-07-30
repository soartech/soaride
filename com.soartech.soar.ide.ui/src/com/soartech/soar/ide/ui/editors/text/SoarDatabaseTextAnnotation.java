package com.soartech.soar.ide.ui.editors.text;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import com.soartech.soar.ide.ui.SoarEditorPluginImages;

public class SoarDatabaseTextAnnotation extends Annotation implements IAnnotationPresentation {

	public static final String type = "com.soartech.soar.ide.ui.error";
	
	public SoarDatabaseTextAnnotation() {
		super("com.soartech.soar.ide.ui.error", true, "ANNOTATION");
	}
	
	@Override
	public void paint(GC gc, Canvas canvas, Rectangle bounds) {
		Image i = SoarEditorPluginImages.get(SoarEditorPluginImages.IMG_ERROR_OVERLAY);
		gc.drawImage(i, 0, 0, 12, 17, bounds.x, bounds.y, bounds.width, bounds.height);
	}

	@Override
	public int getLayer() {
		return 0;
	}
}
