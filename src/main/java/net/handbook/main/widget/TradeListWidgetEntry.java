package net.handbook.main.widget;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.HandbookClient;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.TradeScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;

import java.util.List;

@Environment(EnvType.CLIENT)
public class TradeListWidgetEntry extends ElementListWidget.Entry<TradeListWidgetEntry> {

    private final TextRenderer tr = MinecraftClient.getInstance().textRenderer;
    private final HandbookScreen screen = HandbookClient.handbookScreen;
    private static final Identifier TEXTURE = new Identifier("textures/gui/container/villager2.png");

    public final TradeOffer trade;
    public final String id;
    private boolean highlighted = false;

    public final TexturedButtonWidget button;
    public final List<ClickableWidget> list;

    public TradeListWidgetEntry(TradeOffer trade, String id, int width) {
        this.trade = trade;
        this.id = id;

        button = new TexturedButtonWidget(0, 0, width, 20, 0, 0, 0,
                new Identifier("handbook", "empty"), button -> {
            if (MinecraftClient.getInstance().currentScreen instanceof HandbookScreen)
                screen.tradesWidget.startSharing(this);
            else {
                HandbookClient.tradeScreen.setTraderInfo(id.split("&")[0]);
                HandbookClient.tradeScreen.selectedEntry = this;
                setHighlighted(true);
            }
        });
        list = ImmutableList.of(button);
    }

    @Override
    public void render(DrawContext context, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        button.setPosition(left, top);

        RenderSystem.enableBlend();
        if (highlighted) {
            if (screen.markedEntries.getMarkedEntries("favTrades").contains(id))
                context.fill(left, top + 1, left + 110, top + 19, 1358935040);
            else context.fill(left, top + 1, left + 110, top + 19, 1688906410);
            context.fill(left, top + 1, left + 1, top + 19, -1);
            context.fill(left + 109, top + 1, left + 110, top + 19, -1);
            context.fill(left, top + 1, left + 110, top + 2, -1);
            context.fill(left, top + 18, left + 110, top + 19, -1);
        }
        else {
            if (screen.markedEntries.getMarkedEntries("favTrades").contains(id))
                context.fill(left, top + 1, left + 110, top + 19, 2030023680);
            else context.fill(left, top + 1, left + 110, top + 19, 866822826);
        }

        ItemStack itemStack1 = trade.getOriginalFirstBuyItem();
        ItemStack itemStack2 = trade.getSecondBuyItem();
        ItemStack itemStack3 = trade.getSellItem();

        context.drawItem(itemStack1, left + 2, top + 2);
        context.drawItemInSlot(tr, itemStack1, left + 2, top + 2);

        context.drawItem(itemStack2, left + 37, top + 2);
        context.drawItemInSlot(tr, itemStack2, left + 37, top + 2);

        context.drawTexture(TEXTURE, left + 67, top + 5, 0, 15, 171, 10, 9, 512, 256);

        context.drawItem(itemStack3, left + 91, top + 2);
        context.drawItemInSlot(tr, itemStack3, left + 91, top + 2);

        RenderSystem.disableScissor();
        if (isMouseOver(mouseX, mouseY)) renderTooltip(context, mouseX, mouseY, left);
        RenderSystem.disableBlend();
    }

    private void renderTooltip(DrawContext context, int x, int y, int left) {
        if (y > MinecraftClient.getInstance().getWindow().getScaledHeight() - 40) return;
        ItemStack itemStack = null;

             if (x > left 	   && x < left + 20)  itemStack = trade.getOriginalFirstBuyItem();
        else if (x > left + 35 && x < left + 55)  itemStack = trade.getSecondBuyItem();
        else if (x > left + 89 && x < left + 109) itemStack = trade.getSellItem();

        if (itemStack != null && !itemStack.getName().getString().equals("Air"))
            context.drawItemTooltip(tr, itemStack, x, y);
    }

    public void markEntry() {
        if (screen.markedEntries.getMarkedEntries("favTrades") == null)
            screen.markedEntries.addCategory("favTrades");

        if (screen.markedEntries.getMarkedEntries("favTrades").contains(id)) {
            screen.markedEntries.getMarkedEntries("favTrades").remove(id);
            if (MinecraftClient.getInstance().currentScreen instanceof TradeScreen)
                HandbookClient.tradeScreen.removeFavourite(this);
            return;
        }
        screen.markedEntries.getMarkedEntries("favTrades").add(id);
        if (MinecraftClient.getInstance().currentScreen instanceof TradeScreen)
            HandbookClient.tradeScreen.addFavourite(new TradeListWidgetEntry(trade, id, 125));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) markEntry();
        if (Screen.hasShiftDown()) {
            int x = this.button.getX();
            String text = "";
                 if (mouseX > x 	 && mouseX < x + 20)  text = trade.getOriginalFirstBuyItem().getName().getString();
            else if (mouseX > x + 35 && mouseX < x + 55)  text = trade.getSecondBuyItem().getName().getString();
            else if (mouseX > x + 89 && mouseX < x + 109) text = trade.getSellItem().getName().getString();

            if (!(MinecraftClient.getInstance().currentScreen instanceof TradeScreen)) HandbookClient.openTradeScreen();
            HandbookClient.tradeScreen.setSearchText(text);
            return true;
        }
        this.button.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public List<? extends Element> children() {
        return list;
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return list;
    }

    public void setHighlighted(boolean state) {
        highlighted = state;
    }
}
