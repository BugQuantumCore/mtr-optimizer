package com.mtr_optimizer.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "mtr-optimizer")
public class OptimizerConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean enableVBOCaching = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableGPUInstancing = true;

    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean enableNativeJSEngine = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int signUpdateThrottleTicks = 5;

    @ConfigEntry.Gui.Tooltip
    public boolean enableAdvancedCulling = true;

    @ConfigEntry.Category("lod")
    @ConfigEntry.Gui.TransitiveObject
    public LODConfig lod = new LODConfig();

    public static class LODConfig {
        public boolean enableLOD = true;
        @ConfigEntry.BoundedDiscrete(min = 32, max = 256)
        public int fullDetailDistance = 48;
        @ConfigEntry.BoundedDiscrete(min = 64, max = 512)
        public int billboardDistance = 192;
    }

    public static OptimizerConfig get() {
        return AutoConfig.getConfigHolder(OptimizerConfig.class).getConfig();
    }

    public static void save() {
        AutoConfig.getConfigHolder(OptimizerConfig.class).save();
    }
}