package net.handbook.main.mixin;

import dev.eliux.monumentaitemdictionary.gui.charm.CharmDictionaryGui;
import dev.eliux.monumentaitemdictionary.gui.charm.DictionaryCharm;
import dev.eliux.monumentaitemdictionary.gui.item.ItemDictionaryGui;
import dev.eliux.monumentaitemdictionary.util.ItemColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(CharmDictionaryGui.class)
public abstract class CharmDictionaryGuiMixin {

    @Inject(method = "generateCharmLoreText", at = @At(value = "RETURN"), remap = false, cancellable = true)
    public void hb$generateCharmLoreText(DictionaryCharm charm, CallbackInfoReturnable<List<Text>> cir) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof ItemDictionaryGui || currentScreen instanceof CharmDictionaryGui) {
            List<Text> lines = cir.getReturnValue();
            lines.add(lines.size() - 1, Text.literal("[SHIFT] + Click to search in Handbook 2.0")
                    .setStyle(Style.EMPTY.withColor(ItemColors.TEXT_COLOR)));
            cir.setReturnValue(lines);
        }
    }
}
