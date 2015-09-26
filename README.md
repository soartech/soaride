# SOAR IDE

SOARIDE is an eclipse plugin that provides syntax highlighting and checking for [Soar](http://soar.eecs.umich.edu), a general cognitive architecture for developing systems that exhibit intelligent behavior. This plugin also supports the use of TCL as a macro language for dynamically creating Soar productions.

## Installation

Add this plugin to Eclipse with the update site:

	https://github.com/soartech/soaride/raw/master/com.soartech.soar.ide.update

---------------

# Plugin Development

## Requirements

## Installation

## Architecture
Some quick notes on various important components/classes in SOAR IDE:

* **ISoarModel**: Main entry point for the Soar plugin. It allows you to:
	* Get a list of projects in the workspace that use the Soar nature
	* Get lists of files associated with various Soar agents
	* Register listeners for add/remove/change events on ISoarElements
* **ISoarElement**: A top level interface class used to represent pretty much all aspects of a Soar Project as a tree hierarchy
	* It has it's own lock
	* It has pointers to the workspace and project that it's in and pointers to it's parents and children
	* Also has pointers to the associated resource/file that contains/defines the element
* **ISoarAgent**: Pretty broad interface for the concept of "SoarAgent" in a Soar project
	* Primarily a container class for all the resources that make up an agent, provides accessors for lower level items like productions
	* Provides some access the interpreter
	* Provides some file/sync functionality and the ability to return "Working Copies" of agents (not sure what you do with a working copy yet)
	* Looks like makeConsistent is the primary interface to error checking and agent validation type functionality
* **ISoarProject**: Another container class in the SoarElement hierarchy
* **SoarModelTclInterpreter**: Core component responsible for wrapping the Tcl interpreter used to expand Soar productions (could be either simple interpreter or the interpreter from JSoar). Keeps track of the files sourced and the productions created.
* **SoarFileAgentProxy**: This one is pretty confusing. It seems like it's a representation of a file as sourced by an agent? Basically a "view" of the file as sourced by a particular agent.

## Design and Refactoring Notes
Some quick observations about the code base:
* What are "Working Copies" for?
* It has a lot of locking in it and threading model isn't really apparent
* What is the lifecycle of the JSoar agent contained in the SoarAgent class?
* There is quite a bit of complexity in various parts of the ISoarElement hierarchy. It seems like a lot for a system that's essentially producing error/warning markers and doing syntax highlighting. Maybe it supports live running and debugging of agents and the Outline view in eclipse.
* Both SoarAgent and SoarModelTclInterpreter expose some internals that probably seem like a bad idea (SoarAgent exposes the underlying JSoar agent and the interpreter exposes it's multiple interpreter internals)
* In JSoar it looks like interp doesn't have to be a member of the Agent class, it's only really sourcing the aliases definition, also doesn't look like the interp in the plugin needs to be attached to an agent.
* SoarTclInterface should be rewritten so that multiple dispose calls don't cause problems, also it leaves one Rhs function still regestered
* SoarAgent doesn't handle the case when setInterpreter is called twice with the same interpreter
* TclExpansionError should really return a SourceLocation, otherwise you've got to reach into the interpreter to pull out this type of information, also message should be what is currently the last line of the multiline error message

## KNOWN BUGS
* Throws null pointer exceptions on startup and sometimes during a clean.
* Some errors point to the top level agent file instead of the appropriate low level production file.

## TODO
* This README needs to be fleshed out
* We're using a lot of deprecated eclipse concepts (actions, actionSets, popupMenus, etc...), at some point we'll have to update these.

--------------

# License

Copyright (c) 2015, Soar Technology, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

* Neither the name of Soar Technology, Inc. nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY  *EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED   *WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.   *IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,   *INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT   *NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR   *PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,    *WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)   *ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE    *POSSIBILITY OF SUCH *DAMAGE.
