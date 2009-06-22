This tests the agent priority setting for files that are part of multiple
agents.

* Open load.soar
* Verify that it is a member of [agent-priority-a] and agent-priority-b

* Hover over the $path variable in the test production
* Verify that the value shown is a.contacts

* Click Soar->Edit Agent Priorities...
* Select agent-priority-b and click "Move Up" until it is above agent-priority-a.
* Click Ok and return to load.soar
* Verify that it is a member of [agent-priority-b] and agent-priority-a
* Hover over the $path variable in the test production
* Verify that the value shown is b.contacts

* Click Soar->Edit Agent Priorities...
* Select agent-priority-b and click "Move Down" until it is below agent-priority-a.
* Click Ok and return to load.soar
* Verify that it is a member of [agent-priority-a] and agent-priority-b
* Hover over the $path variable in the test production
* Verify that the value shown is a.contacts

