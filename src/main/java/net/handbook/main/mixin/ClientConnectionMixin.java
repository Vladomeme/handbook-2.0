package net.handbook.main.mixin;

import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.feature.WaypointManager;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Inject(method = "handleDisconnection", at = @At("TAIL"))
    public void handleDisconnection(CallbackInfo ci) {
        if (!HandbookConfig.INSTANCE.enabled) return;
        HandbookClient.dumpAll();
        WaypointManager.setState(false);
    }
}
