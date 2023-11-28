package net.handbook.main.scanner;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOfferList;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class NPCWriter {

    String type = "trader";
    String title = "NPC";
    final List<NPC> entries = new ArrayList<>();
    private transient int newCount = 0;

    @SuppressWarnings("ConstantConditions")
    public void findEntities() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;

        world.getEntities().forEach(entity -> {
            if (!entity.hasCustomName()) return;
            if (entity.getType().equals(EntityType.ARMOR_STAND)) return;
            if (entity.getType().equals(EntityType.MARKER)) return;
            if (entity.getType().equals(EntityType.AREA_EFFECT_CLOUD)) return;

            if (entity.getScoreboardTeam() == null) return;
            if (!entity.getScoreboardTeam().getName().equals("UNPUSHABLE_TEAM")) return;

            for (NPC npc : entries) {
                if (npc.title.equals(entity.getCustomName().getString())) return;
            }
            HandbookClient.LOGGER.info("ADDING NEW NPC: " + entity.getCustomName().getString() + " " + entity.getType());

            newCount++;
            entries.add(new NPC(entity.getCustomName().getString(), world.getRegistryKey().getValue().toString(), entity.getX(), entity.getY(), entity.getZ()));
        });
    }


    public void addOffers(TradeOfferList offers) {
        String name;
        if (MinecraftClient.getInstance().currentScreen instanceof MerchantScreen) {
            name = MinecraftClient.getInstance().currentScreen.getTitle().getString();
        }
        else return;

        for (NPC npc : entries) {
            if (npc.title.equals(name)) {
                npc.setOffers(offers);
                HandbookClient.LOGGER.info("ADDING TRADES TO NPC " + npc.title);
                return;
            }
        }
    }

    public void write() {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Saved \"npcs.json\" with " + entries.size() +
                " NPCs total, " + newCount + " new NPCs."));
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir() + "/handbook", "npcs.json");
            file.getParentFile().mkdirs();
            writer = gson.newJsonWriter(new FileWriter(file));
            writer.setIndent("    ");
            gson.toJson(this, NPCWriter.class, writer);
            this.newCount = 0;
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save npcs.json.");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
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
}
