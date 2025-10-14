package net.handbook.main.widget;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.TradeScreen;
import net.handbook.main.resources.HandbookTradeOffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
public class TradeListWidgetEntry extends ElementListWidget.Entry<TradeListWidgetEntry> {

    private final TextRenderer tr = MinecraftClient.getInstance().textRenderer;
    private final HandbookScreen screen = HandbookClient.handbookScreen;
    private static final Identifier TEXTURE = Identifier.of("container/villager/trade_arrow");

    public final HandbookTradeOffer trade;
    public final String id;
    private boolean highlighted = false;

    public final ButtonWidget button;
    public final List<ClickableWidget> list;

    public TradeListWidgetEntry(HandbookTradeOffer trade, String id, int width) {
        this.trade = trade;
        this.id = id;

        button = ButtonWidget.builder(Text.of(""), button -> {
            if (MinecraftClient.getInstance().currentScreen instanceof HandbookScreen)
                screen.tradesWidget.startSharing(this);
            else {
                HandbookClient.tradeScreen.setTraderInfo(id.split("&")[0]);
                HandbookClient.tradeScreen.selectedEntry = this;
                setHighlighted(true);
            }
        }).dimensions(0, 0, width, 20).build();
        list = ImmutableList.of(button);
    }

    @Override
    public void render(DrawContext context, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        button.setPosition(left, top);

        RenderSystem.enableBlend();
        if (highlighted) {
            if (screen.markedEntries.getMarkedEntries("favTrades").contains(id))
                context.fill(left, top + 1, left + 110, top + 19, HandbookConfig.INSTANCE.highlightFavColor);
            else context.fill(left, top + 1, left + 110, top + 19, HandbookConfig.INSTANCE.highlightColor);
            context.fill(left, top + 1, left + 1, top + 19, HandbookConfig.INSTANCE.bordersColor);
            context.fill(left + 109, top + 1, left + 110, top + 19, HandbookConfig.INSTANCE.bordersColor);
            context.fill(left, top + 1, left + 110, top + 2, HandbookConfig.INSTANCE.bordersColor);
            context.fill(left, top + 18, left + 110, top + 19, HandbookConfig.INSTANCE.bordersColor);
        }
        else {
            if (screen.markedEntries.getMarkedEntries("favTrades").contains(id))
                context.fill(left, top + 1, left + 110, top + 19, HandbookConfig.INSTANCE.favouriteColor);
            else context.fill(left, top + 1, left + 110, top + 19, HandbookConfig.INSTANCE.tradeBackgroundColor);
        }

        context.drawItem(trade.buyItem1(), left + 2, top + 2);
        context.drawItemInSlot(tr, trade.buyItem1(), left + 2, top + 2);

        trade.buyItem2().ifPresent(stack -> {
            context.drawItem(stack, left + 37, top + 2);
            context.drawItemInSlot(tr, stack, left + 37, top + 2);
        });

        context.drawGuiTexture(TEXTURE, left + 67, top + 5, 10, 9);

        context.drawItem(trade.sellItem(), left + 91, top + 2);
        context.drawItemInSlot(tr, trade.sellItem(), left + 91, top + 2);

        RenderSystem.disableScissor();
        if (isMouseOver(mouseX, mouseY)) renderTooltip(context, mouseX, mouseY, left);
        RenderSystem.disableBlend();
    }

    private void renderTooltip(DrawContext context, int x, int y, int left) {
        if (y < 30 || y > MinecraftClient.getInstance().getWindow().getScaledHeight() - 40) return;
        ItemStack itemStack = null;

             if (x > left 	   && x < left + 20)  itemStack = trade.buyItem1();
        else if (x > left + 35 && x < left + 55)  itemStack = trade.buyItem2().orElse(null);
        else if (x > left + 89 && x < left + 109) itemStack = trade.sellItem();

        if (itemStack != null && !itemStack.isEmpty() && !itemStack.getName().getString().equals("Air"))
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
            if (!(MinecraftClient.getInstance().currentScreen instanceof TradeScreen)) HandbookClient.openTradeScreen();
            HandbookClient.tradeScreen.setSearchText(getSelectedItemName(mouseX));
            return true;
        }
        this.button.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String getSelectedItemName(double mouseX) {
        int x = button.getX();

        if (mouseX > x && mouseX < x + 20) return trade.buyItem1().getName().getString();
        else if (mouseX > x + 35 && mouseX < x + 55) return trade.buyItem2().isPresent() ? trade.buyItem2().get().getName().getString() : "";
        else if (mouseX > x + 89 && mouseX < x + 109) return trade.sellItem().getName().getString();

        return "";
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
