This tests adding and removing a file from an agent

* Open file-with-errors.soar
* Verify that it is not a member of any agent (message at top of editor)
* Verify that no errors are reported in the file

* Open agent-file-selection.soaragent
* Check "file-with-errors.soar" to add the file to the agent and save
* Return to file-with-errors.soar
* Verify that the file is a member of agent-file-selection
* Verify that the following errors and warnings appear:
** Warning: agent-file-selection: file does not appear reachable from selected start file
** Error: Encountered "(". Expected: <SYMBOLIC_CONST>

* Click the (-) next to agent-file-selection at the top of the editor to remove the file
  from the agent again
* Verify that it is not a member of any agent (message at top of editor)
* Verify that no errors are reported in the file


