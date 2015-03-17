/**
 * 
 */
package com.soartech.soar.ide.ui.views.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

/**
 * @author aron
 *
 */
public class TestConsoleFactory implements IConsoleFactory {
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IConsoleFactory#openConsole()
     */
    @Override
    public void openConsole() {
//        ConsolePlugin.getDefault().getConsoleManager().addConsoles(
//                new IConsole[] { new TestMessageConsole() });
        
        
//        sampleGetSelectedProject();
//        getActiveEditor();
//        getAllProjects();
//        findProjects();
        
        ISoarProject tfProject = findProject("tf-agent");
        ISoarProject testProject = findProject("test");
        
        //create and add the TestConsole
//        addConsoleForAgent("foo");
        addConsoleForProject("", testProject);
        
//        final TestIOConsole myConsole = new TestIOConsole("Test Console");
//        ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { myConsole });
////        consoleView.display(myConsole);
//        myConsole.writePromptToConsole();
    }
    
    private void addConsoleForProject(String name, ISoarProject proj)
    {
        
        //create and add the TestConsole
        final TclConsole myConsole = new TclConsole(name, proj);
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { myConsole });
        
        myConsole.writePromptToConsole();
    }
    
    protected Object sampleGetSelectedProject() {
        ISelectionService ss = SoarEditorUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getSelectionService();
        
        String projExpID = "org.eclipse.ui.navigator.ProjectExplorer";
        ISelection sel = ss.getSelection(projExpID);
        
        String pckgExpID = "org.eclipse.jdt.ui.PackageExplorer";
        ISelection sel2 = ss.getSelection(pckgExpID);
        
        
        Object selectedObject = sel;
        if(sel instanceof IStructuredSelection) {
              selectedObject = ((IStructuredSelection)sel).getFirstElement();
        }
        if(selectedObject instanceof IAdaptable) {
              IResource res = (IResource) ((IAdaptable) selectedObject)
                          .getAdapter(IResource.class);
              IProject project = res.getProject();
              System.out.println("Project found: "+project.getName());
        }
        
        return selectedObject;
  }
    
    private IEditorPart getActiveEditor()
    {
        IWorkbench iworkbench = PlatformUI.getWorkbench();
        IEditorPart ieditorpart = null;
        if (iworkbench != null) {
            IWorkbenchWindow iworkbenchwindow = iworkbench.getActiveWorkbenchWindow();
            if (iworkbenchwindow != null) {
                IWorkbenchPage iworkbenchpage = iworkbenchwindow.getActivePage();
                if (iworkbenchpage == null) {
                    ieditorpart = iworkbenchpage.getActiveEditor();
                }
                
            }
        }
        
        return ieditorpart;
    }
    
//    private void foo()
//    {
//        for(SoarAgent agent : agents)
//        {
//            SoarModelTclInterpreter interpreter = agent.getInterpreter();
//            
//            if(interpreter != null)
//            {
//                Set<String> agentFiles = interpreter.getFilesToBuild();
//                filesToBuild.addAll(agentFiles);
//                agentFiles.clear();
//            }
//        }
//    }
    
    private IProject[] getAllProjects()
    {
        IProject[] allProjects = ResourcesPlugin.getWorkspace()
                .getRoot()
                .getProjects();
        
        return allProjects;
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
    
    private void findProjects()
    {
        
        ISoarModel soarModel = SoarCorePlugin.getDefault().getSoarModel();
        if(soarModel instanceof ISoarElement) 
        {
            ISoarElement element = (ISoarElement) soarModel;
            List<ISoarElement> projects = new ArrayList<ISoarElement>();
            try {
                projects.addAll(element.getChildren());
            } catch (SoarModelException e) {
                e.printStackTrace();
            }
            
            for(ISoarElement elem : projects) 
            {
                if(elem instanceof ISoarProject)
                {
                    ISoarProject project = (ISoarProject) elem;
                    String name = project.getProject().getName();
                    int i = 0;
//                    if(project.getProject().getName().equals(name))
//                    {
//                        return project;
//                    }
                }
                
                
                
//                if(elem instanceof ISoarProject) {
//                    ISoarProject proj = (ISoarProject) elem;
//                    
//                }
            }
        }
    }
    
}
