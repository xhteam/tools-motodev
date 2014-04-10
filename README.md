To compile.  You need the following:

1. A p2 generated site of the ADT plugins. 
    See https://wiki.eclipse.org/Tycho/Additional_Tools#mirror_goal.

2. Maven 3.x installed

You will need to generate a p2 site, and set the android-adt property in the build to the
location where you have the p2 site stored.

mvn clean install -Dandroid-adt=file:///Users/username/adt-site/target/repository

Running the above will compile all features and plugins.

Eclipse Development.

Your target platform needs to have the following installed:

1. Eclipse Database Plugins
2. Eclipse Sequoyah 2.1
3. Eclipse MAT
4. Java J2EE or Eclipse Plugin Development SDK with WTP SDK.
5. Latest Android Development Tools

