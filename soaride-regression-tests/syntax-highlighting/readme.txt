This tests syntax highlighting in the Soar editor

* Open syntax-highlighting.soar
* Vefify that all of the comments are colored correctly
* Verify that "state" is colored in case (A)
* Verify that <s> is colored as a variable in case (A)
* Verify that <bar> is colored as a variable in case (B) (bug 1416)
* Verify that "interrupt" is colored as a RHS function in case (C)
* Verify that "[left-hand-side 1 2 3]" is colored as a Tcl macro in case (D)
* Verify that "$iopath" is colored as a Tcl variable in case (E)
* Verify that "sp" is colored in case (F)
* Verify that the disjunction is colored in case (G)
* Verify that the string is colored correctly in case (H) 

* Open "Window->Preferences->Soar Editor->Syntax Coloring" and change colors
  of various language elements
* Verify that the colors change in the open editor

* Close and reopen the Eclipse workbench
* Verify that the colors chosen in the previous step persist across Eclipse sessions.
