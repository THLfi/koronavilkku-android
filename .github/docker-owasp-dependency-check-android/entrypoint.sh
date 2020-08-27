#!/bin/dash

set -e

sh -c "gradle --no-daemon -g='$PWD/$INPUT_GRADLE_HOME' dependencyCheckAnalyze"
