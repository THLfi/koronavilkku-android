#!/bin/dash

set -e

ls -Rla

sh -c "./gradlew --no-daemon -g=$GRADLE_HOME dependencyCheckAnalyze"

ls -Rla
