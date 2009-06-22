This tests that new files and folders are correctly added to agents as they're
created.

* Highlight the new-files-and-folders folder and click "Soar->New Soar File"
* Name the file "test" (the extension will be added automatically)
* Verify that the message "Member of: [new-files-and-folders]" appears at the
  top of the editor for test.soar.
* Verify that the line "source test.soar" has been added to the load.soar file.

* Highlight the new-files-and-folders folder and click "Soar->New Soar Folder"
* Name the folder "subfolder"
* Highlight the subfolder folder and click "Soar->New Soar File"
* Name the file "subtest" (the extension will be added automatically)
* Verify that the message "Member of: [new-files-and-folders]" appears at the
  top of the editor for subtest.soar.
* Verify that the warning "new-files-and-folders: file does not appear reachable from selected start file"
  appears in the subtest.soar file.

* Open new-files-and-folders/load.soar
* Add "source subfolder/subtest.soar" to the end of the file and save.
* Verify that the "not reachable" warning is removed from subtest.soar
  (TODO: This does not currently work. Performing a clean build will clear
  		 the warning. See bug 1402.)
  		 
* Delete subfolder and test.soar
* Verify that open editors are closed correctly

* Open load.soar
* Verify that there are two errors, one each missing file
  test.soar and subtest.soar

* Delete the source commands file load.soar. 
* Verify that the two source errors are gone.




