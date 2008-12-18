#!/bin/sh

DIRNAME=`dirname $0`

# Setup PMS_HOME
if [ "x$PMS_HOME" = "x" ]; then
	PMS_HOME=`cd $DIRNAME/; pwd`
fi
export PMS_HOME

# Setup the JVM and make sure Java 6 is used
JAVA="/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Commands/java"

# Setup the classpath
PMS_CLASSPATH=".:$PMS_HOME/lib/pms.jar:$PMS_HOME/lib/commons-logging-api-1.0.4.jar:$PMS_HOME/lib/nanoxml-2.2.3.jar:$PMS_HOME/lib/java-unrar-0.2.jar:$PMS_HOME/lib/entagged-cvs.jar:$PMS_HOME/lib/forms-1.2.1.jar:$PMS_HOME/lib/looks-2.2.1.jar:$PMS_HOME/lib/jna.jar"

# Execute the JVM
exec "$JAVA" $JAVA_OPTS -Xmx768M -Djava.encoding=UTF-8 -classpath $PMS_CLASSPATH net.pms.PMS "$@"

