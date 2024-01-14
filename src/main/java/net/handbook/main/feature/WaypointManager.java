package net.handbook.main.feature;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
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
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.*;

import static net.handbook.main.resources.waypoint.Teleports.*;

public class WaypointManager {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final Queue<WaypointEntry> waypoints = new LinkedList<>();
    static int tick;
    static double distance;

    public static final Identifier BEAM_TEXTURE = new Identifier("textures/entity/beacon_beam.png");

    public static void tick() {
        if (waypoints.isEmpty()) return;

        tick++;
        if (waypoints.size() > 1) checkChain();
        emitParticles();
        if (tick >= 60) tick = 0;
    }

    public static void setWaypoint(Entry entry) {
        setWaypoint(getCoordinates(entry), entry.getTitle(), entry.getText());
    }

    public static void setWaypoint(int[] coordinates, String title, String text) {
        setWaypoint(new WaypointEntry(title, text, new Waypoint(coordinates[0], coordinates[1], coordinates[2])));
    }

    public static void setWaypoint(WaypointEntry entry) {
        WaypointManager.waypoints.clear();
        WaypointManager.waypoints.add(entry);

        setVisibility(true);
        tick = 0;

        if (client.world == null) return;
        String shard = client.world.getRegistryKey().getValue().toString().replace("monumenta:", "").split("-")[0];
        client.inGameHud.getChatHud().addMessage(Text.of(getFastestPath(shard, entry)));
    }

    public static void setWaypointChain(List<WaypointEntry> waypoints) {
        WaypointManager.waypoints.clear();
        WaypointManager.waypoints.addAll(waypoints);

        setVisibility(true);
        tick = 0;

        if (client.world == null) return;
        String shard = client.world.getRegistryKey().getValue().toString().replace("monumenta:", "").split("-")[0];
        client.inGameHud.getChatHud().addMessage(Text.of(getFastestPath(shard, waypoints.get(0))));
    }

    public static void setVisibility(boolean state) {
        if (HandbookScreen.clearWaypoint == null) return;
        if (!state) waypoints.clear();
        HandbookScreen.clearWaypoint.visible = state;
        HandbookScreen.clearWaypoint.active = state;
    }

    public static void emitParticles() {
        if (tick > 40) return;

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
        if (waypoints.peek() == null) return;
        Waypoint waypoint = waypoints.peek().getWaypoint();
        if (waypoint == null) return;

        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world == null || player == null) return;

        double beaconX = distance < 150 ? waypoint.x() - player.getX() : ((waypoint.x() - player.getX()) / distance) * 150;
        double beaconZ = distance < 150 ? waypoint.z() - player.getZ() : ((waypoint.z() - player.getZ()) / distance) * 150;

        context.matrixStack().push();
        context.matrixStack().translate(beaconX, -(player.getY() + 64), beaconZ);
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
            if (distance > 10) continue;

            while (waypointEntry != waypoints.peek()) waypoints.poll();
            return;
        }
    }

    private static void onWaypointReached(ClientPlayerEntity player, ClientWorld world) {
        if (waypoints.size() == 1) {
            client.inGameHud.getChatHud().addMessage(Text.of(
                    waypoints.peek().getText().equals("") ? "" : (waypoints.poll().getText() + " ") + "§aWaypoint removed."));
            setVisibility(false);
        }
        else if (waypoints.size() > 1) {
            client.inGameHud.getChatHud().addMessage(Text.of(
                    waypoints.poll().getText() + " Head to " + waypoints.peek().getClearTitle()));
        }
        world.playSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 2.0f, 1.7f, false);
    }

    public static boolean isActive() {
        return !waypoints.isEmpty();
    }

    public static int[] getCoordinates(Entry entry) {
        return Arrays.stream(entry.getTextFields().get("position").replace("Position:", "")
                .replace(" ", "").split(",", 3)).mapToInt(Integer::parseInt).toArray();
    }

    public static double getDistance() {
        return distance;
    }

    public static String getFastestPath(String shard, WaypointEntry entry) {
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

        //I ate the braces
        if (safeTP == null)
            if (unsafeTP == null) return "Fastest way: walk (~" + straightPath + " blocks).";
            else if (unsafePath > straightPath) return "Fastest way: walk (~" + straightPath + " blocks).";
            else if (straightPath / unsafePath > 2) return "Fastest way: walk (~" + straightPath + " blocks).";
            else
                return "Fastest way: walk (~" + straightPath + " blocks) or " + addHubLocations(nearestTP, unsafeTP, shard, entry)
                        + " (~" + (unsafePath + distanceToTp) + " blocks).";
        else if (unsafeTP == null)
            if (safePath < straightPath) return "Fastest way: "
                    + addHubLocations(nearestTP, safeTP, shard, entry) + " (~" + (safePath + distanceToTp) + " blocks).";
            else return "Fastest way: walk (~" + straightPath + " blocks).";
        else if (safePath < unsafePath)
            if (safePath < straightPath) return "Fastest way: "
                    + addHubLocations(nearestTP, safeTP, shard, entry) + " (~" + (safePath + distanceToTp) + " blocks).";
            else return "Fastest way: walk (~" + straightPath + " blocks).";
        else if (unsafePath > straightPath) return "Fastest way: walk (~" + straightPath + " blocks).";
        else if (safePath < straightPath)
            if (straightPath / unsafePath > 2) return "Fastest way: "
                    + addHubLocations(nearestTP, safeTP, shard, entry) + " (~" + (safePath + distanceToTp) + " blocks) or "
                    + addHubLocations(nearestTP, unsafeTP, shard, entry) + " (~" + (unsafePath + distanceToTp) + " blocks).";
            else return "Fastest way: "
                    + addHubLocations(nearestTP, safeTP, shard, entry) + " (~" + (safePath + distanceToTp) + " blocks).";
        else if (safePath / unsafePath > 2) return "Fastest way: walk (~" + straightPath + " blocks) or "
                + addHubLocations(nearestTP, unsafeTP, shard, entry) + " (~" + (unsafePath + distanceToTp) + " blocks).";
        else return "Fastest way: walk (~" + straightPath + " blocks).";
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

    private static String addHubLocations(Teleport tp1, Teleport tp2, String shard, WaypointEntry entry) {
        if (tp1 == tp2) return "walk";

        Teleport hub = getRegionHub(shard).get();

        if (tp1.name().endsWith("Bell") || tp2.name().endsWith("Bell"))
            if (tp1.name().endsWith("Bell") && tp2.name().endsWith("Bell")) {
                setWaypointsForPath(new Teleport[]{tp1, tp2}, entry);
                return "through " + tp1.name() + " -> " + tp2.name();
            } else if (tp1.name().endsWith("Bell"))
                if (tp2.name().equals(hub.name())) {
                    setWaypointsForPath(new Teleport[]{tp1, Chantry.get(), Galengarde.get()}, entry);
                    return "through " + tp1.name() + " -> Chantry -> Galengarde";
                } else {
                    setWaypointsForPath(new Teleport[]{tp1, Chantry.get(), Galengarde.get(), tp2}, entry);
                    return "through " + tp1.name() + " -> Chantry -> Galengarde -> " + tp2.name();
                }
            else if (tp1.name().startsWith("Chantry")) {
                setWaypointsForPath(new Teleport[]{tp1, tp2}, entry);
                return "through " + tp1.name() + " -> " + tp2.name();
            } else if (tp1.name().equals(hub.name())) {
                setWaypointsForPath(new Teleport[]{Galengarde.get(), Chantry.get(), tp2}, entry);
                return "through Galengarde -> Chantry -> " + tp2.name();
            } else {
                setWaypointsForPath(new Teleport[]{tp1, Galengarde.get(), Chantry.get(), tp2}, entry);
                return "through " + tp1.name() + " -> Galengarde -> Chantry -> " + tp2.name();
            }
        else if (tp1.name().equals(hub.name()) || tp2.name().equals(hub.name())) {
            setWaypointsForPath(new Teleport[]{tp1, tp2}, entry);
            return "through " + tp1.name() + " -> " + tp2.name();
        } else {
            setWaypointsForPath(new Teleport[]{tp1, hub, tp2}, entry);
            return "through " + tp1.name() + " -> " + hub.name() + " -> " + tp2.name();
        }
    }

    private static void setWaypointsForPath(Teleport[] teleports, WaypointEntry entry) {
        waypoints.clear();
        for (Teleport tp : teleports) {
            waypoints.add(new WaypointEntry(tp.name(), tp.name() + " reached.", new Waypoint(tp.x(), tp.y(), tp.z())));
        }
        waypoints.add(entry);
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
