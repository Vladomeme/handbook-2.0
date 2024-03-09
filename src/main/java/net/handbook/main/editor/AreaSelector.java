package net.handbook.main.editor;

import net.handbook.main.MixinMethods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.handbook.main.feature.WaypointManager.buildClickableMessage;

public class AreaSelector {

    static ChatHud chat;
    static int[] coords = new int[6];

    static boolean active = false;
    static int tick;

    //returns int because used in command
    public static int init() {
        chat = MinecraftClient.getInstance().inGameHud.getChatHud();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            coords[0] = (int) player.getX();
            coords[1] = (int) player.getY();
            coords[2] = (int) player.getZ();
            coords[3] = (int) player.getX();
            coords[4] = (int) player.getY() + 1;
            coords[5] = (int) player.getZ();
        }
        sendChatControls();
        active = true;

        return 1;
    }

    public static void emitParticles() {
        tick++;
        if (tick != 5) return;
        tick = 0;
        ClientWorld w = MinecraftClient.getInstance().world;
        DefaultParticleType p = ParticleTypes.END_ROD;
        if (w == null) return;

        for (int i = Math.min(coords[0], coords[3]); i < Math.max(coords[0], coords[3]); i++) {
            w.addImportantParticle(p, true, i, coords[1], coords[2], 0.1, 0, 0);
            w.addImportantParticle(p, true, i, coords[1], coords[5], 0.1, 0, 0);
            w.addImportantParticle(p, true, i, coords[4], coords[2], 0.1, 0, 0);
            w.addImportantParticle(p, true, i, coords[4], coords[5], 0.1, 0, 0);
        }

        for (int i = Math.min(coords[1], coords[4]); i < Math.max(coords[1], coords[4]); i++) {
            w.addImportantParticle(p, true, coords[0], i, coords[2], 0, 0.1, 0);
            w.addImportantParticle(p, true, coords[0], i, coords[5], 0, 0.1, 0);
            w.addImportantParticle(p, true, coords[3], i, coords[2], 0, 0.1, 0);
            w.addImportantParticle(p, true, coords[3], i, coords[5], 0, 0.1, 0);
        }

        for (int i = Math.min(coords[2], coords[5]); i < Math.max(coords[2], coords[5]); i++) {
            w.addImportantParticle(p, true, coords[0], coords[1], i, 0, 0, 0.1);
            w.addImportantParticle(p, true, coords[0], coords[4], i, 0, 0, 0.1);
            w.addImportantParticle(p, true, coords[3], coords[1], i, 0, 0, 0.1);
            w.addImportantParticle(p, true, coords[3], coords[4], i, 0, 0, 0.1);
        }
    }

    public static void sendChatControls() {
        chat.getWidth();
        chat.addMessage(Text.literal("---------------------------------------").setStyle(Style.EMPTY.withColor(Formatting.BLUE)));
        chat.addMessage(Text.literal("           Point 1                    Point 2"));
        char[] dim = new char[]{'X', 'Y', 'Z'};
        for (int i = 0; i < 3; i++) {
            chat.addMessage(Text.literal(dim[i] + " ")
                    .append(buildClickableMessage("[-10]", "/handbook area move 0 " + i + " -10", ""))
                    .append(Text.literal(" "))
                    .append(buildClickableMessage("[-1]", "/handbook area move 0 " + i + " -1", ""))
                    .append(Text.literal(" "))
                    .append(buildClickableMessage("[+1]", "/handbook area move 0 " + i + " 1", ""))
                    .append(Text.literal(" "))
                    .append(buildClickableMessage("[+10]", "/handbook area move 0 " + i + " 10", ""))
                    .append(Text.literal("   "))
                    .append(buildClickableMessage("[-10]", "/handbook area move 1 " + i + " -10", ""))
                    .append(Text.literal(" "))
                    .append(buildClickableMessage("[-1]", "/handbook area move 1 " + i + " -1", ""))
                    .append(Text.literal(" "))
                    .append(buildClickableMessage("[+1]", "/handbook area move 1 " + i + " 1", ""))
                    .append(Text.literal(" "))
                    .append(buildClickableMessage("[+10]", "/handbook area move 1 " + i + " 10", ""))
            );
            chat.addMessage(Text.literal(" "));
        }
        chat.addMessage(Text.literal("     ")
                .append(buildClickableMessage("[Move to player]", "/handbook area move 0", ""))
                .append(Text.literal("        "))
                .append(buildClickableMessage("[Move to player]", "/handbook area move 1", "")));
        chat.addMessage(Text.literal("     " + coords[0] + ", " + coords[1] + ", " + coords[2]
                                   + "           " + coords[3] + ", " + coords[4] + ", " + coords[5]));
        chat.addMessage(Text.literal("[Copy]").setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true).withClickEvent(
                new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "[" + coords[0] + ", " + coords[1] + ", " + coords[2]
                                                                       + ", " + coords[3] + ", " + coords[4] + ", " + coords[5] + "]")))
                .append(Text.literal("   ").setStyle(Style.EMPTY.withUnderline(false)))
                .append(buildClickableMessage("[Exit]", "/handbook area finish", "Exit area selection")));
    }

    public static void updateMessage() {
        ((MixinMethods) chat).handbook$removeLastMessages(2);

        active = false;
        chat.addMessage(Text.literal("     " + coords[0] + ", " + coords[1] + ", " + coords[2]
                + "           " + coords[3] + ", " + coords[4] + ", " + coords[5]));
        chat.addMessage(Text.literal("[Copy]").setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true).withClickEvent(
                        new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "[" + coords[0] + ", " + coords[1] + ", " + coords[2]
                                + ", " + coords[3] + ", " + coords[4] + ", " + coords[5] + "]")))
                .append(Text.literal("   ").setStyle(Style.EMPTY.withUnderline(false)))
                .append(buildClickableMessage("[Exit]", "/handbook area finish", "Exit area selection")));
        active = true;
    }

    //returns int because used in command
    public static int movePoint(int point, int dim, int distance) {
        coords[point * 3 + dim] += distance;
        updateMessage();

        return 1;
    }

    //returns int because used in command
    public static int movePointToPlayer(int point) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            coords[point * 3] = (int) player.getX();
            coords[point * 3 + 1] = (int) player.getY();
            coords[point * 3 + 2] = (int) player.getZ();
        }
        updateMessage();

        return 1;
    }

    //returns int because used in command
    public static int finish() {
        active = false;
        ((MixinMethods) chat).handbook$unblockChat();

        return 1;
    }

    public static boolean isActive() {
        return active;
    }
}
