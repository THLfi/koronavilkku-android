#!/bin/dash

set -e

export GRADLE_USER_HOME="/home/gradle/gradle_home"

sh -c "gradle --no-daemon dependencyCheckAnalyze"
