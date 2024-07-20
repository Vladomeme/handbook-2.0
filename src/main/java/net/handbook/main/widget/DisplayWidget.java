package net.handbook.main.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.CategoryWriter;
import net.handbook.main.editor.LocationWriter;
import net.handbook.main.editor.NPCWriter;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.entry.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

public class DisplayWidget extends ClickableWidget {

    private Entry entry;
    public ListWidgetEntry selectedEntry;
    private String[] description = new String[]{};
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;
    private final HandbookScreen screen = HandbookClient.handbookScreen;

    private Identifier id;
    private int imageWidth;
    private int imageHeight;
    public boolean renderImage = false;
    public boolean invalidImage = false;

    public DisplayWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
        if (entry == null) return;

        description = splitText(entry.getText());

        if (entry.hasImage()) {
            try {
                id = new Identifier("handbook", entry.getImage());
                Optional<Resource> resource = client.getResourceManager().getResource(id);
                if (resource.isPresent()) {
                    BufferedImage image = ImageIO.read(resource.get().getInputStream());
                    imageWidth = image.getWidth();
                    imageHeight = image.getHeight();
                    renderImage = true;
                    invalidImage = false;
                }
                else invalidImage = true;
            }
            catch (IOException e) {
                HandbookClient.LOGGER.error("Invalid image name in entry {}", entry.getTitle());
            }
        }
        else renderImage = false;

        screen.displayButtonsState(false);
        if (entry.getShard() != null) {
            screen.setWaypoint.visible = true;
            screen.setWaypoint.active = true;
            screen.shareLocation.visible = true;
            screen.shareLocation.active = true;
        }
        if (entry.hasOffers()) {
            screen.openTrades.visible = true;
            screen.openTrades.active = true;
            if (HandbookConfig.INSTANCE.editorMode) {
                screen.resetTrades.visible = true;
                screen.resetTrades.active = true;
            }
        }
        if (entry.getWaypoints() != null) {
            screen.setWaypoint.visible = true;
            screen.setWaypoint.active = true;
        }
        if ((HandbookConfig.INSTANCE.editorMode)) {
            screen.delete.visible = true;
            screen.delete.active = true;
            screen.edit.visible = true;
            screen.edit.active = true;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (entry == null || !visible) return;

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(getX(), getY(), 1);

        matrices.push();
        matrices.scale(1.75f, 1.75f, 1);
        context.drawText(tr, entry.getTitle(), 5, 0, 16777215, true);
        matrices.pop();

        int y = 20;
        if (entry.getShard() != null) {
            context.drawText(tr, "Shard: " + entry.getShard(), 10, y, HandbookConfig.INSTANCE.textColor, false);
            y += 10;
        }
        int[] coords = entry.getPosition();
        if (coords != null) {
            context.drawText(tr, "Position: " + coords[0] + ", " + coords[1] + ", " + coords[2], 10, y,
                    HandbookConfig.INSTANCE.textColor, false);
            y += 10;
        }
        y += 3;

        for (int i = 0; i < description.length ; i++) {
            context.drawText(tr, description[i], 10, y + i * 10, HandbookConfig.INSTANCE.textColor, false);
        }

        if (renderImage) {
            if (invalidImage) {
                context.drawText(tr, "Invalid image", (int) (width * 0.5), 10, HandbookConfig.INSTANCE.textColor, false);
                super.render(context, mouseX, mouseY, delta);
                return;
            }
            float scale = 1;
            if (imageWidth > width * 0.5 - 10) {
                if (imageHeight > height * 0.8) {
                    scale = (float) Math.min((width * 0.5 - 10) / imageWidth, (imageHeight * 0.8) / imageHeight);
                }
                else scale = (float) (width * 0.5 / imageWidth);
            }
            matrices.push();
            matrices.scale(scale, scale, 2);
            RenderSystem.enableBlend();
            context.drawTexture(id, (int) ((width * 0.5) / scale), (int) (10 / scale), 0, 0,
                    imageWidth, imageHeight, imageWidth, imageHeight);
            RenderSystem.disableBlend();
            matrices.pop();
        }
        matrices.pop();
    }

    private String[] splitText(String text) {
        StringTokenizer tokens = new StringTokenizer(text, " ");
        StringBuilder output = new StringBuilder();
        int lineLength = 0;
        int maxLength = (width / 10);
        if (maxLength <= 0) maxLength = 100;
        while (tokens.hasMoreTokens()) {
            String word = tokens.nextToken();

            if (word.contains("\n")) {
                output.append(word, 0, word.indexOf("\n") + 1);
                word = word.substring(word.indexOf("\n") + 1);
                lineLength = 0;
            }

            while (word.length() > maxLength) {
                if (maxLength - lineLength < 0) break;
                output.append(word, 0, maxLength - lineLength).append("\n");
                word = word.substring(maxLength - lineLength);
                lineLength = 0;
            }

            if (lineLength + word.length() > maxLength) {
                output.append("\n");
                lineLength = 0;
            }
            output.append(word).append(" ");

            lineLength += word.length() + 1;
        }

        return output.toString().split("\n");
    }

    public void setWaypoint() {
        if (client.world == null) return;

        if (entry.getWaypoints() != null) {
            setWaypointChain();
        } else {
            if (entry.getShard().equals(WaypointManager.getShard())) {
                client.inGameHud.getChatHud().addMessage(Text.of("Waypoint set: " + entry.getTitle()));
                WaypointManager.setWaypoint(entry);
            } else {
                client.inGameHud.getChatHud().addMessage(Text.of("§cERROR: This waypoint belongs to a different shard.")
                        .getWithStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.of("If you believe the shard is correct, enable `Spoof World Names` in /peb.")))).get(0));
            }
        }
        if (client.currentScreen == null) return;
        client.currentScreen.close();
    }

    private void setWaypointChain() {
        client.inGameHud.getChatHud().addMessage(Text.of("Path started: " + entry.getTitle()));
        WaypointManager.setWaypointChain(List.of((entry.getWaypoints())));
    }

    public void shareLocation(String command) {
        if (client.player == null) return;
        String position = "Position: " + entry.getPosition()[0] + ", " + entry.getPosition()[1] + ", " + entry.getPosition()[2];
        client.player.networkHandler.sendCommand(command + " "
                + entry.getClearTitle().replaceAll(" \\((.*?)\\)", "")
                + " (" + entry.getShard() + ") | " + position);

        client.currentScreen = null;
    }

    public void deleteEntry() {
        switch (screen.activeCategory.getTitle()) {
            case "Locations" -> LocationWriter.delete(entry);
            case "NPC" -> NPCWriter.delete(entry);
            default -> {
                for (CategoryWriter writer : HandbookClient.writers) {
                    if (!writer.category.equals(screen.activeCategory)) continue;

                    writer.delete(entry);
                    writer.shouldUpdate = true;
                }
            }
        }
        double scroll = screen.optionsWidget.getScrollAmount();
        screen.optionsWidget.setEntries(screen.activeCategory.getEntries(), "entry");
        screen.optionsWidget.setScrollAmount(scroll);
        setEntry(null);
        screen.displayButtonsState(false);
    }

    public void deleteTrade() {
        try {
            Files.deleteIfExists(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + entry.getID() + ".txt"));
        }
        catch (IOException ignored) {}
        client.inGameHud.getChatHud().addMessage(Text.of("Removed trades. Interact with the villager again to update them."));
    }

    public Entry getEntry() {
        return entry;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (active && visible && isValidClickButton(button) && clicked(mouseX, mouseY)) {
            onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }
}
