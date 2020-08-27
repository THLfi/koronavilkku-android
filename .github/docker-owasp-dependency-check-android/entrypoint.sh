#!/bin/dash

set -e

sh -c "./gradlew --no-daemon -g='$PWD/$INPUT_GRADLE_HOME' dependencyCheckAnalyze"
