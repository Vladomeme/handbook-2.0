package net.handbook.main.mixin;

import net.handbook.main.DataManager;
import net.handbook.main.HBMixinMethods;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.AreaSelector;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.entry.Category;
import net.handbook.main.resources.entry.Entry;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements HBMixinMethods {

    @Shadow @Final
    private List<ChatHudLine> messages;
    @Shadow @Final
    private List<ChatHudLine.Visible> visibleMessages;

    @Shadow public abstract void addMessage(Text message);

    @Unique
    private static final List<Text> blockedMessages = new ArrayList<>();

    @Redirect(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"))
    public void addMessage(ChatHud instance, ChatHudLine message) {
        Text text = message.content();
        if (AreaSelector.isActive()) {
            blockedMessages.add(text);
            return;
        }
        String string = text.getString();
        if (HandbookConfig.INSTANCE.enabled && HandbookConfig.INSTANCE.editMessages && string.contains("Position:"))
            message = new ChatHudLine(message.creationTick(), injectWaypointClickEvent(text), message.signature(), message.indicator());

        invokeAddMessage(message);

        if (HandbookConfig.INSTANCE.enabled) {
            if (string.startsWith("Your bounty for"))
                suggestBountyWaypoint(string);
        }
    }

    @Unique
    public Text injectWaypointClickEvent(Text message) {
        Text text = !message.getSiblings().isEmpty() ? message.getSiblings().getLast() : message;

        int index = text.getString().indexOf("Position:");
        String prePosition = text.getString().substring(0, index);
        String position = text.getString().substring(index).replace("Position: ", "");

        String[] coordinates = position.replace(" ", "").split(",", 3);

        try {
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
        catch (Exception ignored) {
            //unlucky
            return message;
        }
    }

    @Unique
    private void suggestBountyWaypoint(String message) {
        String POIName = message.replace("Your bounty for today is ", "").replace("!", "");
        for (Category<? extends Entry> category : DataManager.getCategories()) {
            if (!category.clearTitle().startsWith("POI")) continue;

            for (Entry entry : category.entries()) {
                if (!entry.clearTitle().equals(POIName)) continue;

                int[] coords = entry.position();

                addMessage(WaypointManager.buildClickableMessage("[Set waypoint]",
                        "/handbook waypoint " + coords[0] + " " + coords[1] + " " + coords[2] + " \"" + POIName + "\"", "Click to set a waypoint"));
                return;
            }
        }
    }

    @Unique
    public void handbook$unblockChat(int deleteMessages) {
        if (deleteMessages != 0) handbook$removeLastMessages(deleteMessages);
        blockedMessages.forEach(this::addMessage);
        blockedMessages.clear();
    }

    @Unique
    public void handbook$removeLastMessages(int amount) {
        for (int i = 0; i < amount; i++) {
            this.messages.removeFirst();
            this.visibleMessages.removeFirst();
        }
    }

    @Unique
    public int getTrunkLength(MutableText text) {
        int length = 0;
        for (Text section : text.getSiblings())
            length += section.getString().length();
        return text.getString().length() - length;
    }

    @Invoker("addMessage")
    abstract void invokeAddMessage(ChatHudLine message);
}
