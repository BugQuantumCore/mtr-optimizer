# Contributing to MTR Render Optimizer

Thank you for your interest in improving railway rendering performance!

## Prerequisites
- JDK 21+
- Rust 1.75+ (Run `./scripts/setup-dev-env.sh` to configure targets)
- Minecraft 1.20.1 Fabric Dev Environment

## Adding a New Optimization
1. **Identify the bottleneck**: Use Spark or VisualVM to profile MTR.
2. **Create a Mixin**: Add your Mixin in `src/main/java/.../mixin/`. Remember to register it in `mtr-optimizer.mixins.json`.
3. **Fallback is mandatory**: If your optimization uses the Rust native library, you **must** provide a pure Java fallback in case the native library fails to load.
4. **Update Docs**: Add your new Mixin targets to `docs/mixin-targets.md`.

## Rust Code Guidelines
- Use `cargo fmt` before committing.
- All JNI functions must handle panics gracefully (use `catch_unwind` if necessary) to prevent crashing the JVM.
- Prefer `rquickjs` over `boa` or `deno_core` for JS execution to keep binary size small.