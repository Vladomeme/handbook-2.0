package net.handbook.main.mixin;

import net.handbook.main.config.HandbookConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Final @Shadow
    private MinecraftClient client;

    @Shadow
    protected abstract void addMessage(Text message, @Nullable MessageSignatureData signature, int ticks, @Nullable MessageIndicator indicator, boolean refresh);

    @Redirect(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V"))
    public void addMessage(ChatHud instance, Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh) {
        if (message.getString().contains("Position:") && HandbookConfig.INSTANCE.enabled) message = injectWaypointClickEvent(message);
        addMessage(message, signature, this.client.inGameHud.getTicks(), indicator, false);
    }

    @Unique
    public Text injectWaypointClickEvent(Text message) {
        Text text = message.getSiblings().size() > 0 ? message.getSiblings().get(message.getSiblings().size() - 1) : message;

        int index = text.getString().indexOf("Position:");
        String prePosition = text.getString().substring(0, index);
        String position = text.getString().substring(index).replace("Position: ", "");

        String[] coordinates = position.replace(" ", "").split(",", 3);
        int x = Integer.parseInt(coordinates[0]);
        int y = Integer.parseInt(coordinates[1]);
        int z = Integer.parseInt(coordinates[2]);

        MutableText modifiedText = Text.empty();

        if (message.getSiblings().size() > 0) {
            modifiedText = Text.literal(message.asTruncatedString(getTrunkLength(message.copy()))).setStyle(message.getStyle());
            for (int i = 0; i < message.getSiblings().size() - 1; i++) {
                modifiedText.append(message.getSiblings().get(i));
            }
        }
        modifiedText.append(Text.literal(prePosition).setStyle(text.getStyle()));
        modifiedText.append(Text.literal(position).setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handbook waypoint " + x + " " + y + " " + z))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to set a waypoint")))
                .withColor(Formatting.AQUA)
                .withUnderline(true)));

        return modifiedText;
    }

    @Unique
    public int getTrunkLength(MutableText text) {
        int length = 0;
        for (Text section : text.getSiblings()) {
            length += section.getString().length();
        }
        return text.getString().length() - length;
    }
}
