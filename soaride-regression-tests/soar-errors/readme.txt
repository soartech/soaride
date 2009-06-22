This tests various Soar variable errors that are found by Soar IDE

* Open soar-errors.soar
* Verify the following errors:
** In test-disconnected-from-state, "Variable <conact> is not connected to state"
** In test-unbound, "Unbound variable <location> passed to function"
** In test-no-positive-conditions1, "Production has no positive conditions"
** In test-no-positive-conditions2, "Production has no positive conditions"
** In test-function-arguments:
	"Too many arguments to function 'crlf'. Expected no arguments"
	"Too many arguments to function 'halt'. Expected no arguments"
	"Too many arguments to function 'interrupt'. Expected no arguments"
	"Too many arguments to function 'timestamp'. Expected no arguments"
	"Too many arguments to function 'accept'. Expected no arguments"
	"Too few arguments to function 'capitalize-symbol'. Expected exactly 1 argument"
	"Too few arguments to function 'ifeq'. Expected exactly 4 arguments"
	"Too many arguments to function 'strlen'. Expected exactly 1 argument"
	
Fix the errors and verify that the errors go away.
	