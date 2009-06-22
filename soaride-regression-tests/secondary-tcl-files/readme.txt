This tests the ability of Soar IDE to detect when a secondary file needs to be
reparsed do to a change in a Tcl macro.

* Expand secondary-tcl-files in the Soar Datamap view
* Verify that there is an attribute named "superstatex" with value nil
* Highlight the attribute and and verify it is a member of test-rename-superstate

* Double-click test-rename-superstate to open productions.soar
* Control click on [lhs] in the production to jump to the lhs macro in macros.tcl

* In the macro change ^superstatex to ^superstate and save the file
* Verify that the superstatex attribute in the datamap has been changed to superstate

* Verify in productions.soar that test-fix-lhs-error has the error "only state allowed"
* Control-click [lhs-with-error] to jump to the macro
* In the macro, change statex to state and save the file
* Verify that the error has been removed from test-fix-lhs-error
