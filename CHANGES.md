# Change Log

This milestone introduces MultiDex support. The update site is improved to support correct installation of Andmore on Eclipse older then Luna.

## 0.5-M3

* (482776)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=482776] Allow installation of Andmore on Eclipse older than Luna 
* (479230)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=479230] Exporting an application package is not working when using multidex
* (476446)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=476446] MultiDex support for Android APK
* (471469)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=471469] Add Profile to use released MARs artifacts as part of the build
* (460132)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=460132] Fix license terms for dual licensed plugins 

## 0.5-M2

Milestone two provides some stability and usability improvements.  The biggest change is that the XML Editor no longer assumes it is the only editor.
Also the Database Perspective and browser have been fixed to show all the icons in the tree.

* (464542)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=464542] Migrate usages of KML2 to use XPP3
* (471901)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=471901] Android Common XML Editor should not be the default for all XML files
* (465702)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=465702] Android XML Editor should not try to be the default for non android XML files
* (471527)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=471527] Some wizards still open the Java perspective instead of the Android perspective
* (468473)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=468473] Android Database Explorer throws NPE when expanding tree

## 0.5-M1

This is the first public stable build for the Andmore project.  This release may have bugs and missing features but should be
useable for most every day development.


### Bugs/Features*

* (463598)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=463598] Importing a project created by ADT doesn't work (not right away, anyway)
* (463142)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=463142] Remove JCommon and JFreeChart-swt
* (462184)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=462184] Remove Usage Stats
* (461960)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=461960] Missing the copyright and applicable license on the header of some files
* (461547)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=461547] Andmore emulator launch asks to show content inside Eclipse
* (461460)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=461460] Android import in 'Other' category
* (461459)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=461459] Lint fails to run, blocking builds
* (461334)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=461334] Support Java 1.8 as an execution environment
* (460492)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=460492] Rename classes and internal strings to reflect changes from previous owners and projects
* (460482)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=460482] Android Logo Compliance
* (460134)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=460134] Top-level feature for installation
* (460038)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=460038] Android Compiler Compliance Level may be too Strict
* (459497)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=459497] Enable Code Coverage for Unit Tests
* (459407)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=459407] Please add project home page at GitHub metadata and in README
* (459363)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=459363] Fix Failing Unit and Integration Tests
* (456983)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=456983] Error when opening the Google Translate preference page
* (456970)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=456970] New Activity dialog does not work correct
* (456968)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=456968] Rename menu and preference panel items to be more accurate to their function
* (456966)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=456966] Change copyright strings in Andmore plugins
* (456965)[https://bugs.eclipse.org/bugs/show_bug.cgi?id=456965] Update icons to replace former MOTODEV images

### New and Noteworthy

*Android Development Perspective*

Android development now has it's own perspective, and no longer contributes functionality to the Java perspective.  This keeps Android functionality
from cluttering up the Java development when you are working with multiple projects.   There is also a Android Database perspective which provides
additional functionality when working with SQLite databases.

<img src="http://ibin.co/1yuJHqH40cgL" height="800" width="600"/>


*Maven Support via the M2E-Android project*

For Maven support, please install the latest M2E-Android tooling. The p2 update site is: http://rgladwell.github.io/m2e-android/updates/master

You can import an existing android maven project and m2e will handle configuration for the new plugins.


*JDK 1.8 project support*

Android projects may specify JDK 1.8 as the compiler level.  Creating new projects defaults to JDK 1.7, but projects may choose JDK 1.8.
this is necessary for some third party tooling that allows back porting of Java 8 features to Java 7.

<img src="http://ibin.co/1yuOVc1NQuF4"/>

*Convert ADT projects to Andmore*

You can convert your existing ADT projects and workspace to work with Andmore.  Select a project, and bring up the context menu, then select
*Configure->Convert to Andmore Project*.  The project will be updated to allow Andmore to recognize it as a Android project.   This currently
only works for existing Android Developer Tools projects, Android Studio projects that are using the old legacy directory structure should use
the *Import->Android->Existing Android Code into Workspace*.  Project support for Android Studio Projects is planned in an upcoming release.

<img src="http://ibin.co/1yuQ3SmODslo"/>

*Integration with Memory Analyzer and Database Development Tools*

Andmore provides the ability to analyze Dalvik Heap Dumps, and work directly with SQLite Databases via the Emulator.  Use the Android Database
perspective and have a running Emulator to access any SQLite Database and manage it directly.

*All the functionality of Android Developers Tools*

Since Andmore is a fork of the Google Android Developer Tools for Eclipse, all the existing functionality for work with your Android applications
is still there.  More is to come in the upcoming milestone releases.
