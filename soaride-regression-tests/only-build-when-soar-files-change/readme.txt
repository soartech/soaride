This tests that the Soar build process is only run when a Soar file or agent
is modified.  Otherwise, in large, multi-language projects, you get the overhead
of a Soar build every time you save any other file in the system.

* Open only-build-when-soar-files-change/load.soar
* Change the 0 to 1 and save
* Verify that "Building Workspace" appears in the lower right corner of Eclipse
  for about 5 seconds.
  
* Open only-build-when-soar-files-change/other.txt
* Make some modification to the file and save
* Verify that "Building workspace" is nearly instantaneous

* Revert only-build-when-soar-files-change/load.soar before proceeding to any
  further tests. Otherwise, the long delay will always be present.
