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
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
public class NPCWriter {

    public static final NPCWriter INSTANCE = read();
    final transient MinecraftClient client = MinecraftClient.getInstance();

    String type = "trader";
    String title = "NPC";
    final List<NPC> entries = new ArrayList<>();
    static int x;
    static int y;
    static int z;

    transient TraderCategory blacklist;
    private transient int newCount = 0;

    //returns int because it's used in command
    @SuppressWarnings("SameReturnValue")
    public int addNPC(Entity entity, boolean manual) {
        if (!HandbookConfig.INSTANCE.editorMode && manual) {
            client.inGameHud.getChatHud().addMessage(Text.literal("Editor mode is disabled."));
            return 1;
        }
        ClientWorld world = client.world;
        if (world == null) return 1;
        if (!entity.hasCustomName() || entity.getScoreboardTeam() == null || !entity.getScoreboardTeam().getName().equals("UNPUSHABLE_TEAM")) {
            if (manual) client.inGameHud.getChatHud().addMessage(
                    Text.of("§cERROR: This entity can not be added."));
            return 1;
        }

        for (NPC npc : entries) {
            if (npc.title.equals(entity.getCustomName().getString())
                    && npc.id.equals(NPC.getID(entity.getCustomName().getString(), entity.getX(), entity.getY(), entity.getZ()))) {
                if (manual) client.inGameHud.getChatHud().addMessage(
                        Text.of("§cERROR: NPC is already added."));
                return 1;
            }
        }
        for (Entry entry : blacklist.getEntries()) {
            if (entry.getID().equals(NPC.getID(entity.getCustomName().getString(), entity.getX(), entity.getY(), entity.getZ()))) {
                if (manual) client.inGameHud.getChatHud().addMessage(
                        Text.of("§cERROR: NPC is already added."));
                return 1;
            }
        }

        if (manual) client.inGameHud.getChatHud().addMessage(
                Text.of("Added new NPC: " + entity.getCustomName().getString()));
        HandbookClient.LOGGER.info("ADDING NEW NPC: {} {}", entity.getCustomName().getString(), entity.getType());

        newCount++;
        entries.add(new NPC(entity.getCustomName().getString(), WaypointManager.getShard(),
                entity.getX(), entity.getY(), entity.getZ()));
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    public int massDelete(int[] area) {
        if (!HandbookConfig.INSTANCE.editorMode) {
            client.inGameHud.getChatHud().addMessage(Text.literal("Editor mode is disabled."));
            return 1;
        }
        int counter = 0;
        for (NPC npc : new ArrayList<>(entries)) {
            int[] pos = npc.position;
            if (!(npc.shard.equals(WaypointManager.getShard())
                    && pos[0] < Math.max(area[0], area[3]) && pos[0] > Math.min(area[0], area[3])
                    && pos[1] < Math.max(area[1], area[4]) && pos[1] > Math.min(area[1], area[4])
                    && pos[2] < Math.max(area[2], area[5]) && pos[2] > Math.min(area[2], area[5]))) continue;
            entries.remove(npc);
            counter++;
        }
        AreaSelector.finish();
        client.inGameHud.getChatHud().addMessage(Text.of("Deleted " + counter + " NPCs. "
                + (counter > 0 ? counter > 10 ? "What a massacre..." : "Informative and unfortunate..." : "Swing and a miss...")));
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    public int clearTrades() {
        if (!HandbookConfig.INSTANCE.editorMode) {
            client.inGameHud.getChatHud().addMessage(Text.literal("Editor mode is disabled."));
            return 1;
        }
        final int[] counter = {0};
        try (Stream<Path> paths = Files.list(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/"))) {
            paths.forEach(path -> {
                String name = path.getFileName().toString();
                if (!name.endsWith(".txt")) return;

                String id = name.replace(".txt", "");
                for (NPC npc : entries) {
                    if (npc.id.equals(id)) return;
                }
                try {
                    Files.delete(path);
                    HandbookClient.LOGGER.info("deleted trades file: {}", path);
                    counter[0]++;
                } catch (Exception ignored) {
                    //don't care
                }
            });
            client.inGameHud.getChatHud().addMessage(Text.of("Deleted " + counter[0] + " (hopefully) unused trade files. "
                    + (counter[0] > 0 ? "Why though?" : "hm?")));
        } catch (Exception ignored) {
            //don't care
        }
        return 1;
    }

    public void deleteEntry(String id) {
        for (NPC entry : entries) {
            if (entry.id.equals(id)) {
                entries.remove(entry);
                client.inGameHud.getChatHud().addMessage(Text.of("Entry removed: " + id));
                blacklist.getEntries().add(new TraderEntry(null, null, null, null, null, id));
                try {
                    Files.deleteIfExists(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id + ".txt"));
                } catch (IOException e) {
                    //don't care
                }
                return;
            }
        }
        client.inGameHud.getChatHud().addMessage(Text.of("Unable to delete this entry"));
    }

    public void addOffers(TradeOfferList offers) {
        String name;
        if (client.currentScreen instanceof MerchantScreen) {
            name = client.currentScreen.getTitle().getString();
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
        client.inGameHud.getChatHud().addMessage(Text.of("Saved \"npcs.json\" with " + entries.size() +
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
                if (!npc.updateOffers) continue;
                npc.updateOffers = false;

                Files.write(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + npc.id + ".txt"), compressTrades(npc.offers));
                HandbookClient.LOGGER.info("Saving trades file {}.txt", npc.id);
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
