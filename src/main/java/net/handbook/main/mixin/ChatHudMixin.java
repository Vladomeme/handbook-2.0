package net.handbook.main.mixin;

import net.handbook.main.HandbookClient;
import net.handbook.main.HBMixinMethods;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.AreaSelector;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.category.BaseCategory;
import net.handbook.main.resources.category.PositionedCategory;
import net.handbook.main.resources.entry.PositionedEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements HBMixinMethods {

    @Shadow @Final
    private MinecraftClient client;
    @Shadow @Final
    private List<ChatHudLine> messages;
    @Shadow @Final
    private List<ChatHudLine.Visible> visibleMessages;

    @Unique
    private static final List<Text> blockedMessages = new ArrayList<>();

    @SuppressWarnings("SameParameterValue")
    @Shadow
    protected abstract void addMessage(Text message, @Nullable MessageSignatureData signature, int ticks, @Nullable MessageIndicator indicator, boolean refresh);

    @Shadow
    public abstract void addMessage(Text message);

    @Redirect(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V"))
    public void addMessage(ChatHud instance, Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh) {
        if (AreaSelector.isActive()) {
            blockedMessages.add(message);
            return;
        }
        if (HandbookConfig.INSTANCE.enabled && HandbookConfig.INSTANCE.editMessages && message.getString().contains("Position:"))
            message = injectWaypointClickEvent(message);

        addMessage(message, signature, client.inGameHud.getTicks(), indicator, false);

        if (HandbookConfig.INSTANCE.enabled) {
            if (message.getString().startsWith("Your bounty for"))
                suggestBountyWaypoint(message.getString());
        }
    }

    @Unique
    public Text injectWaypointClickEvent(Text message) {
        Text text = !message.getSiblings().isEmpty() ? message.getSiblings().get(message.getSiblings().size() - 1) : message;

        int index = text.getString().indexOf("Position:");
        String prePosition = text.getString().substring(0, index);
        String position = text.getString().substring(index).replace("Position: ", "");

        String[] coordinates = position.replace(" ", "").split(",", 3);
        int x = Integer.parseInt(coordinates[0]);
        int y = Integer.parseInt(coordinates[1]);
        int z = Integer.parseInt(coordinates[2]);

        MutableText modifiedText = Text.empty();

        if (!message.getSiblings().isEmpty()) {
            modifiedText = Text.literal(message.asTruncatedString(getTrunkLength(message.copy()))).setStyle(message.getStyle());
            for (int i = 0; i < message.getSiblings().size() - 1; i++)
                modifiedText.append(message.getSiblings().get(i));
        }
        modifiedText.append(Text.literal(prePosition).setStyle(text.getStyle()));
        modifiedText.append(WaypointManager.buildClickableMessage(position,
                "/handbook waypoint " + x + " " + y + " " + z, "Click to set a waypoint"));

        return modifiedText;
    }

    @Unique
    private void suggestBountyWaypoint(String message) {
        String POIName = message.replace("Your bounty for today is ", "").replace("!", "");
        for (BaseCategory category : HandbookClient.handbookScreen.categories) {
            if (!category.getClearTitle().startsWith("POI")) continue;

            for (PositionedEntry entry : ((PositionedCategory) category).getEntries()) {
                if (!entry.getClearTitle().equals(POIName)) continue;

                int[] coords = entry.getPosition();

                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(WaypointManager.buildClickableMessage("[Set waypoint]",
                        "/handbook waypoint " + coords[0] + " " + coords[1] + " " + coords[2], "Click to set a waypoint"));
                return;
            }
        }
    }

    @Unique
    public void handbook$unblockChat() {
        handbook$removeLastMessages(11);
        for (Text message : blockedMessages)
            addMessage(message);
        blockedMessages.clear();
    }

    @Unique
    public void handbook$removeLastMessages(int amount) {
        for (int i = 0; i < amount; i++) {
            this.messages.remove(0);
            this.visibleMessages.remove(0);
        }
    }

    @Unique
    public int getTrunkLength(MutableText text) {
        int length = 0;
        for (Text section : text.getSiblings())
            length += section.getString().length();
        return text.getString().length() - length;
    }
}
