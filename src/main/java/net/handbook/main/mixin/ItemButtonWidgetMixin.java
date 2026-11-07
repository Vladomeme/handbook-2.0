package net.handbook.main.mixin;

import dev.eliux.monumentaitemdictionary.gui.item.DictionaryItem;
import dev.eliux.monumentaitemdictionary.gui.widgets.ItemButtonWidget;
import net.handbook.main.HandbookClient;
import net.handbook.main.feature.TradeScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(ItemButtonWidget.class)
public abstract class ItemButtonWidgetMixin extends ClickableWidget {

    @Shadow @Final private DictionaryItem item;

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Ldev/eliux/monumentaitemdictionary/gui/widgets/ItemButtonWidget;playDownSound(Lnet/minecraft/client/sound/SoundManager;)V"),
            cancellable = true)
    public void hb$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (Screen.hasShiftDown() && !Screen.hasControlDown() && !Screen.hasAltDown()) {
            TradeScreen screen = HandbookClient.openTradeScreen();
            if (screen != null) screen.setSearchText(item.name());
            playDownSound(MinecraftClient.getInstance().getSoundManager());
            cir.setReturnValue(true);
        }
    }

    @SuppressWarnings("unused")
    public ItemButtonWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }
}
