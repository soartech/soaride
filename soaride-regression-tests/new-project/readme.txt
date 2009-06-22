This tests creating a new Soar project as well as creating a new agent
with from the editor status bar.
Also tests bug 1405.

* Switch to the Soar perspective and close all open editors.
* Click File->New->Project...
* Create a new general project called "new-project"
* Right-click the new project and click New->Soar File
* Create a new file called load.soar in the root of new-project.
* Verify that the new file opens and the message "There are no agents
  in this project ..." appears at the top of the editor.

* Click "Click here" at the top of the editor
* Name the new agent "agent" and click finish
* Verify that the agent was created and it was opened in the editor

* Return to the load.soar editor
* Verify that the message at the top of the editor is "Member of: [agent (-)]"

* Delete new-project
* Verify that the agent editor closes automatically
* Verify that the load.soar editor closes automatically
