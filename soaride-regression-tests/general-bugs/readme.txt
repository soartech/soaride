This folder is for testing simple bugs that do not require multiple
files or a specially configured agent. Generally, bugs involving
pure, non-Tcl Soar code with no start file. Each bug has its own file, labeled with the
Bugzilla bug number it tests.

* Open not-reachable-from-start-file.soar
* Here were making sure that the "not reachable" warning is not displayed
  if no start file is selected.
* Verify that there are no errors or warnings

* Open bug-1461.soar
* Validate that there are no errors on the test-bug-1461-no-error production
* Validate that there is a "Too many arguments" error on the test-bug-1461-yes-error production

* Open bug-1458.soar
* Validate that there are no errors in the file

* Open bug-1454.soar
* Validate that there are no errors in the file