This tests hyperlinking in the Soar IDE

^^^ File Navigation
* Open load.soar
* Ctrl-click "source a.soar"
* Verify that a.soar is opened

* In a.soar, type ctrl-G
* Verify that you return to load.soar

* In load.soar, ctrl-click "source load.soar" (within the pushd)
* Verify that subfolder/load.soar is opened

* In subfolder/load.soar, ctrl-click "source b.soar"
* Verify that subfolder/b.soar is opened

* In subfolder/b.soar, type ctrl-G twice
* Verify that you are back at the original load.soar file.

* Open multi.soar
* Right-click and select "Open Referencing Files"
* Verify that a dialog pops up allowing a selection between load.soar and a.soar
* Select a.soar
* Verify that a.soar is opened

^^^ Built-in Tcl help
* Open load.soar
* Ctrl-click the "set" command
* Verify that the Soar/Tcl help browser opens with the Tcl man page for the set command

^^^ Tcl Procedure hyper-linking
* Open subfolder/b.soar
* Ctrl-click the "test-procedure" command
* Verify that a.soar is opened and the definition of the test-procedure proc is 
  highlighted
  
* Open subfolder/b.soar
* In the test-rhs-macro production, ctrl-click the "rhs-macro" command
* Verify that a.soar is opened and the definition of the rhs-macro proc is 
  highlighted

  
