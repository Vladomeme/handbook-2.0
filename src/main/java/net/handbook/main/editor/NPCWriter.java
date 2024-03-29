package net.handbook.main.editor;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.category.TraderCategory;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.TraderEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOfferList;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
public class NPCWriter {

    public static final NPCWriter INSTANCE = read();

    String type = "trader";
    String title = "NPC";
    final List<NPC> entries = new ArrayList<>();
    transient TraderCategory blacklist;
    private transient int newCount = 0;
    static int x;
    static int y;
    static int z;

    public void findEntities() {
        if (!HandbookConfig.INSTANCE.enableScanner) return;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;

        world.getEntities().forEach(entity -> {
            if (entity.getType().equals(EntityType.VILLAGER)) addNPC(entity, false);
        });
    }

    //returns int because it's used in command
    @SuppressWarnings("SameReturnValue")
    public int addNPC(Entity entity, boolean manual) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return 1;
        if (!entity.hasCustomName() || entity.getScoreboardTeam() == null || !entity.getScoreboardTeam().getName().equals("UNPUSHABLE_TEAM")) {
            if (manual) MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.of("§cERROR: This entity can not be added."));
            return 1;
        }

        for (NPC npc : entries) {
            if (npc.title.equals(entity.getCustomName().getString())
                    && npc.id.equals(NPC.getID(entity.getCustomName().getString(), entity.getX(), entity.getY(), entity.getZ()))) {
                if (manual) MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.of("§cERROR: NPC is already added."));
                return 1;
            }
        }
        for (Entry entry : blacklist.getEntries()) {
            if (entry.getID().equals(NPC.getID(entity.getCustomName().getString(), entity.getX(), entity.getY(), entity.getZ()))) {
                if (manual) MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.of("§cERROR: NPC is already added."));
                return 1;
            }
        }

        if (manual) MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                Text.of("Added new NPC: " + entity.getCustomName().getString()));
        HandbookClient.LOGGER.info("ADDING NEW NPC: " + entity.getCustomName().getString() + " " + entity.getType());

        newCount++;
        entries.add(new NPC(entity.getCustomName().getString(), WaypointManager.getShard(),
                entity.getX(), entity.getY(), entity.getZ()));
        return 1;
    }

    public void deleteEntry(String id) {
        for (NPC entry : entries) {
            if (entry.id.equals(id)) {
                entries.remove(entry);
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Entry removed: " + id));
                blacklist.getEntries().add(new TraderEntry(null, null, null, null, null, id));
                try {
                    Files.deleteIfExists(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id + ".txt"));
                } catch (IOException e) {
                    //don't care
                }
                return;
            }
        }
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Unable to delete this entry"));
    }

    public void addOffers(TradeOfferList offers) {
        String name;
        if (MinecraftClient.getInstance().currentScreen instanceof MerchantScreen) {
            name = MinecraftClient.getInstance().currentScreen.getTitle().getString();
        }
        else return;

        for (NPC npc : entries) {
            if (npc.id.equals(NPC.getID(name, x, y, z))) {
                npc.setOffers(offers);
                return;
            }
        }
    }

    public static void setCoordinates(double x, double y, double z) {
        NPCWriter.x = (int) x;
        NPCWriter.y = (int) y;
        NPCWriter.z = (int) z;
    }

    //returns int because it's used in command
    @SuppressWarnings("SameReturnValue")
    public int write() {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Saved \"npcs.json\" with " + entries.size() +
                " NPCs total, " + newCount + " new NPCs."));
        Gson gson = new Gson();
        JsonWriter writer = null;
        //ENTRIES
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir() + "/handbook", "npcs.json");
            file.getParentFile().mkdirs();
            writer = gson.newJsonWriter(new FileWriter(file));
            writer.setIndent("    ");
            gson.toJson(this, NPCWriter.class, writer);
            newCount = 0;
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save npcs.json.");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        //TRADES
        (new File(FabricLoader.getInstance().getConfigDir() + "/handbook/trades")).getParentFile().mkdirs();
        for (NPC npc : entries) {
            if (npc.offers == null || npc.offers.isEmpty()) continue;
            try {
                if (Files.exists(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + npc.id + ".txt"))) continue;

                Files.write(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + npc.id + ".txt"), compressTrades(npc.offers));
                HandbookClient.LOGGER.info("Saving trades file " + npc.id + ".txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //BLACKLIST
        if (blacklist == null) return 1;
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir() + "/handbook", "npc_blacklist.json");
            file.getParentFile().mkdirs();
            writer = gson.newJsonWriter(new FileWriter(file));
            writer.setIndent("    ");
            gson.toJson(blacklist, TraderCategory.class, writer);
            newCount = 0;
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save npc_blacklist.json.");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return 1;
    }

    private static byte[] compressTrades(String text) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream outputStream = new DeflaterOutputStream(byteStream)) {
            outputStream.write(text.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Base64.getEncoder().encode(byteStream.toByteArray());
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

    public static NPCWriter read() {
        Gson gson = new Gson();
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir() + "/handbook", "npcs.json");
            return gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), NPCWriter.class);
        }
        catch (Exception e) {
            HandbookClient.LOGGER.error("Could not find npcs.json in config/handbook/. A new file will be created when dumping,");
        }
        return new NPCWriter();
    }

    public void setBlacklist(TraderCategory blacklist) {
        this.blacklist = blacklist;
    }
}
