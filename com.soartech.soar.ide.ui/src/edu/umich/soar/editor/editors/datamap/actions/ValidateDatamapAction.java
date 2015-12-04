/**
 * 
 */
package edu.umich.soar.editor.editors.datamap.actions;

import org.eclipse.jface.action.Action;


/**
 * @author aron
 *
 */
public class ValidateDatamapAction extends Action {

    
    public ValidateDatamapAction()
    {
        super("Validate Datamap against Soar Project");
    }

    @Override
    public void run() 
    {
        System.out.println("Validating datamap against the Soar Project");
        
        
        
    }
    
    
}
