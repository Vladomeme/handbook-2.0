package net.handbook.main.widget;

import net.handbook.main.HandbookClient;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.entry.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class TradesWidget extends ClickableWidget {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;
    private final HandbookScreen screen = HandbookClient.handbookScreen;

    private String name;
    private TradeListWidgetEntry selectedEntry;
    private Mode shareMode;
    private int buttonsY;

    public TradesWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.of(""));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(getX(), getY(), 100);

        matrices.push();
        matrices.scale(1.75f, 1.75f, 1);
        context.drawText(tr, name, 5, 0, 16777215, true);
        matrices.pop();
        matrices.pop();
    }

    public void startSharing(TradeListWidgetEntry entry) {
        cancelSharing();
        selectedEntry = entry;
        entry.setHighlighted(true);

        screen.tradeButtonsState(true);
        buttonsY = (int) client.mouse.getY() / client.options.getGuiScale().getValue() + 24;
        screen.moveTradeButtons(getX() + 140, buttonsY - 18);
        screen.shareCancel.active = true;
        screen.shareCancel.visible = true;
        screen.shareCancel.setPosition(getX() + 141, buttonsY + 12);
    }

    public void selectMode(Mode mode) {
        shareMode = mode;
        screen.worldButtonsState(true);
        screen.moveWorldButtons(getX() + 182, buttonsY + 6);
        screen.shareCancel.setPosition(getX() + 141, buttonsY + 12);
    }

    public void share(String world) {
        if (MinecraftClient.getInstance().player == null) return;
        StringBuilder command = new StringBuilder();
        command.append(world).append(" ");
        ItemStack item;
        Entry entry = screen.displayWidget.getEntry();
        String position = "Position: " + entry.getPosition()[0] + ", " + entry.getPosition()[1] + ", " + entry.getPosition()[2];
        switch (shareMode) {
            case COST -> {
                item = selectedEntry.trade.getOriginalFirstBuyItem();
                command.append(item.getName().getString());
                if (item.getCount() != 1) command.append(" x").append(item.getCount());

                item = selectedEntry.trade.getSecondBuyItem();
                if (!item.isEmpty()) {
                    command.append(" + ").append(item.getName().getString());
                    if (item.getCount() != 1) command.append(" x").append(item.getCount());
                }

                item = selectedEntry.trade.getSellItem();
                command.append(" -> ").append(item.getName().getString());
                if (item.getCount() != 1) command.append(" x").append(item.getCount());

                command.append(" | ").append(entry.getClearTitle()).append(" (")
                        .append(entry.getShard()).append(")");
            }
            case TRADER -> {
                item = selectedEntry.trade.getSellItem();
                command.append(item.getName().getString());
                if (item.getCount() != 1) command.append(" x").append(item.getCount());

                command.append(" | ").append(entry.getClearTitle()).append(" (")
                        .append(entry.getShard()).append(") ")
                        .append(position);
            }
            case FULL -> {
                item = selectedEntry.trade.getOriginalFirstBuyItem();
                command.append(item.getName().getString());
                if (item.getCount() != 1) command.append(" x").append(item.getCount());

                item = selectedEntry.trade.getSecondBuyItem();
                if (!item.isEmpty()) {
                    command.append(" + ").append(item.getName().getString());
                    if (item.getCount() != 1) command.append(" x").append(item.getCount());
                }

                item = selectedEntry.trade.getSellItem();
                command.append(" -> ").append(item.getName().getString());
                if (item.getCount() != 1) command.append(" x").append(item.getCount());

                command.append(" | ").append(entry.getClearTitle()).append(" (")
                        .append(entry.getShard()).append(") ")
                        .append(position);
            }
        }
        MinecraftClient.getInstance().player.networkHandler.sendCommand(command.toString());

        if (MinecraftClient.getInstance().currentScreen == null) return;
        MinecraftClient.getInstance().currentScreen.close();
    }

    public void cancelSharing() {
        if (selectedEntry != null) selectedEntry.setHighlighted(false);
        selectedEntry = null;

        screen.worldButtonsState(false);
        screen.tradeButtonsState(false);
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum Mode {
        COST,
        TRADER,
        FULL
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
