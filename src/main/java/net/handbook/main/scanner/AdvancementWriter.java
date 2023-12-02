package net.handbook.main.scanner;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class AdvancementWriter {

    public static void dumpAdvancements(String root) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return;

        AdvancementManager manager = MinecraftClient.getInstance().getNetworkHandler().getAdvancementHandler().getManager();
        for (Advancement advancement : manager.getRoots()) {
            if (advancement.getDisplay() == null) continue;

            if (!advancement.getDisplay().getTitle().getString().equalsIgnoreCase(root)) continue;

            StringBuilder output = new StringBuilder();
            output.append("{\"type\":\"normal\",\"title\":\"Advancements\",\"entries\":[");

            output = childrenLoop(output, advancement, true);

            output.append("]}");

            try {
                Files.write(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/" + advancement.getDisplay().getTitle().getString() + ".json"),
                        output.toString().replace("\n", "").replace(",]}{", "]},{").getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Dump successful."));
            return;
        }

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§cERROR: No advancement tree with this root."));
    }

    private static StringBuilder childrenLoop(StringBuilder output, Advancement advancement, boolean loopFurther) {
        if (((Set<Advancement>) advancement.getChildren()).size() > 0) {
            output.append("{\"title\":\"").append(advancement.getDisplay().getTitle().getString())
                    .append("\",\"text\":\"").append(advancement.getDisplay().getDescription().getString());
            output.append("\",\"children\":[");

            for (Advancement child : advancement.getChildren()) {
                if (loopFurther) output = childrenLoop(output, child, false);
                else output = sameLevelLoop(output, child);
            }
            output.append("]}");
        }
        else {
            if (advancement.getDisplay() == null) return output;
            output.append("{\"title\":\"").append(advancement.getDisplay().getTitle().getString())
                    .append("\",\"text\":\"").append(advancement.getDisplay().getDescription().getString())
                    .append("\"},");
        }
        return output;
    }

    private static StringBuilder sameLevelLoop(StringBuilder output, Advancement advancement) {
        if (advancement.getDisplay() == null) return output;

        if (((Set<Advancement>) advancement.getChildren()).size() > 0) {
            output.append("{\"title\":\"").append(advancement.getDisplay().getTitle().getString())
                    .append("\",\"text\":\"").append(advancement.getDisplay().getDescription().getString());
            output.append("\"},");

            for (Advancement child : advancement.getChildren()) {
                output = sameLevelLoop(output, child);
            }
        }
        else {
            if (advancement.getDisplay() == null) return output;
            output.append("{\"title\":\"").append(advancement.getDisplay().getTitle().getString())
                    .append("\",\"text\":\"").append(advancement.getDisplay().getDescription().getString())
                    .append("\"},");
        }
        return output;
    }
}
