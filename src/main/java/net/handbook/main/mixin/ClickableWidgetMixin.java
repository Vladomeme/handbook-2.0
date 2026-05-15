package net.handbook.main.mixin;

import net.handbook.main.HBMixinMethods;
import net.handbook.main.HandbookClient;
import net.handbook.main.editor.LocationScreen;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.MapScreen;
import net.handbook.main.feature.TradeScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClickableWidget.class)
public abstract class ClickableWidgetMixin implements HBMixinMethods {

    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    public void playDownSound(SoundManager soundManager, CallbackInfo ci) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof HandbookScreen
                || currentScreen instanceof TradeScreen
                || currentScreen instanceof MapScreen
                || currentScreen instanceof LocationScreen) {
            if (HandbookClient.clickTimer > 5) {
                HandbookClient.clickTimer = 0;
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_CALCITE_PLACE, 1.0f));
            }
            ci.cancel();
        }
    }
}
