package edu.umich.soar.editor.editors.datamap.actions;

import org.eclipse.jface.action.Action;

import edu.umich.soar.editor.editors.datamap.Datamap;
import edu.umich.soar.editor.editors.datamap.DatamapUtil;

public class OpenDatamapAction extends Action {

	Datamap datamap;
	
	public OpenDatamapAction(Datamap datamap)
	{
		super("Open datamap " + datamap.getFile().getName());
		this.datamap = datamap;
	}
	
	@Override
	public void run() {
	    DatamapUtil.openInEditor(datamap);
//	    datamap.openInEditor();
	}
}
