package com.soartech.soar.ide.ui.editors.text.quickfix;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.ExtensionContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.services.IServiceLocator;

import com.soartech.soar.ide.ui.editors.text.SoarEditor;

public class MarkerContributionFactory extends ExtensionContributionFactory 
{

    @Override
    public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) 
    {
        SoarEditor editor = (SoarEditor) 
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();

            additions.addContributionItem(new MarkerMenuContribution(editor), null);
    }

}
