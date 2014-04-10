To compile.  You need the following:

1. Maven 3.x installed.
2. A p2 generated site of the ADT plugins. 
   a. cd makefile/adtp2
   b. mvn clean install

This will generate the necessary p2 update site of the Android Development Tools for 
the build to use later.

3. Change the src directory.
4. mvn clean install

Running the above will compile all features and plugins.  It will also generate a p2 update
site that can be used to install the plugins.  Currently the Basic and MotoDev Studio features
are generated in the p2 site.

The site location is:

src/site/target/repository

You can add this as a local repository for Eclipse to install from.

Eclipse Development.

Your target platform needs to have the following installed:

1. Eclipse Database Plugins
2. Eclipse Sequoyah 2.1
3. Eclipse MAT
4. Java J2EE or Eclipse Plugin Development SDK with WTP SDK.
5. Latest Android Development Tools

