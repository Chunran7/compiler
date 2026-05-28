#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo "========================================"
echo "Running SeuYacc / IR Maven tests"
echo "Project directory: $ROOT"
echo "========================================"

if ! command -v mvn >/dev/null 2>&1; then
  echo "ERROR: Maven is not installed or not found in PATH."
  echo "Please install Maven or configure IntelliJ IDEA terminal PATH."
  exit 1
fi

echo
echo "Maven version:"
mvn -version

echo
echo "Cleaning old generated outputs..."
rm -rf "$ROOT/target"
rm -rf "$ROOT/out"
rm -rf "$ROOT/generated/pipeline"
rm -rf "$ROOT/generated/pipeline-manual-check"
rm -rf "$ROOT/generated/strict"
rm -rf "$ROOT/generated/strict-test"
rm -rf "$ROOT/generated/strict-flowchart-test"
rm -rf "$ROOT/generated/native-backend-test"
rm -rf "$ROOT/generated/strict-flowchart-run"
rm -rf "$ROOT/generated/semantic"
rm -rf "$ROOT/generated/final"
rm -f "$ROOT/sources.txt"

echo
echo "Running mvn test..."
mvn test

echo
echo "========================================"
echo "All tests passed."
echo "========================================"
