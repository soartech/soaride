This tests creating and deleting a simple agent

* Open load.soar
* Verify that the message "This file is not a member of any agent..." appears
  at the top of the editor.

* Right-click the create-agent directory and click "New->Soar Agent"
* Name the agent "create-agent" and click Finish
* Verify that the agent's start file is /create-agent/load.soar
* Verify that the message "Member of: [create-agent]" appears at the top
  of the editor for load.soar
* Verify that "create-agent" appears in the Soar Datamap view

* Right-click "create-agent.soaragent" and click delete
* Verify that the "create-agent" agent editor closes automatically
* Verify that "create-agent" no longer appears in the Soar Datamap view
* Verify that the message "This file is not a member of any agent..." appears
  at the top of the editor for load.soar.

