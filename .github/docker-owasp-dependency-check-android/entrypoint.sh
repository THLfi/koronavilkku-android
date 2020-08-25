#!/bin/dash

set -e

sh -c "gradle --no-daemon -g=/.gradle dependencyCheckAnalyze"
