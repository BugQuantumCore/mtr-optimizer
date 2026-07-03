package com.mtr_optimizer;

import com.mtr_optimizer.native.NativeJSRunner;
import com.mtr_optimizer.render.VBOCacheManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class MTROptimizerMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("[MTR-Optimizer] Initializing render optimization module...");

        if (NativeJSRunner.isAvailable()) {
            System.out.println("[MTR-Optimizer] Native library loaded successfully");
        } else {
            System.out.println("[MTR-Optimizer] Native library not available, " + "using Java fallback for JS rendering");
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) {
                VBOCacheManager.invalidateAll();
            }
        });

        System.out.println("[MTR-Optimizer] Ready. Mixins will be applied to MTR classes.");
    }
}