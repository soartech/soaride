
* Right-click a.soar and click Refactor->Rename...
* Rename it to b.soar
* Click Preview
* Verify that the correct changes are shown in both load1.soar and load2.soar.

* Click Ok
* Verify that the file is renamed and that the changes were applied
  correctly to load1.soar and load2.soar
  
* Right-click subfolder/a.soar and click Refactor->Rename...
* Rename is to subfolder/b.soar and click Preview
* Verify that the correct changes are shown in both load1.soar and load2.soar.

* Click Ok
* Verify that the file is renamed and that the changes were applied
  correctly to load1.soar and load2.soar

* Rename both b.soar files back to a.soar
* Verify that they are renamed correctly and that load1.soar and load2.soar
  are restored to their original state.
   
* Rename pathological.soar, a file that sources itself
* Verify that the source statement in the file is not modified by the rename
  operation.
