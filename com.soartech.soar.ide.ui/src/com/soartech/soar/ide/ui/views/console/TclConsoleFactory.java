/**
 * 
 */
package com.soartech.soar.ide.ui.views.console;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelException;

/**
 * @author aron
 *
 */
public class TclConsoleFactory implements IConsoleFactory 
{
    
    //keep a list of the already added consoles so we don't add duplicates
    //although the addConsoles() doc says it won't add duplicates it does
    List<String> addedConsoles = new ArrayList<String>();
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IConsoleFactory#openConsole()
     */
    @Override
    public void openConsole() 
    {
        //create a console for each soar project
        List<IConsole> consoles = new ArrayList<IConsole>();
        ISoarModel soarModel = SoarCorePlugin.getDefault().getSoarModel();
        try {
            for(ISoarProject p : soarModel.getProjects())
            {
                if(!addedConsoles.contains(p.getProject().getName()))
                {
                    consoles.add(new TclConsole(p.getProject().getName(), p));
                    addedConsoles.add(p.getProject().getName());                    
                }
            }
        } catch (SoarModelException e) {
            e.printStackTrace();
        }
        
        //add the consoles to the console manager
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(consoles.toArray(new IConsole[]{}));
    }
    
}
