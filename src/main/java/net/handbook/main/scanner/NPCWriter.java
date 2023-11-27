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
import java.util.ArrayList;
import java.util.List;

public class NPCWriter {

    String title = "NPC";
    final List<NPC> entries = new ArrayList<>();

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
            if (npc.title.equals(name)) npc.setOffers(offers);
        }
    }

    public void write() {
        HandbookClient.LOGGER.info("DUMPING " + entries.size() + " NPCs INTO \"handbook_npcs.json\" in /config.");
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Saved handbook_npcs.json in /config with " + entries.size() + " NPCs!"));
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            writer = gson.newJsonWriter(new FileWriter(new File(FabricLoader.getInstance().getConfigDir().toFile(), "handbook_npcs.json")));
            writer.setIndent("    ");
            gson.toJson(this, NPCWriter.class, writer);
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save.");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
