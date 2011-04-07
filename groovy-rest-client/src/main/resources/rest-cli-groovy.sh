#! /bin/bash

SCRIPT_LOCATION=$0
# Step through symlinks to find where the script really is
while [ -L "$SCRIPT_LOCATION" ]; do
  SCRIPT_LOCATION=`readlink -e "$SCRIPT_LOCATION"`
done

BIN_HOME=`dirname "$SCRIPT_LOCATION"`
LIB_HOME=$BIN_HOME/../lib

java -cp $LIB_HOME/*:$BIN_HOME/${pom.artifactId}-${pom.version}.jar RestCli $*