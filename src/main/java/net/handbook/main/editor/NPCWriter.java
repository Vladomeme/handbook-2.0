package net.handbook.main.editor;

import com.mojang.serialization.DataResult;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.HandbookTradeOffer;
import net.handbook.main.resources.HandbookTradeOfferList;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.TraderEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.chunk.WorldChunk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
public class NPCWriter {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final ChatHud chat = client.inGameHud.getChatHud();

    public static CategoryWriter<TraderEntry> writer;
    public static CategoryWriter<TraderEntry> blacklist;
    public static final HashMap<String, byte[]> updatedOffers = new HashMap<>();

    public static int tick = 0;

    static int x;
    static int y;
    static int z;

    private static final String PATH = FabricLoader.getInstance().getConfigDir() + "/handbook/trades/";

    @SuppressWarnings("SameReturnValue")
    public static int add(Entity entity, boolean manual) {
        if (manual) {
            String id = getId(entity);
            for (TraderEntry entry : new ArrayList<>(blacklist.entries())) {
                if (!entry.id().equals(id)) continue;
                blacklist.delete(entry);
                break;
            }
        }
        addEntry(entity, manual);
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    public static int delete(int[] area) {
        deleteInArea(area, false);
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    public static int delete(int[] area, boolean shouldBlacklist) {
        deleteInArea(area, shouldBlacklist);
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    public static int clear() {
        clearTrades();
        return 1;
    }

    public static void setCoordinates(double x, double y, double z) {
        NPCWriter.x = (int) x;
        NPCWriter.y = (int) y;
        NPCWriter.z = (int) z;
    }

    public static String getId(Entity entity) {
        return getID(entity.getCustomName().getString(), entity.getX(), entity.getY(), entity.getZ());
    }

    public static String getID(String title, double x, double y, double z) {
        return title.toLowerCase().replaceAll("§.", "").replaceAll("[^A-Za-z0-9]", "")
                + ((int) x + (int) y + (int) z);
    }

    public static void tick() {
        tick++;
        if (tick == 60) {
            tick = 0;
            String shard = WaypointManager.getShard();
            if (shard.equals("valley") || shard.equals("isles") || shard.equals("ring")) updateNearby(40, false);
        }
    }

    private static void addEntry(Entity entity, boolean manual) {
        if (!shouldAdd(entity, manual)) return;

        if (manual) chat.addMessage(Text.of("Added new NPC: " + entity.getCustomName().getString()));
        HandbookClient.LOGGER.info("[Handbook 2.0] New NPC added: {} {}", entity.getCustomName().getString(), entity.getType());

        writer.add(new TraderEntry(entity.getCustomName().getString(), "", "", WaypointManager.getShard(),
                new int[]{(int) entity.getX(), (int) entity.getY(), (int) entity.getZ()}));
    }

    private static boolean shouldAdd(Entity entity, boolean manual) {
        if (!HandbookConfig.INSTANCE.editorMode && manual) {
            chat.addMessage(Text.literal("§cEditor mode is disabled."));
            return false;
        }
        if (!isNPC(entity)) {
            if (manual) chat.addMessage(Text.of("§cERROR: This entity can not be added."));
            return false;
        }
        String newID = getID(entity.getCustomName().getString(), entity.getX(), entity.getY(), entity.getZ());
        for (Entry entry : writer.entries()) { //todo hash ids
            if (!entry.id().equals(newID)) continue;
            if (manual) chat.addMessage(Text.of("§cERROR: NPC is already added."));
            return false;
        }
        if (writer.addLog != null) {
            for (Entry entry : writer.addLog) {
                if (!entry.id().equals(newID)) continue;
                if (manual)
                    chat.addMessage(Text.of("§cERROR: NPC is already added (waiting for writer to be unlocked)."));
                return false;
            }
        }
        switch (WaypointManager.getShard()) {
            case "playerplots", "plots", "guildplots", "build", "zenith", "depths" -> {
                return false;
            }
        }
        for (Entry entry : blacklist.entries()) {
            if (!entry.id().equals(newID)) continue;
            if (manual) chat.addMessage(Text.of("§cERROR: NPC is blacklisted."));
            return false;
        }
        return true;
    }

    @SuppressWarnings("SameReturnValue")
    private static void deleteInArea(int[] area, boolean shouldBlacklist) {
        if (!HandbookConfig.INSTANCE.editorMode) {
            chat.addMessage(Text.literal("§cEditor mode is disabled."));
            return;
        }
        int counter = 0;
        for (TraderEntry entry : new ArrayList<>(writer.entries())) {
            int[] pos = entry.position();
            if (!(entry.shard().equals(WaypointManager.getShard())
                    && pos[0] < Math.max(area[0], area[3]) && pos[0] > Math.min(area[0], area[3])
                    && pos[1] < Math.max(area[1], area[4]) && pos[1] > Math.min(area[1], area[4])
                    && pos[2] < Math.max(area[2], area[5]) && pos[2] > Math.min(area[2], area[5]))) continue;
            if (shouldBlacklist) blacklist.add(new TraderEntry(entry.id(), entry.shard()));
            writer.delete(entry);
            counter++;
        }
        AreaSelector.exitSelection();
        chat.addMessage(Text.of("Deleted " + counter + " NPCs. "
                + (counter > 0 ? counter > 10 ? "What a massacre..." : "Informative and unfortunate..." : "Swing and a miss...")));
    }

    private static boolean isNPC(Entity entity) {
        return entity.hasCustomName() && entity.getScoreboardTeam() != null && entity.getScoreboardTeam().getName().equals("UNPUSHABLE_TEAM");
    }

    @SuppressWarnings("SameReturnValue")
    public static int updateNearby(int radius, boolean manual) {
        int radiusSquared = radius * radius;
        String shard = WaypointManager.getShard();

        int playerX = (int) client.player.getX();
        int playerY = (int) client.player.getY();
        int playerZ = (int) client.player.getZ();

        int minX = playerX - radius;
        int minY = playerY - radius;
        int minZ = playerZ - radius;
        int maxX = playerX + radius;
        int maxY = playerY + radius;
        int maxZ = playerZ + radius;

        List<Pair<String, int[]>> nearbyEntities = client.world.getOtherEntities(client.player, new Box(minX, minY, minZ, maxX, maxY, maxZ), NPCWriter::isNPC)
                .stream()
                .map(entity -> new Pair<>(entity.getCustomName().getString(), new int[]{(int) entity.getX(), (int) entity.getY(), (int) entity.getZ()}))
                .toList();

        List<TraderEntry> nearbyEntries = new ArrayList<>();

        for (TraderEntry entry : writer.entries()) {
            if (!entry.shard().equals(shard) || entry.persistent()) continue;

            int[] entryPos = entry.position();
            if (Math.pow(entryPos[0] - playerX, 2) + Math.pow(entryPos[1] - playerY, 2) + Math.pow(entryPos[2] - playerZ, 2) < radiusSquared) {
                WorldChunk chunk = client.world.getChunk(entryPos[0] >> 4, entryPos[2] >> 4);
                if (chunk != null && !chunk.isEmpty()) {
                    nearbyEntries.add(entry);
                }
            }
        }

        //check if entities aren't fully loaded in
        if (!manual && (nearbyEntities.isEmpty() || (nearbyEntities.size() < nearbyEntries.size() / 3))) return 1;

        loop:
        for (TraderEntry entry : nearbyEntries) {
            int[] entryPos = entry.position();
            for (Pair<String, int[]> entity : nearbyEntities) {
                if (entity.getRight()[0] == entryPos[0] && entity.getRight()[1] == entryPos[1] && entity.getRight()[2] == entryPos[2]
                        && entry.title().contains(entity.getLeft())) continue loop;
            }
            //entity matching an entry wasn't found in the world, try to find an entity with the same name if entry has offers
            if (entry.hasOffers()) {
                for (Pair<String, int[]> entity : nearbyEntities) {
                    if (entity.getLeft().equals(entry.title())) {
                        TraderEntry newEntry = new TraderEntry(entry.title(), null, null, shard, entity.getRight());
                        boolean exists = false;
                        for (TraderEntry substituteEntry : writer.entries()) {
                            if (substituteEntry.id().equals(newEntry.id()) && substituteEntry != entry) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) writer.add(newEntry);

                        Path newPath = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + newEntry.id());
                        if (!Files.exists(newPath)) {
                            Path oldPath = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + entry.id());
                            try {
                                Files.move(oldPath, newPath);
                            }
                            catch (IOException e) {
                                HandbookClient.LOGGER.error("[Handbook 2.0] Failed to rename trade file {}", oldPath);
                                writer.delete(entry);
                                continue loop;
                            }
                        }
                        writer.delete(entry);
                        continue loop;
                    }
                }
            }
            writer.delete(entry);
        }
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    public static int setPersistency(int radius) {
        int radiusSquared = radius * radius;
        String shard = WaypointManager.getShard();

        int playerX = (int) client.player.getX();
        int playerY = (int) client.player.getY();
        int playerZ = (int) client.player.getZ();

        for (TraderEntry entry : writer.entries()) {
            if (!entry.shard().equals(shard) || entry.persistent()) continue;

            int[] entryPos = entry.position();
            if (Math.pow(entryPos[0] - playerX, 2) + Math.pow(entryPos[1] - playerY, 2) + Math.pow(entryPos[2] - playerZ, 2) < radiusSquared) {
                if (!entry.persistent()) {
                    chat.addMessage(Text.of("Entry is now persistent: " + entry.id()));
                    entry.makePersistent();
                    writer.setUpdate();
                }
            }
        }
        return 1;
    }

    private static void clearTrades() {
        if (!HandbookConfig.INSTANCE.editorMode) {
            chat.addMessage(Text.literal("§cEditor mode is disabled."));
            return;
        }
        AtomicInteger counter = new AtomicInteger(0);
        try (Stream<Path> paths = Files.list(Path.of(PATH))) {
            paths.forEach(path -> {
                for (Entry entry : writer.entries()) {
                    if (entry.id().equals(path.getFileName().toString())) return;
                }
                try {
                    Files.delete(path);
                    counter.getAndIncrement();
                    HandbookClient.LOGGER.info("[Handbook 2.0] Deleted trades file: {}", path);
                }
                catch (Exception ignored) {}
            });
            chat.addMessage(Text.of("Deleted " + counter.get() + " (hopefully) unused trade files. "
                    + (counter.get() > 0 ? "Why though?" : "hm?")));
        }
        catch (Exception ignored) {}
    }

    public static void delete(TraderEntry entry) {
        if (writer.delete(entry)) {
            blacklist.add(new TraderEntry(entry.id(), WaypointManager.getShard()));
            try {
                Files.deleteIfExists(Path.of(PATH + entry.id()));
            }
            catch (Exception ignored) {}
            chat.addMessage(Text.of("Entry removed and blacklisted: " + entry.id()));
        }
        else chat.addMessage(Text.of("Failed to delete entry " + entry.id()));
    }

    public static void addOffers(TradeOfferList offers) {
        if (!(client.currentScreen instanceof MerchantScreen screen)) return;

        new Thread(() -> {
            for (TraderEntry entry : writer.entries()) {
                if (!entry.id().equals(getID(screen.getTitle().getString(), x, y, z))) continue;

                ClientPlayNetworkHandler nh = MinecraftClient.getInstance().getNetworkHandler();
                if (nh == null) {
                    HandbookClient.LOGGER.error("[Handbook 2.0] NPCWriter.addOffers() got called outside of a game world.");
                    return;
                }
                DataResult<NbtElement> dataResult = HandbookTradeOfferList.CODEC.encodeStart(
                        nh.getRegistryManager().getOps(NbtOps.INSTANCE),
                        new HandbookTradeOfferList(offers.stream().map(HandbookTradeOffer::fromTradeOffer).toList()));

                if (dataResult.isError()) {
                    HandbookClient.LOGGER.info("[Handbook 2.0] Failed to encode NPC offers: {}", dataResult.error());
                }
                else {
                    dataResult.ifSuccess(nbtElement -> {
                        String offersString = nbtElement.asString();
                        String oldOffers = entry.offersRaw();
                        if (oldOffers == null || !oldOffers.equals(offersString))
                            updatedOffers.put(entry.id(), compressTrades(offersString));
                    });
                }
            }
        }).start();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") //for .mkdirs()
    public static void saveTrades() {
        (new File(PATH)).getParentFile().mkdirs();
        updatedOffers.forEach((id, data) -> {
            if (data.length < 13) { //meaningless small number
                HandbookClient.LOGGER.warn("[Handbook 2.0] Data size for trade file {} is too small: {}. Skipping writing.", id, data.length);
                return;
            }
            try {
                Files.write(Path.of(PATH + id), data);
                HandbookClient.LOGGER.info("[Handbook 2.0] Saved trades file {}", id);
            }
            catch (IOException e) {
                HandbookClient.LOGGER.error("[Handbook 2.0] Failed to write trades file {}", id);
                HandbookClient.LOGGER.error(e.getMessage());
            }
        });
        updatedOffers.clear();
    }

    public static byte[] compressTrades(String string) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream outputStream = new DeflaterOutputStream(byteStream)) {
            outputStream.write(goddamnLoreQuotationMarksFix(string).getBytes());
        }
        catch (IOException e) {
            HandbookClient.LOGGER.error("[Handbook 2.0] Trade data saving failed during compression.");
        }
        return byteStream.toByteArray();
    }

    public static String decompressTrades(byte[] bytes) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (OutputStream outputStream = new InflaterOutputStream(byteStream)) {
            outputStream.write(bytes);
        }
        catch (IOException e) {
            HandbookClient.LOGGER.error("[Handbook 2.0] Trade data reading failed during decompression.");
        }
        return byteStream.toString(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    public static String decompressTradesOld(String text) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (OutputStream outputStream = new InflaterOutputStream(byteStream)) {
            outputStream.write(Base64.getDecoder().decode(text.getBytes()));
        }
        catch (IOException e) {
            HandbookClient.LOGGER.error("[Handbook 2.0] Trade data reading failed during decompression (legacy).");
        }
        return byteStream.toString(StandardCharsets.UTF_8);
    }

    private static String goddamnLoreQuotationMarksFix(String text) {
        return text.replaceAll("(?<![,:\\\\\\[])\"\"(?=[,\\]])", "\\\\\"\"");
    }
}
