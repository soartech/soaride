/**
 * 
 */
package com.soartech.soar.ide.ui.views.console;


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
public class TclConsoleFactory implements IConsoleFactory {
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IConsoleFactory#openConsole()
     */
    @Override
    public void openConsole() 
    {
        ISoarProject tfProject = findProject("tf-agent");
        ISoarProject testProject = findProject("test");
        
        //create and add the TestConsole
        addConsoleForProject("", testProject);
    }
    
    private void addConsoleForProject(String name, ISoarProject proj)
    {
        //create and add the TestConsole
        final TclConsole myConsole = new TclConsole(name, proj);
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { myConsole });
        
        //consoleView.display(myConsole);
        myConsole.writePromptToConsole();
    }
    
    private ISoarProject findProject(String name)
    {
        ISoarModel soarModel = SoarCorePlugin.getDefault().getSoarModel();
        
        try {
            return soarModel.getProject(name);
        } catch (SoarModelException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
}
