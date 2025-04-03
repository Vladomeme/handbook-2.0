package net.handbook.main.editor;

import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.TraderEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
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
        final int[] counter = {0};
        try (Stream<Path> paths = Files.list(Path.of(PATH))) {
            paths.forEach(path -> {
                String name = path.getFileName().toString();
                if (!name.endsWith(".txt")) return;

                String id = name.replace(".txt", "");
                for (Entry entry : writer.entries()) {
                    if (entry.getID().equals(id)) return;
                }
                try {
                    Files.delete(path);
                    counter[0]++;
                    HandbookClient.LOGGER.info("Deleted trades file: {}", path);
                }
                catch (Exception ignored) {}
            });
            chat.addMessage(Text.of("Deleted " + counter[0] + " (hopefully) unused trade files. "
                    + (counter[0] > 0 ? "Why though?" : "hm?")));
        }
        catch (Exception ignored) {}
    }

    public static void delete(TraderEntry entry) {
        if (writer.entries().remove(entry)) {
            blacklist.entries().add(new TraderEntry(entry.getID(), WaypointManager.getShard()));
            try {
                Files.deleteIfExists(Path.of(PATH + entry.getID() + ".txt"));
            }
            catch (Exception ignored) {}
            writer.setUpdate();
            blacklist.setUpdate();
            chat.addMessage(Text.of("Entry removed and blacklisted: " + entry.getID()));
        }
        else chat.addMessage(Text.of("Failed to delete this entry"));
    }

    public static void addOffers(TradeOfferList offers) {
        if (!(client.currentScreen instanceof MerchantScreen screen)) return;

        for (TraderEntry entry : writer.entries()) {
            if (!entry.getID().equals(getID(screen.getTitle().getString(), x, y, z))) continue;

            NbtCompound offersNbt = new NbtCompound();
            NbtList offerList = new NbtList();
            for (TradeOffer tradeOffer : offers) {
                NbtCompound tradeNbt = new NbtCompound();

                tradeNbt.put("buy", tradeOffer.getOriginalFirstBuyItem().writeNbt(new NbtCompound()));
                tradeNbt.put("buyB", tradeOffer.getSecondBuyItem().writeNbt(new NbtCompound()));
                tradeNbt.put("sell", tradeOffer.getSellItem().writeNbt(new NbtCompound()));
                offerList.add(tradeNbt);
            }
            offersNbt.put("Recipes", offerList);
            String newOffers = offersNbt.toString()
                    .replace("\\\"", "\"")
                    .replace("\\\"", "\\\\\"")
                    .replace("\\u0027", "'");
            String oldOffers = entry.getOffersRaw();

            if (oldOffers == null || !oldOffers.equals(newOffers))
                updatedOffers.put(entry.getID(), compressTrades(newOffers));
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") //for .mkdirs()
    public static void saveTrades() {
        (new File(PATH)).getParentFile().mkdirs();
        updatedOffers.forEach((id, data) -> {
            try {
                Files.write(Path.of(PATH + id + ".txt"), data);
                HandbookClient.LOGGER.info("Saved trades file {}.txt", id);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        updatedOffers.clear();
    }

    private static byte[] compressTrades(String text) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream outputStream = new DeflaterOutputStream(byteStream)) {
            outputStream.write(stupidPlainLoreFix(text).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Base64.getEncoder().encode(byteStream.toByteArray());
    }

    private static String stupidPlainLoreFix(String text) {
        return text.replace("THAT.\"\"", "THAT.\\\"\"")
                .replace("hat!\"\"", "hat!\\\"\"");
    }

    public static String decompressTrades(String text) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (OutputStream outputStream = new InflaterOutputStream(byteStream)) {
            outputStream.write(Base64.getDecoder().decode(text.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteStream.toString(StandardCharsets.UTF_8);
    }
}
