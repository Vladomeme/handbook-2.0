package net.handbook.main.feature;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.handbook.main.HandbookClient;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.WaypointEntry;
import net.handbook.main.resources.waypoint.Teleport;
import net.handbook.main.resources.waypoint.Teleports;
import net.handbook.main.resources.waypoint.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.*;

import static net.handbook.main.resources.waypoint.Teleports.*;

public class WaypointManager {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final Queue<WaypointEntry> waypoints = new LinkedList<>();
    private static final List<WaypointEntry> altPath = new ArrayList<>();
    static int tick;
    static double distance;
    private static boolean paused = false;
    private static boolean sendRestoreMessage = false;

    public static final Identifier BEAM_TEXTURE = new Identifier("textures/entity/beacon_beam.png");

    public static void tick() {
        if (waypoints.isEmpty()) return;

        tick++;
        if (waypoints.size() > 1) checkChain();
        emitParticles();
        if (tick >= 60) tick = 0;
    }

    public static void setWaypoint(Entry entry) {
        setWaypoint(entry.getPosition(), entry.getTitle(), entry.getText());
    }

    public static void setWaypoint(int[] coords, String title, String text) {
        setWaypoint(new WaypointEntry(title, text, new Waypoint(coords[0], coords[1], coords[2]), false, null));
    }

    public static void setWaypoint(WaypointEntry entry) {
        WaypointManager.waypoints.clear();
        WaypointManager.waypoints.add(entry);

        setState(true);
        tick = 0;

        if (client.world == null) return;
        String shard = client.world.getRegistryKey().getValue().toString().replace("monumenta:", "").split("-")[0];
        client.inGameHud.getChatHud().addMessage(getFastestPath(shard, entry, false));
    }

    public static void setWaypointChain(List<WaypointEntry> waypoints) {
        WaypointManager.waypoints.clear();
        for (WaypointEntry entry : waypoints) WaypointManager.waypoints.add(entry.markAsChain());

        setState(true);
        tick = 0;

        if (client.world == null) return;
        String shard = client.world.getRegistryKey().getValue().toString().replace("monumenta:", "").split("-")[0];
        client.inGameHud.getChatHud().addMessage(getFastestPath(shard, waypoints.get(0), true));
    }

    public static void setState(boolean state) {
        if (HandbookScreen.clearWaypoint == null) return;
        if (!state) waypoints.clear();
        paused = false;
        HandbookScreen.clearWaypoint.visible = state;
        HandbookScreen.clearWaypoint.active = state;
    }

    public static void clear() {
        waypoints.clear();
    }

    public static void skip() {
        onWaypointReached(MinecraftClient.getInstance().player, MinecraftClient.getInstance().world);
    }

    public static void emitParticles() {
        if (tick > 40 || paused) return;

        if (waypoints.peek() == null) return;
        Waypoint waypoint = waypoints.peek().getWaypoint();
        if (waypoint == null) return;

        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world == null || player == null) return;
        distance = Math.sqrt(Math.pow(player.getX() - waypoint.x(), 2) + Math.pow(player.getY() - waypoint.y(), 2)
                + Math.pow(player.getZ() - waypoint.z(), 2));
        if (distance < 5 || (waypoints.size() > 1 && distance < 10)) onWaypointReached(player, world);

        double particleX = player.getX() + ((waypoint.x() - player.getX()) / distance) * ((float) tick / 3);
        double particleY = player.getY() + ((waypoint.y() - player.getY()) / distance) * ((float) tick / 3);
        double particleZ = player.getZ() + ((waypoint.z() - player.getZ()) / distance) * ((float) tick / 3);

        world.addParticle(ParticleTypes.END_ROD, particleX + (Math.random() - Math.random()) * 0.5,
                particleY + 0.5 + (Math.random() - Math.random()) * 0.5,
                particleZ + (Math.random() - Math.random()) * 0.5, 0, 0, 0);
    }

    public static void renderBeacon(WorldRenderContext context) {
        if (waypoints.peek() == null || paused) return;
        Waypoint waypoint = waypoints.peek().getWaypoint();
        if (waypoint == null) return;

        ClientWorld world = client.world;
        Vec3d pos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        if (world == null) return;

        double beaconX = distance < 150 ? (double) waypoint.x() - pos.getX() : ((waypoint.x() - pos.getX()) / distance) * 150;
        double beaconZ = distance < 150 ? (double) waypoint.z() - pos.getZ() : ((waypoint.z() - pos.getZ()) / distance) * 150;

        context.matrixStack().push();
        context.matrixStack().translate(beaconX, -(pos.getY() + 64), beaconZ);
        BeaconBlockEntityRenderer.renderBeam(context.matrixStack(), context.consumers(), BEAM_TEXTURE, 0, 1,
                world.getTime(), 0, 1024, DyeColor.LIGHT_BLUE.getColorComponents(), 0.3f, 0.3f
        );
        context.matrixStack().pop();
    }

    private static void checkChain() {
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world == null || player == null) return;

        for (WaypointEntry waypointEntry : waypoints) {
            Waypoint waypoint = waypointEntry.getWaypoint();
            int distance = (int) Math.sqrt(Math.pow(player.getX() - waypoint.x(), 2) + Math.pow(player.getY() - waypoint.y(), 2)
                    + Math.pow(player.getZ() - waypoint.z(), 2));
            if (!(distance < 5 || (waypoints.size() > 1 && distance < 10))) continue;

            if (waypointEntry.equals(waypoints.peek())) return;

            while (!waypointEntry.equals(waypoints.peek())) waypoints.poll().getTitle();
            return;
        }
    }

    private static void onWaypointReached(ClientPlayerEntity player, ClientWorld world) {
        if (paused) return;
        if (waypoints.size() == 1) {
            client.inGameHud.getChatHud().addMessage(Text.of(
                    (waypoints.peek().getText().isEmpty() ? "" : (waypoints.poll().getText() + " ")) + "§aWaypoint removed."));
            setState(false);
        } else {
            if (waypoints.size() > 1) {
                if (waypoints.peek().shouldPause()) {
                    client.inGameHud.getChatHud().addMessage(Text.of(waypoints.poll().getText()));
                    client.inGameHud.getChatHud().addMessage(Text.literal("[Continue]")
                            .setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handbook waypoint continue"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to set the next waypoint")))));
                    paused = true;
                } else {
                    WaypointEntry waypoint = waypoints.poll();
                    client.inGameHud.getChatHud().addMessage(Text.of(
                            waypoint.getText() + " Head to " + waypoints.peek().getClearTitle()));
                    MutableText text = Text.literal("[Skip]")
                            .setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handbook waypoint skip"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to skip this waypoint"))));
                    if (waypoints.peek().inChain() && shouldSuggestPath(waypoint)) {
                        text.append(Text.literal(" ").setStyle(Style.EMPTY.withUnderline(false)))
                                .append(Text.literal("[Add fastest path]")
                                .setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handbook waypoint path"))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to find fastest path")))));
                    }
                    client.inGameHud.getChatHud().addMessage(text);
                }
            }
        }
        world.playSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 2.0f, 1.7f, false);
    }

    public static void continuePath() {
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (player == null || world == null) return;

        paused = false;
        client.inGameHud.getChatHud().addMessage(Text.of("Head to " + waypoints.peek().getClearTitle()));
        MutableText text = Text.literal("[Skip]")
                .setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handbook waypoint skip"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to skip this waypoint"))));
        if (waypoints.peek().inChain() && shouldSuggestPath(waypoints.peek())) {
            text.append(Text.literal(" ").setStyle(Style.EMPTY.withUnderline(false)))
                    .append(Text.literal("[Add fastest path]")
                    .setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handbook waypoint path"))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to find fastest path")))));
        }
        client.inGameHud.getChatHud().addMessage(text);
        world.playSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 2.0f, 1.7f, false);
    }

    public static void addPathToChain() {
        WaypointEntry target = waypoints.poll();
        List<WaypointEntry> remainingPath = waypoints.stream().toList();
        waypoints.clear();
        String shard = client.world.getRegistryKey().getValue().toString().replace("monumenta:", "").split("-")[0];
        client.inGameHud.getChatHud().addMessage(getFastestPath(shard, target, false));
        waypoints.addAll(remainingPath);
    }

    public static boolean isActive() {
        return !waypoints.isEmpty();
    }

    public static void printInfo() {
        StringBuilder string = new StringBuilder();
        for (WaypointEntry waypoint : waypoints) {
            string.append(waypoint.getClearTitle()).append(" -> ");
        }
        HandbookClient.LOGGER.info(string.substring(0, string.length() - 3));
    }

    public static double getDistance() {
        return distance;
    }

    public static Text getFastestPath(String shard, WaypointEntry entry, boolean append) {
        ClientPlayerEntity player = client.player;
        if (player == null) return null;

        Waypoint waypoint = entry.getWaypoint();
        int x = waypoint.x();
        int y = waypoint.y();
        int z = waypoint.z();

        Teleport safeTP = getNearestTeleport(shard, x, y, z, true);
        Teleport unsafeTP = getNearestTeleport(shard, x, y, z, false);

        int safePath = getDistanceToTP(safeTP, x, y, z, false);
        int unsafePath = getDistanceToTP(unsafeTP, x, y, z, false);

        int straightPath;
        Teleport nearestTP;
        int distanceToTp;

        if (isInPlayableArea(shard, player)) {
            nearestTP = getNearestTeleport(shard, (int) player.getX(), (int) player.getY(), (int) player.getZ(), true);
            distanceToTp = getDistanceToTP(nearestTP, (int) player.getX(), (int) player.getY(), (int) player.getZ(), false);
            straightPath = getDistance((int) player.getX(), (int) player.getY(), (int) player.getZ(), x, y, z);
        } else {
            nearestTP = getRegionHub(shard).get();
            distanceToTp = 0;
            straightPath = getDistance(nearestTP.x(), nearestTP.y(), nearestTP.z(), x, y, z);
        }

        MutableText text = Text.empty().append("Fastest way: ");
        if (safeTP == null) {
            if (unsafeTP == null) text.append("walk (~" + straightPath);
            else {
                if (unsafePath > straightPath) text.append("walk (~" + straightPath);
                else {
                    if (straightPath / unsafePath > 2) text.append("walk (~" + straightPath);
                    else text.append("walk (~" + straightPath + " blocks) or ")
                            .append(addHubLocations(nearestTP, unsafeTP, shard, entry, true, append))
                            .append(" (~" + (unsafePath + distanceToTp));
                }
            }
        } else {
            if (unsafeTP == null) {
                if (safePath < straightPath)
                    text.append(addHubLocations(nearestTP, safeTP, shard, entry, false, append))
                        .append(" (~" + (safePath + distanceToTp));
                else text.append("walk (~" + straightPath);
            } else {
                if (safePath < unsafePath) {
                    if (safePath < straightPath) 
                        text.append(addHubLocations(nearestTP, safeTP, shard, entry, false, append))
                            .append(" (~" + (safePath + distanceToTp));
                    else text.append("walk (~" + straightPath);
                } else {
                    if (unsafePath > straightPath) text.append("walk (~" + straightPath);
                    else {
                        if (safePath < straightPath) {
                            if (straightPath / unsafePath > 2) 
                                text.append(addHubLocations(nearestTP, safeTP, shard, entry, false, append))
                                    .append(" (~" + (safePath + distanceToTp) + " blocks) or ")
                                    .append(addHubLocations(nearestTP, unsafeTP, shard, entry, true, append))
                                    .append(" (~" + (unsafePath + distanceToTp));
                            else text.append(addHubLocations(nearestTP, safeTP, shard, entry, false, append))
                                    .append(" (~" + (safePath + distanceToTp));
                        } else {
                            if (safePath / unsafePath > 2)
                                text.append("walk (~" + straightPath + " blocks) or ")
                                        .append(addHubLocations(nearestTP, unsafeTP, shard, entry, true, append))
                                        .append(" (~" + (unsafePath + distanceToTp));
                            else text.append("walk (~" + straightPath);
                        }
                    }
                }
            }
        }
        text.append(" blocks).");
        if (text.getString().contains("walk") && !text.getString().contains(" or "))
            writeWaypoints(waypoints, new Teleport[0], entry, append);
        return text;
    }

    private static Teleport getNearestTeleport(String shard, int x, int y, int z, boolean safe) {
        Teleport nearestTeleport = null;
        int shortestDistance = 999999;

        for (Teleports teleport : Teleports.values()) {
            Teleport tp = teleport.get();
            if (!tp.shard().equals(shard) || tp.isSafe() != safe) continue;

            int distance = getDistanceToTP(tp, x, y, z, true);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearestTeleport = tp;
            }
        }
        return nearestTeleport;
    }

    private static int getDistanceToTP(Teleport tp, int x, int y, int z, boolean correctY) {
        if (tp == null)
            return 999999;
        if (correctY)
            return getCorrectedDistance(tp.x(), tp.y(), tp.z(), x, y, z);
        return getDistance(tp.x(), tp.y(), tp.z(), x, y, z);
    }

    private static int getDistance(int x1, int y1, int z1, int x2, int y2, int z2) {
        return (int) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2));
    }

    private static int getCorrectedDistance(int x1, int y1, int z1, int x2, int y2, int z2) {
        return (int) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) * 10 + Math.pow(z1 - z2, 2));
    }

    private static Text addHubLocations(Teleport tp1, Teleport tp2, String shard, WaypointEntry entry, boolean alt, boolean append) {
        if (tp1 == tp2) return Text.of("walk");
        MutableText text = Text.empty().append("through ");

        Teleport hub = getRegionHub(shard).get();

        if (tp1.name().endsWith("Bell") || tp2.name().endsWith("Bell")) {
            if (tp1.name().endsWith("Bell") && tp2.name().endsWith("Bell")) {
                writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, tp2}, entry, append);
                text.append(tp1.name() + " -> " + tp2.name());
            } else {
                if (tp1.name().endsWith("Bell")) {
                    if (tp2.name().equals(hub.name())) {
                        writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, Chantry.get(), Galengarde.get()}, entry, append);
                        text.append(tp1.name() + " -> Chantry -> Galengarde");
                    } else {
                        if (tp2.name().equals("Chantry of Repentance")) {
                            writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, Chantry.get()}, entry, append);
                            text.append(tp1.name() + " -> " + tp2.name());
                        }
                        else {
                            writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, Chantry.get(), Galengarde.get(), tp2}, entry, append);
                            text.append(tp1.name() + " -> Chantry -> Galengarde -> " + tp2.name());
                        }
                    }
                } else {
                    if (tp1.name().startsWith("Chantry")) {
                        writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, tp2}, entry, append);
                        text.append(tp1.name() + " -> " + tp2.name());
                    } else {
                        if (tp1.name().equals(hub.name())) {
                            writeWaypoints(alt ? altPath : waypoints, new Teleport[]{Galengarde.get(), Chantry.get(), tp2}, entry, append);
                            text.append("Galengarde -> Chantry -> " + tp2.name());
                        } else {
                            writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, Galengarde.get(), Chantry.get(), tp2}, entry, append);
                            text.append(tp1.name() + " -> Galengarde -> Chantry -> " + tp2.name());
                        }
                    }
                }
            }
        } else {
            if (tp1.name().equals(hub.name()) || tp2.name().equals(hub.name())) {
                writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, tp2}, entry, append);
                text.append(tp1.name() + " -> " + tp2.name());
            } else {
                writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, hub, tp2}, entry, append);
                text.append(tp1.name() + " -> " + hub.name() + " -> " + tp2.name());
            }
        }

        if (alt) return text.setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handbook waypoint alternate"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to use this route instead"))));
        return text;
    }

    private static void writeWaypoints(Collection<WaypointEntry> collection, Teleport[] teleports, WaypointEntry entry, boolean append) {
        if (append) return;
        collection.clear();
        for (Teleport tp : teleports) {
            collection.add(new WaypointEntry(tp.name(), tp.name() + " reached.", new Waypoint(tp.x(), tp.y(), tp.z()), false, null));
        }
        collection.add(entry);
    }

    private static boolean shouldSuggestPath(WaypointEntry entry) {
        String shard = client.world.getRegistryKey().getValue().toString().replace("monumenta:", "").split("-")[0];
        String message = getFastestPath(shard, entry, true).getString();
        return !message.contains("walk") || message.contains(" or ");
    }

    public static void setAltPath(boolean append) {
        if (append) {
            if (altPath.isEmpty()) {
                client.inGameHud.getChatHud().addMessage(Text.of("Something's broken. Don't click that."));
                return;
            }
            WaypointEntry target = altPath.stream().toList().get(altPath.size() - 1);
            while (!waypoints.peek().getTitle().equals(target.getTitle())) waypoints.poll();
            List<WaypointEntry> remainingPath = waypoints.stream().toList();
            waypoints.clear();
            waypoints.addAll(altPath);
            waypoints.addAll(remainingPath);
            client.inGameHud.getChatHud().addMessage(Text.of("Waypoints restored."));
        }
        else {
            waypoints.clear();
            waypoints.addAll(altPath);
            client.inGameHud.getChatHud().addMessage(Text.of("Route changed."));
        }
    }

    public static void saveWaypoints() {
        altPath.clear();
        altPath.addAll(waypoints);
    }

    public static boolean waypointsSaved() {
        return !altPath.isEmpty();
    }

    public static boolean shouldRestore() {
        return sendRestoreMessage;
    }

    public static void prepareRestoreMessage() {
        sendRestoreMessage = true;
    }

    public static void sendRestoreMessage() {
        sendRestoreMessage = false;
        client.inGameHud.getChatHud().addMessage(Text.literal("Restore waypoints")
                .setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handbook waypoint alternate"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to restore handbook waypoints")))));
    }

    private static Teleports getRegionHub(String shard) {
        switch (shard) {
            case "valley" -> {
                return Sierhaven;
            }
            case "isles" -> {
                return Mistport;
            }
            case "ring" -> {
                return Galengarde;
            }
        }
        return Empty;
    }

    private static boolean isInPlayableArea(String shard, ClientPlayerEntity player) {
        int x = (int) player.getX();
        int z = (int) player.getZ();
        switch (shard) {
            case "valley" -> {
                return x > -1800 && x < 1720 && z > -690 && z < 800;
            }
            case "isles" -> {
                return x > -2222 && x < 870 && z > -660 && z < 1900;
            }
            case "ring" -> {
                return x > -1160 && x < 980 && z > -1130 && z < 1825;
            }
        }
        return true;
    }
}