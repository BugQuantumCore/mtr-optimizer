# Mixin Target Mappings

Since MTR and its addons are obfuscated or change class names between versions, this document tracks the target classes for our Mixins.

| Module | Target Class (MTR 4.0.5) | Fallback / Older Versions | Method Intercepted |
|--------|--------------------------|---------------------------|--------------------|
| Train Renderer | `mtr.client.TrainClientRenderer` | `mtr.client.TrainRenderer` | `render(...)` |
| Sign Renderer | `mtr.client.StationNameSignRenderer` | `mtr.client.SignRenderer` | `render(...)` |
| Rail Builder | `mtr.client.RailRenderHelper` | N/A | `renderRail(...)` |
| Catenary | `mtr.client.CatenaryRenderer` | N/A | `render(...)` |

> **Note for Contributors**: If MTR updates and breaks a Mixin, use `Vineflower` to decompile the new MTR jar, find the new class name, and update both the `@Mixin(targets=...)` annotation and this document.