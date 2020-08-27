#!/bin/dash

set -e

ls -Rla /home

sh -c "./gradlew --no-daemon -g=$INPUT_GRADLE_HOME dependencyCheckAnalyze"

ls -Rla /home
