package net.handbook.main.feature;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.WaypointEntry;
import net.handbook.main.resources.waypoint.Teleport;
import net.handbook.main.resources.waypoint.Teleports;
import net.handbook.main.resources.waypoint.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
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

@SuppressWarnings("SameReturnValue")
public class WaypointManager {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final ChatHud chat = client.inGameHud.getChatHud();

    private static final Queue<WaypointEntry> waypoints = new LinkedList<>();
    private static final List<WaypointEntry> altPath = new ArrayList<>();
    private static int tick;
    private static double distance;
    private static boolean paused = false;
    private static String prevShard;
    private static boolean sendRestoreMessage = false;

    private static final Identifier BEAM_TEXTURE = new Identifier("textures/entity/beacon_beam.png");

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

    //returns int because it's used in command
    public static int setWaypoint(WaypointEntry entry) {
        WaypointManager.waypoints.clear();
        WaypointManager.waypoints.add(entry);

        setState(true);
        tick = 0;

        if (client.world == null) return 1;
        chat.addMessage(getFastestPath(getShard(), entry, false));
        return 1;
    }

    public static void setWaypointChain(List<WaypointEntry> waypoints) {
        WaypointManager.waypoints.clear();
        for (WaypointEntry entry : waypoints) WaypointManager.waypoints.add(entry.markAsChain());

        setState(true);
        tick = 0;

        if (client.world == null) return;
        chat.addMessage(getFastestPath(getShard(), waypoints.get(0), true));
    }

    public static void setState(boolean state) {
        if (HandbookScreen.clearWaypoint == null) return;
        if (!state) waypoints.clear();
        paused = false;
        prevShard = getShard();
        HandbookScreen.clearWaypoint.visible = state;
        HandbookScreen.clearWaypoint.active = state;
        HandbookScreen.continueWaypoint.visible = state;
        HandbookScreen.continueWaypoint.active = state;
    }

    public static void continueOrSkip() {
        if (paused) continuePath();
        else onWaypointReached(client.player, client.world);
    }

    public static void emitParticles() {
        if (tick > 40 || paused) return;

        if (waypoints.peek() == null) return;
        Waypoint waypoint = waypoints.peek().getWaypoint();
        if (waypoint == null) return;

        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world == null || player == null) return;
        if (prevShard != null && !prevShard.equals(getShard())) {
            waypoints.clear();
            return;
        }
        distance = Math.sqrt(Math.pow(player.getX() - waypoint.x(), 2) + Math.pow(player.getY() - waypoint.y(), 2)
                + Math.pow(player.getZ() - waypoint.z(), 2));
        if (distance < 5 || (waypoints.size() > 1 && distance < 10)) onWaypointReached(player, world);

        double particleX = player.getX() + ((waypoint.x() - player.getX()) / distance) * ((float) tick / 3);
        double particleY = player.getY() + ((waypoint.y() - player.getY()) / distance) * ((float) tick / 3);
        double particleZ = player.getZ() + ((waypoint.z() - player.getZ()) / distance) * ((float) tick / 3);

        world.addParticle(HandbookConfig.INSTANCE.monuParticles ? ParticleTypes.COMPOSTER : ParticleTypes.END_ROD,
                particleX + (Math.random() - Math.random()) * 0.5,
                particleY + 0.5 + (Math.random() - Math.random()) * 0.5,
                particleZ + (Math.random() - Math.random()) * 0.5, 0, 0, 0);
    }

    public static void renderBeacon(WorldRenderContext context) {
        if (waypoints.peek() == null || paused || !HandbookConfig.INSTANCE.renderBeacon) return;
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

            while (!waypointEntry.equals(waypoints.peek())) waypoints.poll();
            return;
        }
    }

    //returns int because it's used in command
    public static int onWaypointReached(ClientPlayerEntity player, ClientWorld world) {
        if (paused || waypoints.isEmpty()) return 1;

        world.playSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 2.0f, 1.7f, false);

        if (waypoints.size() == 1) {
            chat.addMessage(Text.of(
                    ((waypoints.peek().getText() == null || waypoints.peek().getText().isEmpty())
                            ? "" : (waypoints.poll().getText() + " ")) + "§aWaypoint removed."));
            setState(false);
            return 1;
        }
        if (waypoints.peek().shouldPause() && !HandbookConfig.INSTANCE.alwaysContinue) {
            chat.addMessage(Text.of(waypoints.poll().getText()));
            chat.addMessage(buildClickableMessage("[Continue]",
                    "/handbook waypoint continue", "Click to set the next waypoint"));
            paused = true;
            return 1;
        }
        WaypointEntry waypoint = waypoints.poll();
        chat.addMessage(Text.of(waypoint.getText() + " Head to " + waypoints.peek().getClearTitle()));
        MutableText text = buildClickableMessage("[Skip]",
                "/handbook waypoint skip", "Click to skip this waypoint");

        if (waypoints.peek().inChain() && shouldSuggestPath(waypoint))
            text.append(Text.literal(" ").setStyle(Style.EMPTY.withUnderline(false)))
                    .append(buildClickableMessage("[Add fastest path]",
                            "/handbook waypoint path", "Click to find fastest path"));
        chat.addMessage(text);
        return 1;
    }

    //returns int because it's used in command
    public static int continuePath() {
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (player == null || world == null) return 1;

        world.playSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 2.0f, 1.7f, false);

        paused = false;
        chat.addMessage(Text.of("Head to " + waypoints.peek().getClearTitle()));
        MutableText text = buildClickableMessage("[Skip]",
                "/handbook waypoint skip", "Click to skip this waypoint");
        if (waypoints.peek().inChain() && shouldSuggestPath(waypoints.peek()))
            text.append(Text.literal(" ").setStyle(Style.EMPTY.withUnderline(false)))
                    .append(buildClickableMessage("[Add fastest path]",
                            "/handbook waypoint path","Click to find fastest path"));
        chat.addMessage(text);
        return 1;
    }

    //returns int because it's used in command
    public static int addPathToChain() {
        WaypointEntry target = waypoints.poll();
        List<WaypointEntry> remainingPath = waypoints.stream().toList();
        waypoints.clear();
        chat.addMessage(getFastestPath(getShard(), target, false));
        waypoints.addAll(remainingPath);
        return 1;
    }

    //returns int because it's used in command
    public static int printInfo() {
        if (waypoints.isEmpty()) {
            HandbookClient.LOGGER.info("No waypoints active");
            return 1;
        }

        StringBuilder string = new StringBuilder();
        for (WaypointEntry waypoint : waypoints) {
            string.append(waypoint.getClearTitle()).append(" -> ");
        }
        HandbookClient.LOGGER.info(string.substring(0, string.length() - 3));
        return 1;
    }

    public static Text getFastestPath(String shard, WaypointEntry entry, boolean append) {
        ClientPlayerEntity player = client.player;
        if (player == null) return Text.of("");

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
        StringBuilder text = (new StringBuilder()).append("through ");

        Teleport hub = getRegionHub(shard).get();

        if (tp1.name().endsWith("Bell") || tp2.name().endsWith("Bell")) {
            if (tp1.name().endsWith("Bell") && tp2.name().endsWith("Bell")) {
                writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, tp2}, entry, append);
                text.append(tp1.name()).append(" -> ").append(tp2.name());
            } else {
                if (tp1.name().endsWith("Bell")) {
                    if (tp2.name().equals(hub.name())) {
                        writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, Chantry.get(), Galengarde.get()}, entry, append);
                        text.append(tp1.name()).append(" -> Chantry -> Galengarde");
                    } else {
                        if (tp2.name().equals("Chantry of Repentance")) {
                            writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, Chantry.get()}, entry, append);
                            text.append(tp1.name()).append(" -> ").append(tp2.name());
                        }
                        else {
                            writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, Chantry.get(), Galengarde.get(), tp2}, entry, append);
                            text.append(tp1.name()).append(" -> Chantry -> Galengarde -> ").append(tp2.name());
                        }
                    }
                } else {
                    if (tp1.name().startsWith("Chantry")) {
                        writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, tp2}, entry, append);
                        text.append(tp1.name()).append(" -> ").append(tp2.name());
                    } else {
                        if (tp1.name().equals(hub.name())) {
                            writeWaypoints(alt ? altPath : waypoints, new Teleport[]{Galengarde.get(), Chantry.get(), tp2}, entry, append);
                            text.append("Galengarde -> Chantry -> ").append(tp2.name());
                        } else {
                            writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, Galengarde.get(), Chantry.get(), tp2}, entry, append);
                            text.append(tp1.name()).append(" -> Galengarde -> Chantry -> ").append(tp2.name());
                        }
                    }
                }
            }
        } else {
            if (tp1.name().equals(hub.name()) || tp2.name().equals(hub.name())) {
                writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, tp2}, entry, append);
                text.append(tp1.name()).append(" -> ").append(tp2.name());
            } else {
                writeWaypoints(alt ? altPath : waypoints, new Teleport[]{tp1, hub, tp2}, entry, append);
                text.append(tp1.name()).append(" -> ").append(hub.name()).append(" -> ").append(tp2.name());
            }
        }

        if (alt) return buildClickableMessage(text.toString(),
                "/handbook waypoint alternate", "Click to use this route instead");
        return Text.of(text.toString());
    }

    private static void writeWaypoints(Collection<WaypointEntry> collection, Teleport[] teleports, WaypointEntry entry, boolean append) {
        if (append) return;
        collection.clear();
        for (Teleport tp : teleports) {
            collection.add(new WaypointEntry(tp.name(), tp.name() + " reached.", new Waypoint(tp.x(), tp.y(), tp.z()), false, null));
        }
        collection.add(entry);
    }

    //returns int because it's used in command
    public static int setAltPath() {
        if (waypoints.peek().inChain()) {
            if (altPath.isEmpty()) {
                chat.addMessage(Text.of("Something's broken. Don't click that."));
                return 1;
            }
            WaypointEntry target = altPath.stream().toList().get(altPath.size() - 1);
            while (!waypoints.peek().getTitle().equals(target.getTitle())) waypoints.poll();
            List<WaypointEntry> remainingPath = waypoints.stream().toList();
            waypoints.clear();
            waypoints.addAll(altPath);
            waypoints.addAll(remainingPath);
        } else {
            waypoints.clear();
            waypoints.addAll(altPath);
        }
        chat.addMessage(Text.of("Route changed."));
        return 1;
    }

    //returns int because it's used in command
    public static int restoreWaypoints() {
        waypoints.clear();
        waypoints.addAll(altPath);
        chat.addMessage(Text.of("Waypoints restored."));
        return 1;
    }

    public static void saveWaypoints() {
        altPath.clear();
        altPath.addAll(waypoints);
        waypoints.clear();
    }

    public static void prepareRestoreMessage() {
        sendRestoreMessage = true;
    }

    public static void sendRestoreMessage() {
        sendRestoreMessage = false;
        if (prevShard.equals(getShard())) {
            chat.addMessage(buildClickableMessage("Restore waypoints",
                    "/handbook waypoint restore", "Click to restore handbook waypoints"));
        }
    }

    public static MutableText buildClickableMessage(String text, String command, String hoverText) {
        return Text.literal(text)
                .setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(hoverText))));
    }

    public static void clear() {
        waypoints.clear();
    }

    public static String getShard() {
        return client.world.getRegistryKey().getValue().toString().replace("monumenta:", "").split("-")[0];
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

    public static double getDistance() {
        return distance;
    }

    public static boolean isActive() {
        return !waypoints.isEmpty();
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

    private static boolean shouldSuggestPath(WaypointEntry entry) {
        String message = getFastestPath(getShard(), entry, true).getString();
        return !message.contains("walk") || message.contains(" or ");
    }

    public static boolean waypointsSaved() {
        return !altPath.isEmpty();
    }

    public static boolean shouldRestore() {
        return sendRestoreMessage;
    }
}