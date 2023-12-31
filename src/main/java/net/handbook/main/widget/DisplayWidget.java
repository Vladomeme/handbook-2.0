package net.handbook.main.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.Waypoint;
import net.handbook.main.resources.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;
import java.util.StringTokenizer;

public class DisplayWidget extends ClickableWidget {

    private Entry entry;
    public ListWidgetEntry selectedEntry;
    private String[] description = new String[]{};
    private final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

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
                Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(id);
                if (resource.isPresent()) {
                    BufferedImage image = ImageIO.read(resource.get().getInputStream());
                    imageWidth = image.getWidth();
                    imageHeight = image.getHeight();
                    renderImage = true;
                    invalidImage = false;
                }
                else {
                    invalidImage = true;
                }
            } catch (IOException e) {
                HandbookClient.LOGGER.error("Invalid image name in entry " + entry.getTitle());
                throw new RuntimeException(e);
            }
        }
        else {
            renderImage = false;
        }

        boolean state = (entry.getTextFields() != null && entry.getTextFields().get("shard") != null);
        HandbookScreen.setWaypoint.visible = state;
        HandbookScreen.setWaypoint.active = state;
        HandbookScreen.shareLocation.visible = state;
        HandbookScreen.shareLocation.active = state;

        state = entry.getOffers() != null;
        HandbookScreen.openTrades.active = state;
        HandbookScreen.openTrades.visible = state;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (entry == null || !visible) return;

        context.getMatrices().push();
        context.getMatrices().translate(getX(), getY(), 1);

        context.getMatrices().push();
        context.getMatrices().scale(1.75f, 1.75f, 1);
        context.drawText(tr, entry.getTitle(), 5, 0, 16777215, true);
        context.getMatrices().pop();

        int y = 20;
        if (entry.getTextFields() != null) {
            for (String text : entry.getTextFields().values()) {
                for (String line : splitText(text)) {
                    context.drawText(tr, line, 10, y, 16777215, false);
                    y = y + 10;
                }
                y = y + 3;
            }
        }

        for (int i = 0; i < description.length ; i++) {
            context.drawText(tr, description[i], 10, y + i * 10, 16777215, false);
        }

        if (renderImage) {
            if (invalidImage) {
                context.drawText(tr, "Invalid image", (int) (this.width * 0.5), 10, 16777215, false);
                super.render(context, mouseX, mouseY, delta);
                return;
            }
            float scale = 1;
            if (imageWidth > this.width * 0.5 - 10) {
                if (imageHeight > this.height * 0.8) {
                    scale = (float) Math.min((this.width * 0.5 - 10) / imageWidth, (this.imageHeight * 0.8) / imageHeight);
                }
                else scale = (float) (this.width * 0.5 / imageWidth);
            }
            context.getMatrices().push();
            context.getMatrices().scale(scale, scale, 2);
            RenderSystem.enableBlend();
            context.drawTexture(id, (int) ((this.width * 0.5) / scale), (int) (10 / scale), 0, 0,
                    imageWidth, imageHeight, imageWidth, imageHeight);
            RenderSystem.disableBlend();
            context.getMatrices().pop();
        }
        context.getMatrices().pop();
    }

    public String[] splitText(String text) {
        StringTokenizer tokens = new StringTokenizer(text, " ");
        StringBuilder output = new StringBuilder();
        int lineLength = 0;
        int maxLength = (this.width / 10);
        if (maxLength <= 0) maxLength = 100;
        while (tokens.hasMoreTokens()) {
            String word = tokens.nextToken();

            if (word.contains("\n")) {
                output.append(word, 0, word.indexOf("\n") + 1);
                word = word.substring(word.indexOf("\n") + 1);
                lineLength = 0;
            }

            while (word.length() > maxLength) {
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
        if (MinecraftClient.getInstance().world == null) return;

        String world = MinecraftClient.getInstance().world.getRegistryKey().getValue().toString().replace("monumenta:", "").split("-")[0];
        if (entry.getTextFields().get("shard").replace("Shard: ", "").equals(world)) {
            Waypoint.setPosition(entry.getTextFields().get("position"));
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Waypoint set: " + entry.getTitle()));
        }
        else {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§cERROR: This waypoint belongs to a different shard."));
        }
        if (MinecraftClient.getInstance().currentScreen == null) return;
        MinecraftClient.getInstance().currentScreen.close();
    }

    public void shareLocation(String command) {
        if (MinecraftClient.getInstance().player == null) return;
        MinecraftClient.getInstance().player.networkHandler.sendCommand(command + " "
                + entry.getClearTitle() + " (" + entry.getTextFields().get("shard").replace("Shard: ", "")
                        + ") | " + entry.getTextFields().get("position"));

        if (MinecraftClient.getInstance().currentScreen == null) return;
        MinecraftClient.getInstance().currentScreen.close();
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
}
