package net.handbook.main.widget;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.feature.HandbookScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
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
    private static final Identifier TEXTURE = new Identifier("textures/gui/container/villager2.png");

    public final TradeOffer trade;
    private boolean highlighted = false;

    public final TexturedButtonWidget button;
    public final List<ClickableWidget> list;

    public TradeListWidgetEntry(TradeOffer trade, int width) {
        this.trade = trade;

        button = new TexturedButtonWidget(0, 0, width, 20, 0, 0, 0,
                new Identifier("handbook", "empty"), button -> HandbookScreen.tradesWidget.startSharing(this));
        list = ImmutableList.of(button);
    }

    @Override
    public void render(DrawContext context, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        button.setPosition(left, top);

        if (highlighted) {
            RenderSystem.enableBlend();
            context.fill(left, top, left + 110, top + 20, 866822826);
            RenderSystem.disableBlend();
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
    }

    private void renderTooltip(DrawContext context, int x, int y, int left) {
        if (y > HandbookScreen.tradeList.getBottom()) return;
        ItemStack itemStack = null;

             if (x > left 	   && x < left + 20)  itemStack = trade.getOriginalFirstBuyItem();
        else if (x > left + 35 && x < left + 55)  itemStack = trade.getSecondBuyItem();
        else if (x > left + 89 && x < left + 109) itemStack = trade.getSellItem();

        if (itemStack != null && !itemStack.getName().getString().equals("Air"))
            context.drawItemTooltip(tr, itemStack, x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
