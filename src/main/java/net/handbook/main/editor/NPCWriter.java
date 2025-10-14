package net.handbook.main.editor;

import com.mojang.serialization.DataResult;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.HandbookTradeOffer;
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
import net.minecraft.village.TradeOfferList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
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
    static final HashMap<String, byte[]> updatedOffers = new HashMap<>();

    static int x;
    static int y;
    static int z;

    private static final String PATH = FabricLoader.getInstance().getConfigDir() + "/handbook/trades/";

    @SuppressWarnings("SameReturnValue")
    public static int add(Entity entity, boolean manual) {
        if (manual) {
            String id = getId(entity);
            for (TraderEntry entry : new ArrayList<>(blacklist.entries())) {
                if (!entry.getID().equals(id)) continue;
                blacklist.entries().remove(entry);
                blacklist.setUpdate();
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

    private static void addEntry(Entity entity, boolean manual) {
        if (!shouldAdd(entity, manual)) return;

        if (manual) chat.addMessage(Text.of("Added new NPC: " + entity.getCustomName().getString()));
        HandbookClient.LOGGER.info("ADDING NEW NPC: {} {}", entity.getCustomName().getString(), entity.getType());

        writer.entries().add(new TraderEntry(entity.getCustomName().getString(), "", "", WaypointManager.getShard(),
                new int[]{(int) entity.getX(), (int) entity.getY(), (int) entity.getZ()}));
        writer.setUpdate();
    }

    private static boolean shouldAdd(Entity entity, boolean manual) {
        if (!HandbookConfig.INSTANCE.editorMode && manual) {
            chat.addMessage(Text.literal("§cEditor mode is disabled."));
            return false;
        }
        if (!entity.hasCustomName() || entity.getScoreboardTeam() == null || !entity.getScoreboardTeam().getName().equals("UNPUSHABLE_TEAM")) {
            if (manual) chat.addMessage(Text.of("§cERROR: This entity can not be added."));
            return false;
        }
        String newID = getID(entity.getCustomName().getString(), entity.getX(), entity.getY(), entity.getZ());
        for (Entry entry : writer.entries()) {
            if (!entry.getID().equals(newID)) continue;
            if (manual) chat.addMessage(Text.of("§cERROR: NPC is already added."));
            return false;
        }
        switch (WaypointManager.getShard()) {
            case "playerplots", "plots", "guildplots", "build", "zenith", "depths" -> {
                return false;
            }
        }
        for (Entry entry : blacklist.entries()) {
            if (!entry.getID().equals(newID)) continue;
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
            int[] pos = entry.getPosition();
            if (!(entry.getShard().equals(WaypointManager.getShard())
                    && pos[0] < Math.max(area[0], area[3]) && pos[0] > Math.min(area[0], area[3])
                    && pos[1] < Math.max(area[1], area[4]) && pos[1] > Math.min(area[1], area[4])
                    && pos[2] < Math.max(area[2], area[5]) && pos[2] > Math.min(area[2], area[5]))) continue;
            if (shouldBlacklist) blacklist.entries().add(new TraderEntry(entry.getID(), entry.getShard()));
            writer.entries().remove(entry);
            counter++;
        }
        AreaSelector.exitSelection();
        if (counter > 0) writer.setUpdate();
        chat.addMessage(Text.of("Deleted " + counter + " NPCs. "
                + (counter > 0 ? counter > 10 ? "What a massacre..." : "Informative and unfortunate..." : "Swing and a miss...")));
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
                    if (entry.getID().equals(path.getFileName().toString())) return;
                }
                try {
                    Files.delete(path);
                    counter.getAndIncrement();
                    HandbookClient.LOGGER.info("Deleted trades file: {}", path);
                }
                catch (Exception ignored) {}
            });
            chat.addMessage(Text.of("Deleted " + counter.get() + " (hopefully) unused trade files. "
                    + (counter.get() > 0 ? "Why though?" : "hm?")));
        }
        catch (Exception ignored) {}
    }

    public static void delete(TraderEntry entry) {
        if (writer.entries().remove(entry)) {
            blacklist.entries().add(new TraderEntry(entry.getID(), WaypointManager.getShard()));
            try {
                Files.deleteIfExists(Path.of(PATH + entry.getID()));
            }
            catch (Exception ignored) {}
            writer.setUpdate();
            blacklist.setUpdate();
            chat.addMessage(Text.of("Entry removed and blacklisted: " + entry.getID()));
        }
        else chat.addMessage(Text.of("Failed to delete entry " + entry.getID()));
    }

    public static void addOffers(TradeOfferList offers) {
        if (!(client.currentScreen instanceof MerchantScreen screen)) return;

        new Thread(() -> {
            for (TraderEntry entry : writer.entries()) {
                if (!entry.getID().equals(getID(screen.getTitle().getString(), x, y, z))) continue;

                ClientPlayNetworkHandler nh = MinecraftClient.getInstance().getNetworkHandler();
                if (nh == null) {
                    HandbookClient.LOGGER.error("[Handbook] NPCWriter.addOffers() got called outside of a game world.");
                    return;
                }
                DataResult<NbtElement> dataResult = HandbookTradeOffer.LIST_CODEC.encodeStart(
                        nh.getRegistryManager().getOps(NbtOps.INSTANCE),
                        offers.stream().map(HandbookTradeOffer::fromTradeOffer).toList());

                if (dataResult.isError()) {
                    HandbookClient.LOGGER.info("[Handbook] Failed to encode NPC offers: {}", dataResult.error());
                }
                else {
                    dataResult.ifSuccess(nbtElement -> {
                        String offersString = nbtElement.asString();
                        //todo ???
                        //offersString.replace("\\\"", "\"").replace("\\\"", "\\\\\"").replace("\\u0027", "'");
                        String oldOffers = entry.getOffersRaw();
                        if (oldOffers == null || !oldOffers.equals(offersString))
                            updatedOffers.put(entry.getID(), compressTrades(offersString));
                    });
                }
            }
        }).start();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") //for .mkdirs()
    public static void saveTrades() {
        (new File(PATH)).getParentFile().mkdirs();
        updatedOffers.forEach((id, data) -> {
            try {
                Files.write(Path.of(PATH + id), data);
                HandbookClient.LOGGER.info("Saved trades file {}", id);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
        return byteStream.toByteArray();
    }

    public static String decompressTrades(byte[] bytes) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (OutputStream outputStream = new InflaterOutputStream(byteStream)) {
            outputStream.write(bytes);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteStream.toString(StandardCharsets.UTF_8);
    }

    public static String decompressTradesOld(String text) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (OutputStream outputStream = new InflaterOutputStream(byteStream)) {
            outputStream.write(Base64.getDecoder().decode(text.getBytes()));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteStream.toString(StandardCharsets.UTF_8);
    }

    private static String goddamnLoreQuotationMarksFix(String text) {
        return text.replaceAll("(?<![,:\\\\\\[])\"\"(?=[,\\]])", "\\\\\"\"");
    }
}
