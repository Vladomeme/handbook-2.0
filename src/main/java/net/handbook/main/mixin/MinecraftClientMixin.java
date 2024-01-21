package net.handbook.main.mixin;

import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.feature.WaypointManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "disconnect()V", at = @At("TAIL"))
    public void handleDisconnection(CallbackInfo ci) {
        if (!HandbookConfig.INSTANCE.enabled) return;
        if (WaypointManager.isActive()) {
            WaypointManager.saveWaypoints();
            WaypointManager.clear();
        }
        HandbookClient.dumpAll();
    }
}
