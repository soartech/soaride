Tests in duplicate-production

* Open production.soar
* Verify that this file is a member of the duplicate-production agent.

* Select the single-production in that file, 
  either by highlighting or by placing the cursor within it.
* Either press Ctrl+Shift+D or right click and select "Duplicate production" from the popup menu.
* Verify that a new production appears called "copy*of*single-production", but otherwise be identical

* Wait a moment for the soar agent to refresh, then do the other (to test that it works both ways)
* Verify the resulting production is called "copy*2*of*single-production", but is otherwise identical
	