#!/bin/dash

set -e


sh -c "gradle --no-daemon dependencyCheckAnalyze"
