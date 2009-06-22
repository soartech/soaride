This tests the ability of Soar IDE to output the stack of a Tcl error in the Soar Source Viewer as well as to 
avoid evaluating source commands on non-exisatant files.

* Open productions.soar
* Bring up the Soar Source Viewer and expand the test-good production
* Verify that there is no error on the production and that the Tcl expands to:
	"(state <s> ^superstate nil)"
* Now place the test-bad production into the Soar Source Viewer and expand the Tcl
* Verify that there is an error on the production that the expansion in the Soar Source Viewer has the Tcl stack in it:
	can't read "error": no such variable
	    while executing
	"return "(state <s> ^superstate $error)""
	    (procedure "recurse" line 2)
	    invoked from within
	"recurse"
	    invoked from within
	"return [recurse]"
	    (procedure "lhs-with-error" line 2)
	    invoked from within
	"lhs-with-error"
	    invoked from within
	"return " test-bad
	    [lhs-with-error]
	-->
	    (interrupt)
	""
	    (in namespace eval "::" script line 1)
	    invoked from within
	"namespace eval :: {return " test-bad
	    [lhs-with-error]
	-->
	    (interrupt)
	"}" 

* Verify that load.soar contains an error that test.soar does not exist.
* Verify that tcl-error-reporting.soaragent does not contain any errors (no error icon on the file) 