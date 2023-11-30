package net.handbook.main.widget;

import net.handbook.main.HandbookClient;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.Waypoint;
import net.handbook.main.resources.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.StringTokenizer;

public class DisplayWidget extends ClickableWidget {

    private Entry entry;
    private String[] description = new String[]{};
    private final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

    public DisplayWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
        if (entry == null) return;

        description = splitText(entry.getText());

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
        if (entry == null) return;

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

        super.render(context, mouseX, mouseY, delta);
    }

    public String[] splitText(String text) {
        StringTokenizer tokens = new StringTokenizer(text, " ");
        StringBuilder output = new StringBuilder();
        int lineLength = 0;
        int maxLength = (this.width - 100) / 6;
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

    public void shareLocation() {
        if (MinecraftClient.getInstance().player == null) return;
        MinecraftClient.getInstance().player.networkHandler.sendChatMessage(
                entry.getClearTitle() + " (" + entry.getTextFields().get("shard").replace("Shard: ", "")
                        + ") | [" + entry.getTextFields().get("position") + "]");

        if (MinecraftClient.getInstance().currentScreen == null) return;
        MinecraftClient.getInstance().currentScreen.close();
    }

    public void openTrades() {
        if (MinecraftClient.getInstance().player == null) return;
        HandbookClient.LOGGER.info("OPENING A PREVIEW TRADE SCREEN");
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        MerchantScreenHandler screenHandler = new MerchantScreenHandler(0, inventory);
        screenHandler.setOffers(entry.getOffers());
        MinecraftClient.getInstance().setScreen(new MerchantScreen(screenHandler, inventory,
                Text.of(entry.getClearTitle() + " (Preview)").getWithStyle(Style.EMPTY.withColor(Formatting.RED)).get(0)));
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
