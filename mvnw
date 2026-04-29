#!/bin/sh
# Maven Wrapper Script
MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_PROJECTBASEDIR="$(pwd)"

if [ -f "$MAVEN_WRAPPER_JAR" ]; then
  exec java -jar "$MAVEN_WRAPPER_JAR" "$@"
else
  exec mvn "$@"
fi
