---
name: bibix4
description: This project uses bibix4 (not gradle/maven). Build via `/Users/joonsoo/Documents/apps/bibix4/bibix4 <target>`. Actions defined in build.bbx4 (e.g. `runMgroup3Test`, `runMgroup2Test`, `runMgroup3ParserTest`). Logs in `bbx4build/logs/actions/jparser.<actionName>/context.log`.
---

# Building & testing this project

This repo is built with **bibix4**, not gradle/maven/sbt. The legacy `build.bbx` exists but the current active build file is `build.bbx4`.

## Invoking bibix4

```bash
/Users/joonsoo/Documents/apps/bibix4/bibix4 <target>
```

There is no `bbx4` on PATH — always use the absolute path above. The wrapper script just runs `java ... -jar /Users/joonsoo/Documents/apps/bibix4/bibix4-bundle.jar`.

## What `<target>` can be

Two kinds of targets:

### Library/data targets (just build, no execution)
- `mgroup3.parser` — build the parser module
- `mgroup3.test` — build the test classes (does **not** run them)
- `mgroup3.gen`, `mgroup3.parserDataSchema.schema`, etc.
- Same pattern for `mgroup2.*`, `naive.*`, `metalang.*`, `ktparser.*`, etc.

### Actions (run something; declared with `@action` in build.bbx4)

Find them with: `grep "^def " build.bbx4`. As of last check:
- `runTest` — runs scalatest across base/naive/metalang/milestone2/mgroup2 tests (slow)
- `runMgroup2Test` — single scalatest suite `com.giyeok.jparser.mgroup2.Mgroup2Test`
- `runMgroup3Test` — all JUnit tests in `mgroup3.test`
- `runMgroup3GenTest` — only `com.giyeok.jparser.mgroup3.ParserGenTest`
- `runMgroup3ParserTest` — only `com.giyeok.jparser.mgroup3.Mgroup3ParserTest`
- `runMgroup3MulangCdgTest` — only `com.giyeok.jparser.mgroup3.MulangCdgTest`
- `runMgroup3AdvancedTest` — only `com.giyeok.jparser.mgroup3.Mgroup3ParserAdvancedTest`

Run an action: `/Users/joonsoo/Documents/apps/bibix4/bibix4 runMgroup3Test`.

## Reading test results

Two log locations:

1. **Streaming action log** — `bbx4build/logs/actions/jparser.<actionName>/context.log`. The action-level events (which test is running, summary counts, failure markers).

2. **Per-call stdout/stderr** — `bbx4build/logs/outputs/jparser.<actionName>/call-*/cmd-001.{stdout,stderr}`. The actual program output (println, traces, etc.). When you add `setTrace(gen)` or other diagnostic prints, this is where they land.

The tool's stdout shows summary lines like `[97 tests successful] [1 tests failed]`. To find **which** test failed, grep the context.log:

```bash
grep -i "fail\|Failures" bbx4build/logs/actions/jparser.runMgroup3Test/context.log | tail
```

To see actual test output (println, exception messages, traces), look at the per-call stdout:

```bash
ls bbx4build/logs/outputs/jparser.runMgroup3Test/        # find latest call-xxx
grep -A 50 "TRACE parseStep" bbx4build/logs/outputs/jparser.runMgroup3Test/call-*/cmd-001.stdout
```

**Don't re-run bibix4 just to see output.** After an action completes, the logs above already contain everything stdout/stderr produced — read those instead of re-running.

Failure markers look like:
```
Failures (1):
    MethodSource [className = 'com.giyeok.jparser.mgroup3.MulangCdgTest', methodName = 'testTryLetMu', ...]
```

For scalatest actions (e.g. runMgroup2Test) the failure format is different — look for `*** FAILED ***` or `did not equal`.

## When proto/schema changes

If you edit a `.proto` file under `mgroup3/schema/proto/`, regenerate the Java sources before rebuilding:

```bash
/Users/joonsoo/Documents/apps/bibix4/bibix4 mgroup3.parserDataSchema.generate
```

Same pattern for other schemas (`mgroup2.proto.generate`, `milestone2.proto.generate`, `base.proto.generate`, `study.proto.generate`). The generated Java goes under `mgroup3/schema/generated/java/`, **is** committed to git (intentionally — easier IDE indexing).

## Common gotchas

- **Build output is huge.** A successful run writes a 100KB+ JvmLib summary line. If the output is just one giant JvmLib(...) line ending normally, build succeeded. Look for stderr lines starting with `error:` to detect actual failures, or the trailing `Failed:` / `Build failed` line.
- **Output too large to read fully.** When the bash tool persists a >2KB output to disk, you can usually tell success from the *last* lines (tail) — bibix4 prints `[bibix4] action 'X' finished` on success and `[bibix4] action 'X' failed` on failure.
- **`bbx`, `bbx4`, `bbx5` are not commands.** Earlier sessions invented these. Only the absolute path works.
- **Don't mix `build.bbx` and `build.bbx4`.** `build.bbx` is the legacy bibix-0.x format; `build.bbx4` is the current one and is what bibix4 reads.
