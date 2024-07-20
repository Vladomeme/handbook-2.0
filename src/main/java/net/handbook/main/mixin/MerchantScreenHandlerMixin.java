package net.handbook.main.mixin;

import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.NPCWriter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOfferList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreenHandler.class)
public abstract class MerchantScreenHandlerMixin extends ScreenHandler {

    @Inject(method = "setOffers", at = @At("TAIL"))
    public void setOffers(TradeOfferList offers, CallbackInfo ci) {
        if (HandbookConfig.INSTANCE.enabled && HandbookConfig.INSTANCE.enableScanner) NPCWriter.addOffers(offers);
        if (HandbookConfig.INSTANCE.quickSaveTrades) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = client.world;
            ClientPlayerEntity player = client.player;
            if (world == null || player == null) return;
            world.playSound(player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 2.0f, 1.7f, false);

            client.inGameHud.getChatHud().addMessage(Text.of("§aOffers saved."));
            client.setScreen(null);
        }
    }

    @SuppressWarnings("unused")
    protected MerchantScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }


}
