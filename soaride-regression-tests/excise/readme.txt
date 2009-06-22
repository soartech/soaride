This tests Soar IDE's ability to detect overwritten productions

* Open load.soar and other.soar
* Verify that there are no errors in other.soar
* Verify that load.soar has on "Overwrites" error on the second test-excise production

* Add "excise test-excise" between the two test-excise productions and save
* Verify that load.soar now has no errors
* Verify that an "Overwrites" error has been added to the test-excise production in
  other.soar
  
* Add "excise test-excise" before the test-excise production in other.soar and save
* Verify that there are no errors in other.soar
* Verify that there are no errors in load.soar

* Remove both excise commands (revert load.soar and other.soar)
* Verify that there are no errors in other.soar
* Verify that load.soar has on "Overwrites" error on the second test-excise production




