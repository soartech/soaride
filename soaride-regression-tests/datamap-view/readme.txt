This tests the Soar datamap view

* Set the layout of the datamap view to horizontal from the toolbar
* Close the workbench
* Reopen the workbench
* Verify that the layout was persisted and that the layout is selected in the toolbar
* Repeat for the remaining layouts

Bug 941
-------
* Open load.soar in an editor
* Expand the datamap
* Right click on the datamap-view/state(?)/<*>(+) node.
* Select 'Generate Monitor Production'
* Verify that the following is generated in the load.soar editor with no exceptions:

##!
# @brief Auto-generated monitor production
sp {monitor-attributes*
    (state <s> ^state <s0>)
    (<s0> ^<*1> <*0>)
-->
    (write (crlf) |Auto-generated monitor: |
        |<*0>=| <*0>    
)
}

----

* Expand the datamap for datamap-view
* Control-click to select the ^superstate and ^io.input-link.contacts.contact attributes
* Verify that test-attribute-intersection1, test-attribute-intersection2, and
	test-attribute-intersection3 are all listed in the datamap production list.
	
* Control-click to select only the ^superstate and ^io.input-link.contacts.contact.location attributes
* Verify that only test-attribute-intersection1 and
	test-attribute-intersection3 are all listed in the datamap production list.

 

