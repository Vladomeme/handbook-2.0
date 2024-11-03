package net.handbook.main.editor;

import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.config.HandbookConfig;
import net.minecraft.advancement.AdvancementManager;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class AdvancementWriter {

    //returns int because it's used in command
    @SuppressWarnings("SameReturnValue")
    public static int dumpAdvancements(String rootString) {
        if (!HandbookConfig.INSTANCE.editorMode) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("Editor mode is disabled."));
            return 1;
        }
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return 1;

        AdvancementManager manager = MinecraftClient.getInstance().getNetworkHandler().getAdvancementHandler().getManager();
        for (PlacedAdvancement root : manager.getRoots()) {
            if (root.getAdvancement().display().isEmpty()) continue;

            if (!root.getAdvancement().display().get().getTitle().getString().equalsIgnoreCase(rootString)) continue;

            StringBuilder output = new StringBuilder();
            output.append("{\"type\":\"normal\",\"title\":\"Advancements\",\"entries\":[");

            output = childrenLoop(output, root, true);

            output.append("]}");

            try {
                Files.write(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/"
                                + root.getAdvancement().display().get().getTitle().getString() + ".json"),
                        output.toString().replace("\n", "").replace(",]}{", "]},{").getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Dump successful."));
            return 1;
        }

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§cERROR: No advancement tree with this root."));
        return 1;
    }

    private static StringBuilder childrenLoop(StringBuilder output, PlacedAdvancement root, boolean loopFurther) {
        if (!((Set<PlacedAdvancement>) root.getChildren()).isEmpty()) {
            output.append("{\"title\":\"").append(root.getAdvancement().display().get().getTitle().getString())
                    .append("\",\"text\":\"").append(root.getAdvancement().display().get().getDescription().getString());
            output.append("\",\"children\":[");

            for (PlacedAdvancement child : root.getChildren()) {
                if (loopFurther) output = childrenLoop(output, child, false);
                else output = sameLevelLoop(output, child);
            }
            output.append("]}");
        }
        else {
            if (root.getAdvancement().display().isEmpty()) return output;
            output.append("{\"title\":\"").append(root.getAdvancement().display().get().getTitle().getString())
                    .append("\",\"text\":\"").append(root.getAdvancement().display().get().getDescription().getString())
                    .append("\"},");
        }
        return output;
    }

    private static StringBuilder sameLevelLoop(StringBuilder output, PlacedAdvancement root) {
        if (root.getAdvancement().display().isEmpty()) return output;

        if (!((Set<PlacedAdvancement>) root.getChildren()).isEmpty()) {
            output.append("{\"title\":\"").append(root.getAdvancement().display().get().getTitle().getString())
                    .append("\",\"text\":\"").append(root.getAdvancement().display().get().getDescription().getString());
            output.append("\"},");

            for (PlacedAdvancement child : root.getChildren()) {
                output = sameLevelLoop(output, child);
            }
        }
        else {
            if (root.getAdvancement().display().isEmpty()) return output;
            output.append("{\"title\":\"").append(root.getAdvancement().display().get().getTitle().getString())
                    .append("\",\"text\":\"").append(root.getAdvancement().display().get().getDescription().getString())
                    .append("\"},");
        }
        return output;
    }
}
