# Eclipse Update Site
```
https://github.com/soartech/soaride/raw/master/com.soartech.soar.ide.update
```
---

# Getting Started with the Soar IDE

The Soar IDE is an eclipse plugin that provides syntax highlighting and checking for [Soar](http://soar.eecs.umich.edu), a general cognitive architecture for developing systems that exhibit intelligent behavior. This plugin also supports the use of TCL as a macro language for dynamically creating Soar productions.

The Soar IDE includes many features that make it the development environment of choice for the world's best Soar programmers including:

*   Syntax Highlighting
*   Error Marking
*   Content Assist
*   Templates
*   Hover Help
*   Hyperlinking
*   An interactive datamap of Soar memory
*   Grouping subsets of soar files into "agents"
*   A Source Viewer to display expanded Tcl calls


# Installing the Soar IDE Plugin

### Install Eclipse

*   Since the Soar IDE is built as a plug-in to Eclipse, you need to [download](http://www.eclipse.org/downloads) Eclipse (version 4.5.2 or higher) and install it.
*   Help installing Eclipse can be found [here](http://www.eclipse.org/documentation).

### Java

*   The plugin requires the Java Runtime Environment (JRE) 6.0 or greater, if you do not have it installed, you can get it [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

### Install the Soar IDE Eclipse Plugin

Once Eclipse is installed, you need to download the Soar IDE plug-in.

1.  From the **Help** menu in Eclipse, select **Install New Software...** to open the **Install** dialog.
2.  Now add a new remote site to download the Soar IDE plug-in:
    1.  Click the **Add...** button to open the **Add Repository** dialog.
    2.  Type _Soar IDE_ into the **Name** text field in the dialog.
    3.  Type _https://github.com/soartech/soaride/raw/master/com.soartech.soar.ide.update_ in the **URL** text field and click **OK**.
3.  A new site should now be displayed in the **Install** dialog. Make sure the box next to the **Soar IDE** site is checked, and click **Finish** to move on.
4.  The **Updates** dialog should now be displayed. Check the box next to the **Soar IDE** feature, and click **Finish**.
5.  Accept the license agreement on the this screen, and click **Next**.
6.  Now click **Finish** to begin installing the plug-in.
7.  You will be asked to verify the plug-in that is being installed. Click **Install All** to proceed.

Eclipse should now ask you to restart in order to complete the installation.

# The Soar Perspective

### Open the Soar Perspective

An Eclipse perspective is a pre-defined grouping of views associated with the language or problem context we want to work with. Eclipse comes with perspectives useful for various tasks, the default being the Java Perspective.

To view the list of available perspectives, from the **Window** menu choose **Open Perspective -> Other**.

This displays a dialog box asking us to choose a perspective to open. In this case, we will be writing a Soar program, so we will open the Soar Perspective.

Select **Soar** and click **OK**.

# Soar Projects

### Create a new Soar Project

In Eclipse, files exist within projects, so before we create any files, we must first create a new project for them to be a part of.

To create a new general project, right-click in the Package Explorer view and choose **New -> Project**.

This opens the **New Project** wizard dialog which contains wizards for every type of project Eclipse knows about. In order to provide support for Soar within multiple project types, there is no specific "Soar" project type. Instead, we have the ability to set "Soar Project Support" on any type of project so that it understands Soar.

*   _**Note:** If we wanted to integrate Soar into a Java project, we would instead create a Java project here. This applies, in general, to other languages as well._

Since we will just be writing Soar code, choose **General -> Project** from the list of project wizards and click **Next**.

Now we need to name the new project. Type a name, such as "helloworld" in the **Project name** field.

Once this is done, click **Finish** to create the project.

The newly created project is now displayed in the Package Explorer.

Empty projects do not provide Soar support automatically. There are two ways to get them to however:

*   Create a new soar agent for the project.
*   **Right-click** on the project in the **Package Explorer** and choose **Include Soar Project Support**.

*   _**Note:** We can tell that a project has Soar Support by the "s" decoration on the project icon in the Package Explorer._

# Soar Files

### Create a new Soar File

Now that we have an empty project with Soar support, we can create a new Soar file for that project.

To add a new file to the project, right-click on the project and choose **New -> Soar File**.

The **New Soar File** dialog should now appear on the screen. This dialog handles the creation of Soar files, and will automatically add the .soar extension to your new file.

Enter a name for the new Soar file in the **File name** field and click **Finish** to create it and add it to the project.

*   _**Note:** The project that was initially selected when the **New Soar File** wizard was activated is automatically selected as the parent folder when the file is created in this way._

# Soar Agents

### Brief Overview

*   In order for the Soar IDE to process soar files (besides syntax highlighting), they must be added to an agent.
*   Soar files can be part of multiple agents.
*   A Soar Agent exists within the project as a single text file.

Please see the Agents and the Agent Editor help section for more information.

### Create a new Soar Agent

The newly created file should now be displayed in the editor. Notice the message at the top of the file informing you that "There are no agents in this project", along with a link to click to remedy this problem.

Click the provided link to open the **New Soar Agent** wizard. Enter a name for the agent in the **Agent name** field and click **Finish** to create it and add it to the project.

The newly created agent should now be displayed in the editor. If you click on your soar file in the editor, you'll notice the _Member of:_ message at the top telling you which agents this soar file belongs to.

*   _**Note:** Alternatively, to open a **New Soar Agent** wizard, you can right-click on the project and choose **New -> Soar Agent**, or choose **Soar -> New Soar Agent** from the Eclipse menubar._

### More Information

Please see the Agents and the Agent Editor help section for more information.

# Soar Productions

### Writing a Soar Production

To write a Soar production, we use the **sp - Soar Production** template built in to the Soar IDE. Simply type **sp** into the editor and then press **ctrl-space**. This displays an auto-complete popup from which you can choose the corresponding template.

Select the template by **double-clicking** on it or by pressing **Enter**.

Opening the **sp - Soar Production** template generates a bare-bones Soar production. The production name starts out highlighted, ready to be entered. You can then

The location to enter the new production name will start out highlighted, and pressing **tab** will jump the cursor between the other areas in the template that need to be filled out. These areas are outlined with blue rectangles.

A sample "hello world" production is shown below. If you are following along with the "Getting Started" tutorial, please fill out the rest of the template as shown below:

``` sp { helloworld ( state <s> ^superstate nil) --> (write |I'm sorry Dave.|) (write |I'm afraid I can't do that.|) (halt) } ```  

# Running your Soar Project

### Running your Soar Project

To run the Soar project, we need to hook it to a debugger. In this case, we will use the Java Soar Debugger available as part of the Soar Suite.

To hook the project to the Java Soar Debugger, we will use Eclipse's External Tools run manager. This is accessed by clicking the arrow next to the **External Tools** icon in the Eclipse toolbar, then choosing **External Tools** from the dropdown menu.

The **External Tools** dialog should now be shown on the screen.

To create a new launch configuration, click the **New** button at the far left of the toolbar. It's the button that looks like a blank piece of paper with a yellow '+' in the upper right corner.

*   _**Note:** Make sure the **Program** launch configuration is highlighted when you click the **New** button, not the **Ant Build** launch configuration._

Now we need to give the new launch configuration four pieces of information to run. An example of this launch configuration is shown below also.

*   **Name**: This can be anything, in this case "java_soar_debugger".
*   **Location**: To keep this line clean, we will be running a .bat (or .sh) script. [Here](../content/run-JavaDebugger.bat) is a sample one.
*   **Working Directory**: The working directory for the script (or program) to run from.
*   **Arguments**: The arguments to give the program that is being run.

Now save the run configuration by clicking **Apply** and run it by clicking **Run**. Running it opens the Java Soar Debugger with the production loaded and ready to go.

*   _**Note:** Once we have run the configuration once, it will appear in the **External Tools** dropdown menu on the Eclipse toolbar._

# Soar Editor

The Soar Editor is a text editor with the Soar-specific features described below.

### Syntax Highlighting

The editor supports basic syntax highlighting. The functions and commands defined in the Soar8 Manual, as well as variables and comments, are shown with special formatting to indicate that they are keywords or simply to make a block of code stand out.

The colors used for syntax highlighting can be configured on the Syntax Coloring Preferences Page

### Error Marking

The Soar Editor checks the syntax of your Soar code. Errors are marked in typical Eclipse fashion with both an error marker on the line where it occurs, as well as an entry in the **Problems View**.

Typical Errors include:

*   Invalid production syntax
*   Disconnected variables
*   Duplicate production names

### Agent Membership

The editor includes a bar at the top of each file indication which agents the current soar file is a member of.

If the current soar file is not a member of any agents, the option will be given to add it to an existing agent or create a new agent.

If the current soar file is part of an agent, clicking the "(-)" will remove it from the corresponding agent.

### Content Assist

The Soar IDE includes many different types of content assist in the editor. Assistance is provided for Soar keywords, datamap attributes, production variables, and Tcl macros.

The content assist is invoked by pressing **ctrl-space** in various specific contexts. The list will automatically be narrowed as letters are typed.

*   After a whitespace character, content assist will display a list of Soar keywords and Tcl procedure names.
*   After a **^** content assist will display a list of valid attributes from the datamap.
*   After a **<** content assist will display a list of valid variables.
*   After a **[** content assist will display a list of valid Tcl macros.

### Templates

Eclipse Templates are small snippets of reusable code that can be automatically inserted into the editor. They act like a short form in which you fill with custom text. For more details on Eclipse Templates, refer to the article on [Teaching Templates](http://www.javalobby.org/forums/thread.jspa?threadID=15736).

To use a Soar Template:

2.  Press **ctrl+space** just as you would if you were looking for content assistance. The pane to the right displays the text of the currently selected template.

4.  Press **enter** to run the selected template. The form elements of the active template are outlined.
5.  Press the **tab** key to move between elements.

The templates used by the Soar IDE can be created and configured on the Templates Preferences Page

### Code Folding and Regions

The editor provides functionality for code folding, with both automatic and user defined regions.

Automatically defined foldable regions include both soar productions and tcl procedures.

Users can define foldable regions by surrounding code with **#region RegionName** and **#endregion**. The syntax should look familiar as it is the same in Visual Studio.

Notice that the user defined region is also displayed in the outline.

### Soar Document Formatting

The editor has a basic integrated code formatter. It can be invoked by highlighting a region of code and pressing **ctrl-shift-f** or by selecting **Format Soar Document** through the pop-up menu. The entire file will be formatted if no selection is highlighted.

The code will be formatted according to the values set in the Soar Editor Preferences Page.

### Hover Help

The Soar Editor displays hover help for user defined Tcl procedures, built-in Tcl procedures, and Soar commands. To access hover help, hover the cursor over the name of the desired procedure or command.

*   Hovering over a user defined Tcl procedure displays the comment for that procedure as well as a list of arguments it takes.

*   Hovering over a built-in command displays a short description of the command followed by a synopsis of how to use the command.

### Hyperlinking

Hyperlinking in the Soar Editor allows you to easily navigate to the definition of a Tcl procedure by **ctrl-clicking** on its name. When the **ctrl** button is held down, the text of hyperlinkable items turn blue and become underlined as you mouse over them.

You can also access integrated manpages for built-in Tcl procedures with this method.

### Hotkeys

*   **[ctrl-/]** Comments out the selected line.
*   **[alt-/]** Auto-completes the word adjacent to the cursor. Hitting the hotkey multiple times iterates over the list possible auto-completion choices.
*   **[ctrl-g]** Opens any files referencing this one. This is also an option in the popup menu.

# Soar Agents and the Agent Editor

### Soar Agents

Within the Soar IDE, and agent is an organizational concept roughly equivalent to a single Visual Soar project. An agent is stored in the Eclipse workspace as a file with a .soaragent extension. An agent consists of:

*   A set of files from the Eclipse workspace that are included as members of the agent. A file will not be checked for errors and will not be used in datamap calculations unless it is a member of an agent.
*   A start file which is used to load the source code. This is typically the file you would source to load the agent into Soar. It is used to provide warnings when a particular file is not reachable from the start file and to detect overwritten production, i.e. two productions with the same name which may indicate a copy/paste error.
*   A datamap. The datamap is constructed only from the productions foudn in files that are members of the agent. The datamap is used in code completion.

Soar files are not required to be a member of any agent. For such files, only syntax highlighting will be enabled. A Soar file may be a member of more than one agent.

### Agent Files

Agent files are simply files in the Eclipse workspace with a .soaragent extension. There is no requirement about where they are located or even to be in the same folder as the files that constitute the agent's code base. Agent files are plain text XML files. As such, they should be checked into version control. When an Eclipse project containing Soar agents is checked out from version control, or imported, the agents are discovered automatically.

### Creating New Agents

To create a new agent, click **Soar->New Soar Agent...**. Select the folder you would like the agent to reside in and click Finish. If **Create empty agent** is not checked, the agent will automatically be populated with all Soar files in the folder (and all sub-folders) in which the agent is created. Additionally, if a file named **load.soar** is present, it will automatically be set as the agent's start file.

_Note: If Soar support is not enabled when the agent is created, it will be enabled automatically. Thus, it is almost never necessary to manually enable Soar support._

### Agent Editor

Soar agents are edited with a custom form editor in Eclipse. There are several ways to open an agent in the agent editor:

*   Double-click the agent file (name.soaragent) in the package explorer, or eclipse navigator view
*   Double-click the agent in the Soar Explorer view.
*   Double-click the agent in the Soar Datamap view.
*   When editing a file that is a member of the desired agent, click the agent's name in the hyperlink bar at the top of the editor.

Like other editors, changes to agents must be saved before taking effect. For example, you may change the start file and remove several files from an agent, but the changes will not take affect until you save.

#### Selecting the Start File

To select a start file, click the **...** button next to the start file text field. This will open an Eclipse resource selection dialog. The first text field is a text filter. Type a filter in this field such as "*.soar", or the first few letters of the name of the file you'd like to use. Select the file from the list provided and click **Ok**.

#### Adding and Removing Member Files

Members files are added to and removed from an agent in the "Agent File Selection" section of the agent editor. The left panel displays all of the directories in the Eclipse project while the right panel displays the files in the folder selected in the left panel. Checking or unchecking a folder in the left panel adds or remove all sub-files and sub-folders in the folder. Checking or unchecking a file in the right panel gives fine-grained control over the exact files that are included.

Since many Soar agent code bases are restricted to a single directory tree, it is usually simplest to check the top-level folder of the code base and then selectively filter out files as necsesary. If new files or folders are added by the user, they will be automatically added to the agent.

Note that when a file is not a member of any agent, it can be quickly added to an agent through the hyperlink bar at the top of the Soar editor. Similarly, a file can be removed from an agent by clicking the **(-)** link in the hyperlink bar.

#### Options

The final section of the agent editor is an options section. It currently has a single option, "Source command changes ...". When checked, the source command performs a directory change automatically. This is the behavior of UofM's Soar distribution. When unchecked, the directory change is not performed. This is equivalent to the behavior of the Tcl source command.  

# Soar Dynamic Datamap

The dynamic datamap is generated by running an analysis of the productions in the project to create a depiction of the soar memory structure.

### Datamap Table

Elements in the datamap table are attributes that are tested against states in productions.

*   Click on an attribute to display the list of corresponding productions in which the attribute was tested or created.
*   **Ctrl-click** multiple attributes to display the intersection of productions corresponding with those attributes.
*   Each attribute has some combination of '+' and '?' descriptors associated with it. The '+' descriptor means that the attribute was created on the RHS, and the '?' descriptor means that the attribute was tested on the LHS.
*   You can generate a production that monitors selected attributes by right-clicking on them and choosing **Generate Monitor Production** from the context menu. The generated production tests for the selected attributes on the LHS.
*   The **Values** column in the table displays values that the attributes are tested against or created with.

### Production List

The production list displays productions that correspond to selected attributes in the Soar Datamap.

*   Select a production here to display it in the source viewer.
*   Double-click a production to open the file it is located in and display it in an editor.
*   Error and warning markers decorate the displayed elements according to the status of the model.

# Soar Static Datamap

The static datamap gives the user the ability to define expected data structures, and then perform static checking on the code to see if it creates or tests any unexpected data structures. This can help find bugs and typos.

### Static Datamap

The static datamap exists as a .dm file in the base directory of your soar project.

*   Create a new static datamap by right clicking on your project and selecting New -> Soar Datamap and giving it a name.
*   The static datamap opens itself as an editor, it is not displayed in a different view like the dynamic datamap.
*   There are two tabs in the static datamap editor: one with a tree-view which can be manipulated through context menus, and one with the raw text model.
*   Interact with the static datamap by right-clicking on the tree to add/edit children and validate against the project.

# Soar Outline

The Soar Outline displays the Soar productions and Tcl procedures defined in the file open in the currently active editor (more than one editor can be opened at once).

You can navigate the editor to the definition of a given production or procedure by selecting it in the outline.

### Toolbar Features

*   Sort outline elements alphabetically.

### Other Features

*   Error and warning markers decorate displayed elements according to the status of the model.
*   Select a production or procedure to display it in the Soar Source Viewer and navigate to its definition within the file.

# Soar Explorer

The Soar Explorer is a Package Explorer-type view that displays the Soar productions and Tcl procedures in the project. With the Soar Explorer, you can display the productions and procedures as a single list under the project or as a file tree grouped as packages according to their location in the project.

### Toolbar Features

*   Filter the productions and procedures by entering a text string in the **Filter** box.
*   Toggle buttons to show/hide corresponding productions and procedures.
*   Sorting the explorer elements alphabetically.
*   Toggle the structure of the explorer between a project-wide list of productions and procedures, and a tree displaying the elements in packages and files.

### Other Features

*   Displayed elements are decorated with error and warning markers according to the status of the model.
*   Selecting a production or procedure displays it in the Soar Source Viewer.
*   Double-clicking a procedure or production opens an editor that displays the file and location of its definition.

# Soar Source Viewer

The Soar Source Viewer displays the source code of the selection in the currently active view (Package Explorer, Soar Explorer, Soar Editor, etc). This is useful for quickly browsing code without opening several editors.

### Tcl Expansion

When viewing code in the Source Viewer, you many optionally turn on Tcl expansion by clicking on the toolbar button. This option displays any calls to Tcl macros in their expanded form.

# Soar Editor Preferences

The Soar Preferences page contains settings for preferences within the Soar IDE such as editor spacing, syntax coloring, and templates. It can be accessed through the top Eclipse menu by choosing **Window -> Preferences -> Soar Editor**. The Soar Preferences page includes a main Soar Editor preferences page and three subpages.

### Soar Editor

The head preferences page contains preference settings for the displayed tab width and tabs to spaces conversion.

These preference settings are also used for the Automatic Code Formatting in the editor.

### Syntax Coloring

The Syntax Coloring subpage lets you select colors for the various objects that appear within the Soar Editor.

### Templates

The Templates preferences subpage lets you manage Eclipse Templates for the editor.

Eclipse Templates are small snippets of reusable code that can be automatically inserted into the editor. They act like a short form in which you fill with custom text. For more details on Eclipse Templates, refer to the article on [Teaching Templates](http://www.javalobby.org/forums/thread.jspa?threadID=15736).

# FAQ

### Why am I getting an out of memory error?

The Eclipse default memory settings are set in the eclipse.ini file located in the root of your Eclipse installation, and are as follows:

-vmargs  
-Xms40m  
-Xmx256m

This is ok for most users, but these settings can be tweaked by editing eclipse.ini file. This will give some more room for plugins (like the Soar IDE) that need additional memory.

To fix the problem, change the values to match the following:

-vmargs  
-Xms128m  
-Xmx512m

### A file says it doesn't belong to any agents

*   Make sure Include Soar Project Support is selected for the Eclipse project
*   Make sure there is at least one Soar Agent for that project
*   Make sure the file is selected in that .soaragent file
*   If you change any of these things while the file is open, it may not refresh until you close and reopen it

### The Source Viewer isn't show the expansion for my Tcl macro

*   Follow the above steps, making sure the file you are viewing and the file that defines the macro are both members of the agent
*   Make sure the Tcl expansion is enabled (toggle the icon on the top-right of the Source Viewer)
*   Make sure there are no outstanding syntax errors in the agent
*   Make sure you are selecting a call to a Tcl macro, not the Tcl macro definition
*   You may have to close/reopen the file, clean the agent, or restart Eclipse

### I don't see the Source Viewer, Datamap, etc

*   Make sure you are in the Soar Perspective
*   If they have become hidden, you can show individual views via Window->Show View->Other->Soar
*   You can reset the Soar Perspective (right-click on Soar Perspective -> Reset)

--------------

# License

Copyright (c) 2016, Soar Technology, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

* Neither the name of Soar Technology, Inc. nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY  *EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED   *WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.   *IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,   *INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT   *NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR   *PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,    *WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)   *ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE    *POSSIBILITY OF SUCH *DAMAGE.
