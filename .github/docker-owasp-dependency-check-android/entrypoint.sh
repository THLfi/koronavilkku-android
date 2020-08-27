#!/bin/dash

set -e


sh -c "./gradlew --no-daemon -g=$GRADLE_HOME dependencyCheckAnalyze"
