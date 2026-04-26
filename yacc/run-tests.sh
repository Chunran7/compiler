#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"
rm -rf "$OUT"
mkdir -p "$OUT"

find "$ROOT/src/main/java" "$ROOT/src/test/java" -name "*.java" > "$ROOT/sources.txt"
javac -encoding UTF-8 -d "$OUT" @"$ROOT/sources.txt"
rm -f "$ROOT/sources.txt"

java -cp "$OUT" com.example.compiler.test.TotalIntegrationTest
