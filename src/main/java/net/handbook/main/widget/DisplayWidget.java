package net.handbook.main.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.entry.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.resource.Resource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

public class DisplayWidget extends ClickableWidget {

    private Entry entry;
    public ListWidgetEntry selectedEntry;
    private String[] description = new String[]{};
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;

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

        HandbookScreen.displayButtonsState(false);
        if (entry.getShard() != null) {
            HandbookScreen.setWaypoint.visible = true;
            HandbookScreen.setWaypoint.active = true;
            HandbookScreen.shareLocation.visible = true;
            HandbookScreen.shareLocation.active = true;
        }
        if (entry.getOffers() != null) {
            HandbookScreen.openTrades.active = true;
            HandbookScreen.openTrades.visible = true;
        }
        if (entry.getWaypoints() != null) {
            HandbookScreen.setWaypoint.visible = true;
            HandbookScreen.setWaypoint.active = true;
        }
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

        if (entry.getShard() != null)
            context.drawText(tr, "Shard: " + entry.getShard(), 10, 20, 16777215, false);
        int[] coords = entry.getPosition();
        if (coords != null)
            context.drawText(tr, "Position: " + coords[0] + ", " + coords[1] + ", " + coords[2], 10, 30, 16777215, false);
        int y = 43;

        for (int i = 0; i < description.length ; i++) {
            context.drawText(tr, description[i], 10, y + i * 10, 16777215, false);
        }

        if (renderImage) {
            if (invalidImage) {
                context.drawText(tr, "Invalid image", (int) (width * 0.5), 10, 16777215, false);
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
            context.getMatrices().push();
            context.getMatrices().scale(scale, scale, 2);
            RenderSystem.enableBlend();
            context.drawTexture(id, (int) ((width * 0.5) / scale), (int) (10 / scale), 0, 0,
                    imageWidth, imageHeight, imageWidth, imageHeight);
            RenderSystem.disableBlend();
            context.getMatrices().pop();
        }
        context.getMatrices().pop();
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

        if (client.currentScreen == null) return;
        client.currentScreen.close();
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
