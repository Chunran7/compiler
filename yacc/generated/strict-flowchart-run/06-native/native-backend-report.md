# Native Backend Report

- clang version:
```text
Apple clang version 21.0.0 (clang-2100.1.1.101)
Target: arm64-apple-darwin25.4.0
Thread model: posix
InstalledDir: /Library/Developer/CommandLineTools/usr/bin
```
- input LLVM IR path: `generated/strict-flowchart-run/04-ir/output.ll`
- validate result: `0`
- assembly output path: `generated/strict-flowchart-run/06-native/output.s`
- object output path: `generated/strict-flowchart-run/06-native/output.o`
- executable output path: `generated/strict-flowchart-run/06-native/native-executable`
- run exit code: `3`
- stdout:
```text

```
- stderr:
```text

```
- skipped reason: ``
- limitations: Native backend depends on local clang. Current IR is generated for the project C subset; future external functions will need runtime libraries or extra link arguments.
