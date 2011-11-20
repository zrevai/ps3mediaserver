Compiling PS3 Media Server on OSX
---------------------------------

This directory contains the instructions and binaries to compile PS3 Media
Server on OSX. Building PMS will result in a "pms-macosx-x.x.x.dmg" disk
image file.

Before you can compile PMS, you need to install Xcode from Apple first.
Xcode contains all build tools needed to compile PMS. If you do not have
it yet, go to http://developer.apple.com/technologies/xcode.html, create a
free Apple developer account and download Xcode and install it.

Once Xcode is installed, open a Terminal and change directory to the "osx"
directory where you found this document.

    cd ps3mediaserver/osx

Then you can use "ant" to build the disk image:

    ant DMG

This will create a new file "pms-macosx-x.x.x.dmg". Open this file in the
Finder and install PMS on your computer.
