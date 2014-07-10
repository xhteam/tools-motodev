# Contributing

Proteus is based off the open source plugins from MotoDev Studio.  This is a fork of those plugins.   The
goal is to enhance the Google ADT plugins to provide additional features and functionality that ADT currently
does not provide.

This is an open source community lead project, so contributions are encouraged.  All code contributions will
be licensed under an Eclipse Public License.

The following describes how to build Proteus and bring the source code into an existing eclipse environment to development and improve.

## Building from the Command Line

To compile.  You need the following:

1. Maven 3.x installed.

Onece Maven has been installed, you are ready to build.

2. Change to the _src_ directory.
3. mvn clean install

Running the above will compile all features and plugins.  It will also generate a p2 update
site that can be used to install the plugins.  Currently the Basic and MotoDev Studio features
are generated in the p2 site.

The site location is:

src/site/target/repository

You can add this as a local repository for Eclipse to install from.

This will also build all supported platform versions of the full IDE as well.  These can be found in the directory 

src/site/target/products

Windows, MacOSX, and Linux builds are available.

## Creating a Target Platform

The easiest way to create a target platform to use for development with Eclipse is to
download an appropriate Binary build of the IDE that has all necessary plugins installed already.

https://drive.google.com/folderview?id=0B6ggpSoYBC_HbWtYdEJ0cjFTUkU#list

The above url contains various binary builds of the IDE.  Download one of those, and unzip to
where you want it installed.

Then within Eclipse, under the Preferences->Plugin Development->Target Platform.  Create a new TargetPlatform,
from an installation directory.   Point to where you have unzipped the above installation, and save the target
platform configuration.  Make sure to set the new Target Platform as the Active platform.

You are now ready to work on the plugins for Proteus.

## Importing the Source Code

There are two ways to do this both involve checking out the source code from the git repository on the tycho branch.

I'm assuming you are experienced with using git since you cloned the project from github or forked it from there.

You can import the plugins as Existing Maven Projects or as Eclipse Projects.  Either way will work.   

Make sure you have your target platform set as described above, and you should be able to work on the code.

