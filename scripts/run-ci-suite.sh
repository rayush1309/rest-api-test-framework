#!/usr/bin/env bash
set -uo pipefail

suite="${1:?Usage: run-ci-suite.sh <smoke|regression|e2e|mocking>}"
case "$suite" in
  smoke|regression|e2e|mocking) ;;
  *) echo "Unknown CI suite: $suite" >&2; exit 2 ;;
esac

environment="${TEST_ENVIRONMENT:-${ENVIRONMENT:-qa}}"
set +e
mvn --batch-mode test -Denv="$environment" -DsuiteXmlFile="suites/ci-$suite.xml"
test_status=$?
set -e

report_directory="target/ci-surefire-reports/$suite"
rm -rf "$report_directory"
mkdir -p "$report_directory"
if [ -d target/surefire-reports ]; then
  cp -R target/surefire-reports/. "$report_directory/"
fi

exit "$test_status"
