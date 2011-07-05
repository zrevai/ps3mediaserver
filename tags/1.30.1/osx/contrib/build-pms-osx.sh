#!/bin/sh
#
# build-pms-osx.sh
#
# Version: 1.8.3
# Last updated: 2011-06-21
# Author: Patrick Atoon
#
#
# DESCRIPTION
#
# Building a statically linked PS3 Media Server for OSX is not an easy
# task and requires advanced knowledge of building the libraries and
# tools involved.
#
# This script will take care of building all that is required to build
# a statically linked PS3 Media Server for OSX.
# It will attempt to build universal binaries (for Intel and PCC)  where
# possible. However, some libraries do not support this and the script
# will compile for the current architecture.
#
# The script will compile the PS3 Media Server disk image file:
#
#    pms-macosx-arch-x.xx.x.dmg
#
# This script is provided as is. If it works for you, good! If it does
# not, try to figure out why and share your findings on the PS3 Media
# Server forums (http://www.ps3mediaserver.org/forum/)
#
#
# REQUIREMENTS
#
# Some Developer tools need to be installed manually. The script detects
# this and provides help. Other sources will be downloaded automatically.
#
#
# ACKNOWLEDGEMENTS
#
# Many thanks to the PS3 Media Server developers and forum moderators
# for keeping the project alive and the info coming. Special thanks to
# Adrian Stutz for sharing his findings of how to build a statically
# linked MPlayerOSX (http://www.mplayerosx.ch/), without his hard work
# this script would not have been possible. Also thanks to Redlum for
# his assistance in getting this script production ready.
#
#
# TODO
#
# - Clean up dependencies that are not needed for PMS
# - Fix fribidi
#
#
# COPYRIGHT
#
# This script is distributed under the Creative Commons Attribution 3.0
# (CC BY) license. This means you are free to copy, distribute, transmit
# and adapt it to suit your needs, as long as you mention the original
# author in your work.
# For more details, see http://creativecommons.org/licenses/by/3.0/
#
#
# CONFIGURATION
#
# Set FIXED_REVISIONS to "no" to check out the latest revisions.
# Default is "yes" to check out the last known working revision.
FIXED_REVISIONS="yes"

# Set TARGET_ARCHITECTURE for building binaries. Choose one of the following:
#
#    native: build for your own computer
#    x86_64: build for 64 bits Intel x86
#    i386: build for Intel 386
#    ppc: build for PowerPC
#
TARGET_ARCHITECTURE="i386"

# Set the amount of threads that are used for compiling everything. This
# should generally be the same as the amount of CPU cores in your computer
# (look for Total Number Of Cores in System Profiler if you don't know it).
THREADS="2"

# It should not be necessary to change anything below this line
##########################################

# Binaries
ANT=/usr/bin/ant
CURL=/usr/bin/curl
GCC=/usr/bin/gcc
GIT=/usr/local/git/bin/git
HDID=/usr/bin/hdid
HDIUTIL=/usr/bin/hdiutil
MAKE=/usr/bin/make
SED=/usr/bin/sed
SVN=/usr/bin/svn
TAR=/usr/bin/tar
YASM=/usr/local/bin/yasm
UNZIP=/usr/bin/unzip

##########################################
# Create a directory when it does not exist
#
createdir() {
    if [ ! -d $1 ]; then
        mkdir $1
    fi
}


##########################################
# Set the compiler flags to determine the architecture to compile for
# Optional parameter: architecture flags string to replace the default,
# e.g. "-arch ppc -faltivec -mcpu=7450"
#
set_flags() {
    # Minimum OS version as target
    export MACOSX_DEPLOYMENT_TARGET=10.5
    export CFLAGS="-mmacosx-version-min=10.5 -isystem /Developer/SDKs/MacOSX10.5.sdk"
    export LDFLAGS="-mmacosx-version-min=10.5 -isysroot /Developer/SDKs/MacOSX10.5.sdk -Wl,-syslibroot,/Developer/SDKs/MacOSX10.5.sdk"
    export CXXFLAGS="-mmacosx-version-min=10.5 -isysroot /Developer/SDKs/MacOSX10.5.sdk"

    if [ "$1" != "" ]; then
        # Use the supplied parameter string for architecture flags
        export CFLAGS="$CFLAGS $1"
        export LDFLAGS="$LDFLAGS $1"
        export CXXFLAGS="$CXXFLAGS $1"
    else
        # Uncomment for single architecture binary
        export CFLAGS="$CFLAGS -arch $ARCHITECTURE"
        export LDFLAGS="$LDFLAGS -arch $ARCHITECTURE"
        export CXXFLAGS="$CXXFLAGS -arch $ARCHITECTURE"
    fi

    # Paths of the build environment
    export LDFLAGS="$LDFLAGS -L$TARGET/lib -Wl,-search_paths_first"
    export CFLAGS="$CFLAGS -I$TARGET/include"
    export CXXFLAGS="$CXXFLAGS -I$TARGET/include"
}


##########################################
# Initialize building environment
#
initialize() {
    WORKDIR=`pwd`

    # Directories for statically compiled libraries
    TARGET="$WORKDIR/target"
    SRC="$WORKDIR/src"
    createdir "$SRC"
    createdir "$TARGET"

    if [ "$TARGET_ARCHITECTURE" == "native" ]; then
        ARCHITECTURE=`/usr/bin/uname -p`
    else
        ARCHITECTURE=$TARGET_ARCHITECTURE
    fi

    # Set default compiler flags
    set_flags

    # Reset paths for compiling
    export PATH="$TARGET/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/usr/X11/bin"
    export PKG_CONFIG_PATH=""
}


##########################################
# Check for gcc, make, svn, ant and curl
#
check_xcode() {
    if [ ! -x $GCC -o ! -x $SVN -o ! -x $ANT -o ! -x $CURL -o ! -x $MAKE ]; then
        cat << EOM
It seems you are missing Xcode from Apple, which is required to run this script.

Please go to http://developer.apple.com/technologies/xcode.html, create a free
Apple developer account and download Xcode and install it.
EOM
        exit;
    fi
}


##########################################
# Check for yasm
#
check_yasm() {
    if [ ! -x $YASM ]; then
        cat << EOM
It seems you are missing "yasm", which is required to run this script.
Please run the following commands to install "yasm":

    $SVN checkout http://www.tortall.net/svn/yasm/trunk/yasm yasm
    cd yasm
    ./autogen.sh
    ./configure
    make
    sudo make install
    cd ..

EOM
        exit;
    fi
}


##########################################
# Check for git
#
check_git() {
    if [ ! -x $GIT ]; then
        cat << EOM
It seems you are missing "git", which is required to run this script.
Please go to http://code.google.com/p/git-osx-installer/, download git
and install it.
EOM
        exit;
    fi
}


##########################################
# Check for jarbundler
#
check_jarbundler() {
    # See if the jar file exists
    ls /usr/share/ant/lib/jarbundler-*.jar > /dev/null 2>&1

    if [ "$?" == "1" ]; then
        cat << EOM
It seems you are missing "jarbundler", which is required to run this script.
Please go to http://www.informagen.com/JarBundler/ , download the jarbundler
and install it.
EOM
        exit;
    fi
}


##########################################
# Exit if the previous command ended with an error status
#
exit_on_error() {
    if [ "$?" != "0" ]; then
        echo Fatal error occurred, aborting build.
        cd $WORKDIR
        exit
    fi
}


##########################################
# Building start marker to more easily follow the build process
#
start_build() {
    cat << EOM


--------------------------------------------------------------------------------------
Building $1
--------------------------------------------------------------------------------------

EOM
}



##########################################
# DCRAW
# http://www.cybercom.net/~dcoffin/dcraw/
#
build_dcraw() {
    start_build dcraw
    cd $SRC

    if [ ! -d dcraw-9.07 ]; then
        $CURL -L http://www.cybercom.net/~dcoffin/dcraw/archive/dcraw-9.07.tar.gz > dcraw-9.07.tar.gz
        exit_on_error
        $TAR xzf dcraw-9.07.tar.gz -s /dcraw/dcraw-9.07/
    fi

    cd dcraw-9.07
    set_flags
    $GCC -O4 -o dcraw dcraw.c -lm -ljpeg -DNO_LCMS $CFLAGS -L$TARGET/lib
    exit_on_error
    createdir $TARGET/bin
    cp dcraw $TARGET/bin
    cd $WORKDIR
}


##########################################
# EXPAT
# http://expat.sourceforge.net/
#
build_expat() {
    start_build expat
    cd $SRC

    if [ ! -d expat-2.0.1 ]; then
        $CURL -L http://downloads.sourceforge.net/project/expat/expat/2.0.1/expat-2.0.1.tar.gz > expat-2.0.1.tar.gz
        exit_on_error
        $TAR xzf expat-2.0.1.tar.gz
    fi

    cd expat-2.0.1
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# FAAD2
# http://www.audiocoding.com/faad2.html
#
build_faad2() {
    start_build faad2
    cd $SRC

    if [ ! -d faad2-2.7 ]; then
        $CURL -L http://downloads.sourceforge.net/project/faac/faad2-src/faad2-2.7/faad2-2.7.tar.gz > faad2-2.7.tar.gz
        exit_on_error
        $TAR xzf faad2-2.7.tar.gz
    fi

    cd faad2-2.7
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# FLAC
# http://flac.sourceforge.net/
#
build_flac() {
    start_build flac
    cd $SRC

    if [ ! -d flac-1.2.1 ]; then
        $CURL -L http://downloads.xiph.org/releases/flac/flac-1.2.1.tar.gz > flac-1.2.1.tar.gz
        exit_on_error
        $TAR xzf flac-1.2.1.tar.gz
    fi

    cd flac-1.2.1
    set_flags

    if [ "$ARCHITECTURE" == "x86_64" ]; then
        ./configure --disable-shared --disable-dependency-tracking --host=x86-apple-darwin10 --prefix=$TARGET
    else
        ./configure --disable-shared --disable-dependency-tracking --disable-asm-optimizations --prefix=$TARGET
    fi

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# FONTCONFIG
# http://fontconfig.org/wiki/
#
build_fontconfig() {
    start_build fontconfig
    cd $SRC

    if [ ! -d fontconfig-2.8.0 ]; then
        $CURL -L http://www.freedesktop.org/software/fontconfig/release/fontconfig-2.8.0.tar.gz > fontconfig-2.8.0.tar.gz
        exit_on_error
        $TAR xzf fontconfig-2.8.0.tar.gz
    fi

    cd fontconfig-2.8.0
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# FREETYPE
# http://www.freetype.org/
#
build_freetype() {
    start_build freetype
    cd $SRC

    if [ ! -d freetype-2.4.4 ]; then
        $CURL -L http://download.savannah.gnu.org/releases/freetype/freetype-2.4.4.tar.gz > freetype-2.4.4.tar.gz
        exit_on_error
        $TAR xzf freetype-2.4.4.tar.gz
    fi

    cd freetype-2.4.4
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# FRIBIDI
# http://fribidi.org/
#
build_fribidi() {
    start_build fribidi
    cd $SRC

    if [ ! -d fribidi-0.19.2 ]; then
        $CURL -L http://fribidi.org/download/fribidi-0.19.2.tar.gz > fribidi-0.19.2.tar.gz
        exit_on_error
        $TAR xzf fribidi-0.19.2.tar.gz
    fi

    cd fribidi-0.19.2
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# GIFLIB
# http://sourceforge.net/projects/giflib/
#
build_giflib() {
    start_build giflib
    cd $SRC

    if [ ! -d giflib-4.1.6 ]; then
        $CURL -L http://downloads.sourceforge.net/project/giflib/giflib%204.x/giflib-4.1.6/giflib-4.1.6.tar.bz2 > giflib-4.1.6.tar.bz2
        exit_on_error
        $TAR xjf giflib-4.1.6.tar.bz2
    fi

    cd giflib-4.1.6
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# ICONV
# http://www.gnu.org/software/libiconv/
#
build_iconv() {
    start_build iconv
    cd $SRC

    if [ ! -d libiconv-1.13.1 ]; then
        $CURL -L http://ftp.gnu.org/pub/gnu/libiconv/libiconv-1.13.1.tar.gz > libiconv-1.13.1.tar.gz
        exit_on_error
        $TAR xzf libiconv-1.13.1.tar.gz
    fi

    cd libiconv-1.13.1
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# JPEG
# http://www.ijg.org/
#
build_jpeg() {
    start_build jpeg
    cd $SRC

    if [ ! -d jpeg-8c ]; then
        $CURL -L http://www.ijg.org/files/jpegsrc.v8c.tar.gz > jpegsrc.v8c.tar.gz
        exit_on_error
        $TAR xzf jpegsrc.v8c.tar.gz
    fi

    cd jpeg-8c
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LAME
# http://lame.sourceforge.net/
#
build_lame() {
    start_build lame
    cd $SRC

    if [ ! -d lame-3.98.4 ]; then
        $CURL -L http://downloads.sourceforge.net/project/lame/lame/3.98.4/lame-3.98.4.tar.gz > lame-3.98.4.tar.gz
        exit_on_error
        $TAR xzf lame-3.98.4.tar.gz
    fi

    cd lame-3.98.4
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBDCA
# http://www.videolan.org/developers/libdca.html
#
build_libdca() {
    start_build libdca
    cd $SRC

    if [ ! -d libdca-0.0.5 ]; then
        $CURL -L http://download.videolan.org/pub/videolan/libdca/0.0.5/libdca-0.0.5.tar.bz2 > libdca-0.0.5.tar.bz2
        exit_on_error
        $TAR xjf libdca-0.0.5.tar.bz2
    fi

    cd libdca-0.0.5
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBDV
# http://libdv.sourceforge.net/
#
build_libdv() {
    start_build libdv
    cd $SRC

    if [ ! -d libdv-1.0.0 ]; then
        $CURL -L http://downloads.sourceforge.net/project/libdv/libdv/1.0.0/libdv-1.0.0.tar.gz > libdv-1.0.0.tar.gz
        exit_on_error
        $TAR xzf libdv-1.0.0.tar.gz
    fi

    cd libdv-1.0.0
    set_flags
    export LDFLAGS="$LDFLAGS -flat_namespace -undefined suppress"
    ./configure --disable-shared --disable-dependency-tracking --disable-xv \
        --disable-gtk --disable-sdl --disable-asm --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBDVDCSS
# http://www.videolan.org/developers/libdvdcss.html
#
build_libdvdcss() {
    start_build libdvdcss
    cd $SRC

    if [ ! -d libdvdcss-1.2.9 ]; then
        $CURL -L http://download.videolan.org/pub/libdvdcss/1.2.9/libdvdcss-1.2.9.tar.gz > libdvdcss-1.2.9.tar.gz
        exit_on_error
        $TAR xzf libdvdcss-1.2.9.tar.gz
    fi

    cd libdvdcss-1.2.9
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBDVDNAV
# svn://svn.mplayerhq.hu/dvdnav/trunk/libdvdnav/
#
build_libdvdnav() {
    start_build libdvdnav
    cd $SRC

    if [ "$FIXED_REVISIONS" == "yes" ]; then
        REVISION="-r 1226"
    else
        REVISION=""
    fi

    if [ ! -d libdvdnav ]; then
        $SVN checkout $REVISION svn://svn.mplayerhq.hu/dvdnav/trunk/libdvdnav/ libdvdnav
        exit_on_error
        cd libdvdnav
    else
        cd libdvdnav
        $SVN update $REVISION
        exit_on_error
    fi

    set_flags
    ./autogen.sh --with-dvdread-config=$TARGET/bin/dvdread-config --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}

##########################################
# LIBDVDREAD
# svn://svn.mplayerhq.hu/dvdnav/trunk/libdvdread/
#
build_libdvdread() {
    start_build libdvdread
    cd $SRC

    if [ "$FIXED_REVISIONS" == "yes" ]; then
        REVISION="-r 1226"
    else
        REVISION=""
    fi

    if [ ! -d libdvdread ]; then
        $SVN checkout $REVISION svn://svn.mplayerhq.hu/dvdnav/trunk/libdvdread/ libdvdread
        exit_on_error
        cd libdvdread
    else
        cd libdvdread
        $SVN update $REVISION
        exit_on_error
    fi

    set_flags
    ./autogen.sh --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBMAD
# http://www.underbit.com/products/mad/
#
build_libmad() {
    start_build libmad
    cd $SRC

    if [ ! -d libmad-0.15.1b ]; then
        $CURL -L ftp://ftp.mars.org/pub/mpeg/libmad-0.15.1b.tar.gz > libmad-0.15.1b.tar.gz
        exit_on_error
        $TAR xzf libmad-0.15.1b.tar.gz
    fi

    cd libmad-0.15.1b
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBMEDIAINFO
# http://sourceforge.net/projects/mediainfo/
#
build_libmediainfo() {
    start_build libmediainfo
    cd $SRC

    if [ ! -d libmediainfo_0.7.44 ]; then
        $CURL -L http://downloads.sourceforge.net/project/mediainfo/source/libmediainfo/0.7.44/libmediainfo_0.7.44.tar.bz2 > libmediainfo_0.7.44.tar.bz2
        exit_on_error
        $TAR xjf libmediainfo_0.7.44.tar.bz2 -s /MediaInfoLib/libmediainfo_0.7.44/
    fi

    cd libmediainfo_0.7.44
    cd Project/GNU/Library
    export CFLAGS=
    export LDFLAGS=
    export CXXFLAGS=

    # Note: libmediainfo requires libzen source to compile
    ./autogen
    ./configure --enable-arch-i386 --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBPNG
# http://www.libpng.org/pub/png/libpng.html
#
build_libpng() {
    start_build libpng
    cd $SRC

    if [ ! -d libpng-1.5.2 ]; then
        $CURL -L http://downloads.sourceforge.net/project/libpng/libpng15/1.5.2/libpng-1.5.2.tar.gz > libpng-1.5.2.tar.gz
        exit_on_error
        $TAR xzf libpng-1.5.2.tar.gz
    fi

    cd libpng-1.5.2
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBOGG
# http://xiph.org/downloads/
#
build_libogg() {
    start_build libogg
    cd $SRC

    if [ ! -d libogg-1.2.2 ]; then
        $CURL -L http://downloads.xiph.org/releases/ogg/libogg-1.2.2.tar.gz > libogg-1.2.2.tar.gz
        exit_on_error
        $TAR xzf libogg-1.2.2.tar.gz
    fi

    cd libogg-1.2.2
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBVORBIS
# http://xiph.org/downloads/
#
build_libvorbis() {
    start_build libvorbis
    cd $SRC

    if [ ! -d libvorbis-1.3.2 ]; then
        $CURL -L http://downloads.xiph.org/releases/vorbis/libvorbis-1.3.2.tar.gz > libvorbis-1.3.2.tar.gz
        exit_on_error
        $TAR xzf libvorbis-1.3.2.tar.gz
    fi

    cd libvorbis-1.3.2
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --with-ogg=$TARGET --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBTHEORA
# http://xiph.org/downloads/
#
build_libtheora() {
    start_build libtheora
    cd $SRC

    if [ ! -d libtheora-1.1.1 ]; then
        $CURL -L http://downloads.xiph.org/releases/theora/libtheora-1.1.1.tar.bz2 > libtheora-1.1.1.tar.bz2
        exit_on_error
        $TAR xjf libtheora-1.1.1.tar.bz2
    fi

    cd libtheora-1.1.1
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --with-ogg=$TARGET \
        --with-vorbis=$TARGET --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBZEN
# http://sourceforge.net/projects/zenlib/
#
build_libzen() {
    start_build libzen
    cd $SRC

    if [ ! -d libzen_0.4.19 ]; then
        $CURL -L http://downloads.sourceforge.net/project/zenlib/ZenLib%20-%20Sources/0.4.19/libzen_0.4.19.tar.bz2 > libzen_0.4.19.tar.bz2
        exit_on_error
        $TAR xjf libzen_0.4.19.tar.bz2 -s /ZenLib/libzen_0.4.19/

        # libmediainfo needs this
        ln -s libzen_0.4.19 ZenLib
    fi

    cd libzen_0.4.19
    cd Project/GNU/Library
    export CFLAGS=
    export LDFLAGS=
    export CXXFLAGS=
    ./autogen
    ./configure --enable-arch-i386 --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LZO2
# http://www.oberhumer.com/opensource/lzo/
#
build_lzo2() {
    start_build lzo2
    cd $SRC

    if [ ! -d lzo-2.04 ]; then
        $CURL -L http://www.oberhumer.com/opensource/lzo/download/lzo-2.04.tar.gz > lzo-2.04.tar.gz
        exit_on_error
        $TAR xzf lzo-2.04.tar.gz
    fi

    cd lzo-2.04
    set_flags

    if [ "$ARCHITECTURE" == "i386" ] || [ "$ARCHITECTURE" == "x86_64" ] ; then
        ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    else
        ./configure --disable-shared --disable-dependency-tracking --disable-asm --prefix=$TARGET
    fi

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# NCURSES
# http://www.gnu.org/software/ncurses/
#
build_ncurses() {
    start_build ncurses
    cd $SRC

    if [ ! -d ncurses-5.9 ]; then
        $CURL -L http://ftp.gnu.org/pub/gnu/ncurses/ncurses-5.9.tar.gz > ncurses-5.9.tar.gz
        exit_on_error
        $TAR xzf ncurses-5.9.tar.gz
    fi

    cd ncurses-5.9
    set_flags
    ./configure --without-shared --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE libs
    exit_on_error
    $MAKE install.libs
    cd $WORKDIR
}


##########################################
# TSMUXER
# http://www.smlabs.net/en/products/tsmuxer/
# http://www.videohelp.com/tools/tsMuxeR
# Interesting Open Source followup project in development: https://github.com/kierank/libmpegts
#
build_tsMuxeR() {
    start_build tsMuxeR
    cd $SRC

    if [ ! -d tsMuxeR_1.10.6 ]; then
        $CURL -H "Referer: http://www.videohelp.com/tools/tsMuxeR" -L http://www.videohelp.com/download/tsMuxeR_1.10.6.dmg > tsMuxeR_1.10.6.dmg
        exit_on_error
        createdir tsMuxeR_1.10.6
    fi

    # Nothing to build. Just open the disk image, copy the binary and detach the disk image
    $HDID tsMuxeR_1.10.6.dmg
    exit_on_error
    cp -f /Volumes/tsMuxeR/tsMuxerGUI.app/Contents/MacOS/tsMuxeR tsMuxeR_1.10.6/tsMuxeR
    cp -f tsMuxeR_1.10.6/tsMuxeR $TARGET/bin
    $HDIUTIL detach /Volumes/tsMuxeR
    cd $WORKDIR
}


##########################################
# X264
# svn://svn.videolan.org/x264/trunk
#
build_x264() {
    start_build x264
    cd $SRC

    if [ -d x264 ]; then
        cd x264
        $GIT pull git://git.videolan.org/x264.git
        exit_on_error
    else
        $GIT clone git://git.videolan.org/x264.git x264
        exit_on_error
        cd x264
    fi

    if [ "$FIXED_REVISIONS" == "yes" ]; then
        $GIT checkout "`$GIT rev-list master -n 1 --first-parent --before=2011-04-24`"
        exit_on_error
    fi

    set_flags

    if [ "$ARCHITECTURE" == "x86_64" ]; then
        ./configure --prefix=$TARGET
    else
        ./configure --prefix=$TARGET --host=x86-apple-darwin10 --disable-asm
    fi

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install install-lib-static

    cd $WORKDIR
}


##########################################
# XVID
# http://www.xvid.org/
#
build_xvid() {
    start_build xvid
    cd $SRC

    if [ ! -d xvidcore-1.3.1 ]; then
        $CURL -L http://downloads.xvid.org/downloads/xvidcore-1.3.1.tar.gz > xvidcore-1.3.1.tar.gz
        exit_on_error
        $TAR xzf xvidcore-1.3.1.tar.gz -s /xvidcore/xvidcore-1.3.1/
    fi

    cd xvidcore-1.3.1/build/generic
    set_flags

    if [ "$ARCHITECTURE" == "x86_64" ]; then
        ./configure --prefix=$TARGET --host=x86-apple-darwin10
    else
        ./configure --prefix=$TARGET
    fi

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install

    # Remove dynamic libraries
    rm -f $TARGET/lib/libxvidcore*.dylib

    cd $WORKDIR
}


##########################################
# ZLIB
# http://zlib.net/
#
build_zlib() {
    start_build zlib
    cd $SRC

    if [ ! -d zlib-1.2.5 ]; then
        $CURL -L http://zlib.net/zlib-1.2.5.tar.gz > zlib-1.2.5.tar.gz
        exit_on_error
        $TAR xzf zlib-1.2.5.tar.gz
    fi

    cd zlib-1.2.5
    set_flags
    ./configure --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install

    # Remove dynamic libraries
    rm -f $TARGET/lib/libz*.dylib
    cd $WORKDIR
}


##########################################
# FFMPEG
# http://www.ffmpeg.org/
#
build_ffmpeg() {
    start_build ffmpeg
    cd $SRC
    if [ -d ffmpeg ]; then
        cd ffmpeg
        $GIT pull git://git.videolan.org/ffmpeg.git
        exit_on_error
    else
        $GIT clone git://git.videolan.org/ffmpeg.git ffmpeg
        exit_on_error
        cd ffmpeg
    fi

    if [ "$FIXED_REVISIONS" == "yes" ]; then
        $GIT checkout "`$GIT rev-list master -n 1 --first-parent --before=2011-06-21`"
        exit_on_error
    fi

    # Fix path to git in "version.sh" to avoid version "UNKNOWN"
    GIT_STR=`echo $GIT | $SED -e "s/\//\\\\\\\\\\\//g"`
    $SED -i -e "s/ git / $GIT_STR /g" version.sh

    set_flags

    # Theora/vorbis disabled for mplayer, also disabled here to avoid build errors
    ./configure --enable-gpl --enable-libmp3lame --enable-libx264 --enable-libxvid \
              --disable-libtheora --disable-libvorbis --disable-shared --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}



##########################################
# MPLAYER
# http://www.mplayerhq.hu/design7/news.html
#
build_mplayer() {
    start_build mplayer
    cd $SRC

    if [ "$FIXED_REVISIONS" == "yes" ]; then
        REVISION="-r 33685"
    else
        REVISION=""
    fi

    if [ -d mplayer ]; then
        cd mplayer
        $SVN update $REVISION
        exit_on_error
    else
        $SVN checkout $REVISION svn://svn.mplayerhq.hu/mplayer/trunk mplayer
        exit_on_error
        cd mplayer
    fi

    # Copy ffmpeg source to avoid making another git clone by configure
    rm -rf ffmpeg
    cp -rf $SRC/ffmpeg .

    set_flags

    # Extra flags for compiling mplayer
    export CFLAGS="-O4 -fomit-frame-pointer -pipe $CFLAGS"
    export CXXFLAGS="-O4 -fomit-frame-pointer -pipe $CXXFLAGS"

    # Fribidi, theora and vorbis support seems broken in this revision, disable it for now
    ./configure --disable-x11 --disable-gl --disable-qtx --disable-dvdread-internal \
              --disable-fribidi --disable-theora --disable-libvorbis \
              --with-freetype-config=$TARGET/bin/freetype-config --prefix=$TARGET

    # Somehow -I/usr/X11/include still made it into the config.mak, regardless of the --disable-x11
    $SED -i -e "s/-I\/usr\/X11\/include//g" config.mak

   # Fix fribidi regression (http://lists.mplayerhq.hu/pipermail/mplayer-users/2011-May/082649.html)
    $SED -i -e "s/#ifdef CONFIG_FRIBIDI/#if defined(CONFIG_FRIBIDI) \&\& \!defined(CODECS2HTML)/g" sub/subreader.h

    # Remove the ffmpeg directory and copy the compiled ffmpeg again to avoid "make" rebuilding it
    rm -rf ffmpeg
    cp -rf $SRC/ffmpeg .

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}

##########################################
# PS3MEDIASERVER
# http://code.google.com/p/ps3mediaserver/
#
build_ps3mediaserver() {
    start_build ps3mediaserver
    cd $SRC

    if [ "$FIXED_REVISIONS" == "yes" ]; then
        REVISION="-r 626" # To include some important OS X fixes
    else
        REVISION=""
    fi

    if [ -d ps3mediaserver ]; then
        cd ps3mediaserver
        $SVN update $REVISION
        exit_on_error
    else
        $SVN checkout $REVISION http://ps3mediaserver.googlecode.com/svn/trunk/ps3mediaserver ps3mediaserver
        exit_on_error
        cd ps3mediaserver
    fi

    cd osx
   
    # Overwrite with the home built tools
    cp $TARGET/bin/dcraw .
    cp $TARGET/bin/ffmpeg .
    cp $TARGET/bin/flac .
    cp $TARGET/bin/mplayer .
    cp $TARGET/bin/mencoder .
    # Mencoder_mt is only needed for older revisions of mplayer, the latest revisions include multithreading by default
    #cp $TARGET/bin/mencoder_mt .
    cp $TARGET/bin/tsMuxeR .

    set_flags
    $ANT DMG
    exit_on_error

    # Add the architecture name to the final file
    PMS_FILENAME_ORIG=`ls pms-macosx-*.dmg | head -1`
    PMS_FILENAME_NEW=`echo $PMS_FILENAME_ORIG | $SED -e "s/-macosx-/-macosx-$ARCHITECTURE-/"`
    mv -f $PMS_FILENAME_ORIG $PMS_FILENAME_NEW
    cp $PMS_FILENAME_NEW $WORKDIR
    cd $WORKDIR
}


##########################################
# Finally, execute the script...
#

# Check requirements
check_xcode
check_yasm
check_git
check_jarbundler

# Initialize variables for compiling
initialize

# Build static libraries to link against
build_zlib
build_expat
build_faad2
build_fontconfig
build_freetype
build_fribidi
build_giflib
build_jpeg
build_iconv
build_ncurses
build_lame
build_libdca
build_libdv
build_libdvdcss
build_libdvdread
build_libdvdnav
build_libmad
build_libzen
# Note: libmediainfo requires libzen to build
build_libmediainfo
build_libpng
build_libogg
build_libvorbis
build_libtheora
build_lzo2
build_x264
build_xvid

# Build tools for including with PS3 Media Server
build_flac
build_dcraw
build_tsMuxeR
build_ffmpeg
build_mplayer

# Build PS3 Media Server itself
build_ps3mediaserver
