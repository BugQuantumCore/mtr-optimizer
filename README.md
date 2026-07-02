# 🚄 MTR Render Optimizer

**English**

An unofficial client-side rendering optimization mod for [Minecraft Transit Railway (MTR)](https://github.com/Minecraft-Transit-Railway/Minecraft-Transit-Railway) and its addons (JCM, Tianjin Metro, etc.) on Fabric 1.20.1.

## ✨ Features
- **🚀 GPU Instancing for Trains**: Batches identical train models into single draw calls.
- **💾 VBO Caching**: Caches static railway infrastructure (rails, catenaries) on the GPU.
- **⚡ Native JS Engine**: Replaces Nashorn with Rust-compiled QuickJS for PIDS sign rendering (5x faster).
- **📐 SIMD Math**: Accelerates bezier curve calculations for rails using Rust SIMD.
- **👁️ Advanced Culling**: Frustum and occlusion culling specifically tuned for MTR entities.

## 📊 Performance
*Tested on a large subway hub with 20+ trains and 100+ PIDS signs (RTX 3060, Ryzen 5 5600X).*
| Setup | FPS (Avg) | Draw Calls | CPU Time (Render) |
|-------|-----------|------------|-------------------|
| Vanilla + MTR | 24 FPS | ~4,500 | 38 ms |
| + Sodium | 45 FPS | ~3,200 | 22 ms |
| **+ MTR Optimizer** | **110 FPS** | **~450** | **8 ms** |

## 📥 Installation
1. Install [Fabric Loader](https://fabricmc.net/) for 1.20.1.
2. Download the mod from GitHub Releases.
3. Drop the `.jar` into your `.minecraft/mods` folder.
*(No additional dependencies required, native libraries are bundled inside!)*

## ⚠️ Compatibility
- ✅ **Compatible**: Sodium, Lithium, Iris, EntityCulling.
- ❌ **Incompatible**: VulkanMod (Rendering pipeline conflicts).

## 🛠️ Building from Source
See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed instructions on setting up the Java + Rust hybrid build environment.