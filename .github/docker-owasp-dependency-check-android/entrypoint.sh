#!/bin/dash

set -e

export GRADLE_USER_HOME="/root/gradle_home"

sh -c "gradle --no-daemon dependencyCheckAnalyze"
