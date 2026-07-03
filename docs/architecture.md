# Architecture Overview

MTR Render Optimizer operates by intercepting the Minecraft rendering pipeline at critical points using the Fabric Mixin framework, and offloading heavy computations to a native Rust library via JNI.

## Rendering Pipeline Interception

1. **Entity Culling**: Before `WorldRenderer` iterates over entities, `AdvancedFrustumCuller` filters out MTR trains and signs that are outside the camera frustum or occluded by opaque blocks.
2. **VBO Caching**: When MTR attempts to build static geometry (rails, catenaries) via `BufferBuilder`, our Mixin intercepts the `end()` call. The vertex data is uploaded to a persistent GPU VBO and cached. Subsequent frames simply bind and draw the VBO.
3. **GPU Instancing**: For dynamic entities like trains, `TrainInstanceBatcher` collects transform matrices and instance data (color, door state) into a shared instance VBO. A single `glDrawArraysInstanced` call renders all trains of the same model.
4. **Dynamic Textures (PIDS)**: MTR uses JS to render station signs. We replace the Java `ScriptEngine` with a Rust-backed QuickJS engine. A dirty-flag system ensures JS is only executed when sign data (e.g., next train arrival time) actually changes.

## Memory Management
- **Off-heap buffers**: Instance data and JS pixel outputs use `MemoryUtil` (LWJGL) to avoid Java GC pressure.
- **LRU Caching**: VBOs and compiled JS contexts are evicted if not accessed for >10 minutes.