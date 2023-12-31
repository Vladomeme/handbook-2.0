package net.handbook.main.widget;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.util.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ListWidgetEntry extends ElementListWidget.Entry<ListWidgetEntry> {

    private final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

    private final String type;
    public final Entry entry;
    private boolean highlighted = false;

    public final TexturedButtonWidget button;
    public final List<ClickableWidget> list;

    public ListWidgetEntry(Entry entry, int width, String type) {
        this.entry = entry;
        this.type = type;

        this.button = new TexturedButtonWidget(0, 0, width, 12, 0, 0, 0,
                new Identifier("handbook", "empty"), button -> {
            updateHighlight(true);
            entry.mouseClicked();
        });
        this.list = ImmutableList.of(this.button);
    }

    @Override
    public void render(DrawContext context, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        this.button.setPosition(left, top);

        if (highlighted) {
            RenderSystem.enableBlend();
            context.fill(left, top - 2, left + entryWidth, top + entryHeight + 2, 866822826);
            RenderSystem.disableBlend();
        }

        if (tr.getWidth(entry.getTitle()) > 150) {
            context.drawText(tr, tr.trimToWidth(entry.getTitle(), 147) + "...", left + 10, top, 16777215, false);
        }
        else context.drawText(tr, entry.getTitle(), left + 10, top, 16777215, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.button.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public List<? extends Element> children() {
        return this.list;
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return this.list;
    }

    public void updateHighlight(boolean state) {
        switch (type) {
            case "category" -> {
                if (HandbookScreen.selectedEntry != null)
                    HandbookScreen.selectedEntry.setHighlighted(false);
                HandbookScreen.selectedEntry = this;

            }
            case "entry" -> {
                if (HandbookScreen.displayWidget.selectedEntry != null)
                    HandbookScreen.displayWidget.selectedEntry.setHighlighted(false);
                HandbookScreen.displayWidget.selectedEntry = this;
            }
        }
        setHighlighted(state);
    }

    public void setHighlighted(boolean state) {
        this.highlighted = state;
    }
}
