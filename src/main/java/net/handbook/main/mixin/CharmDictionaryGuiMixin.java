package net.handbook.main.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.eliux.monumentaitemdictionary.gui.charm.CharmDictionaryGui;
import dev.eliux.monumentaitemdictionary.gui.charm.DictionaryCharm;
import dev.eliux.monumentaitemdictionary.gui.item.ItemDictionaryGui;
import dev.eliux.monumentaitemdictionary.util.ItemColors;
import net.handbook.main.HandbookClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(CharmDictionaryGui.class)
public abstract class CharmDictionaryGuiMixin {

    @Inject(method = "generateCharmLoreText", at = @At(value = "RETURN"), remap = false, cancellable = true)
    public void generateCharmLoreText$lore(DictionaryCharm charm, CallbackInfoReturnable<List<Text>> cir) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof ItemDictionaryGui || currentScreen instanceof CharmDictionaryGui) {
            List<Text> lines = cir.getReturnValue();
            lines.add(lines.size() - 1, Text.literal("[SHIFT] + Click to search in Handbook 2.0")
                    .setStyle(Style.EMPTY.withColor(ItemColors.TEXT_COLOR)));
            cir.setReturnValue(lines);
        }
    }

    @Inject(method = "lambda$buildCharmList$9", at = @At(value = "HEAD"))
    public void buildCharmList$click(CallbackInfo ci, @Local(argsOnly = true) DictionaryCharm charm) {
        if (Screen.hasShiftDown() && !Screen.hasControlDown() && !Screen.hasAltDown()) {
            HandbookClient.openTradeScreen();
            HandbookClient.tradeScreen.setSearchText(charm.name);
        }
    }
}
