#!/bin/dash

set -e

ls -Rla /home

sh -c "./gradlew --no-daemon -g=$GRADLE_HOME dependencyCheckAnalyze"

ls -Rla /home
