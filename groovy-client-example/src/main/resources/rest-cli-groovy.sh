#! /bin/bash

SCRIPT_LOCATION=$0
# Step through symlinks to find where the script really is
while [ -L "$SCRIPT_LOCATION" ]; do
  SCRIPT_LOCATION=`readlink -e "$SCRIPT_LOCATION"`
done

BIN_HOME=`dirname "$SCRIPT_LOCATION"`
LIB_HOME=$BIN_HOME/../lib

CLASSPATH=$BIN_HOME/groovy-client-example-${version}.jar
for i in `ls $LIB_HOME/`;
do
    CLASSPATH=$CLASSPATH:$LIB_HOME/$i
done

java -cp $CLASSPATH RestCli $*