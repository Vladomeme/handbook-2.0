package net.handbook.main.widget;

import net.handbook.main.HandbookClient;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.entry.TraderEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
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
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
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
        ((TraderEntry) screen.displayWidget.getEntry()).share(world, selectedEntry.trade, shareMode);
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
