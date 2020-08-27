#!/bin/dash

set -e

ls -Rla

sh -c "./gradlew --no-daemon -g=$PWD/$INPUT_GRADLE_HOME dependencyCheckAnalyze"
