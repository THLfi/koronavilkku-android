#!/bin/dash

set -e

sh -c "gradle --no-daemon -g=$INPUT_GRADLE_HOME dependencyCheckAnalyze"
