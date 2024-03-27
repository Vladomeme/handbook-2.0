package net.handbook.main.widget;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.HandbookClient;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.entry.Entry;
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
    private final HandbookScreen screen = HandbookClient.handbookScreen;

    private final String type;
    public final Entry entry;
    private boolean highlighted = false;

    public final TexturedButtonWidget button;
    public final List<ClickableWidget> list;

    public ListWidgetEntry(Entry entry, int width, String type) {
        this.entry = entry;
        this.type = type;

        button = new TexturedButtonWidget(0, 0, width, 12, 0, 0, 0,
                new Identifier("handbook", "empty"), button -> {
            updateHighlight(true);
            entry.mouseClicked();
        });
        list = ImmutableList.of(button);
    }

    @Override
    public void render(DrawContext context, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        button.setPosition(left, top);

        String category;
        if (type.equals("entry")) category = screen.activeCategory.getTitle();
        else category = "Categories";
        RenderSystem.enableBlend();
        if (highlighted) {
            if (screen.markedEntries.getMarkedEntries(category).contains(entry.getTitle()))
                context.fill(left, top - 2, left + entryWidth, top + entryHeight + 2, 1358935040);
            else context.fill(left, top - 2, left + entryWidth, top + entryHeight + 2, 866822826);
        }
        else {
            if (screen.markedEntries.getMarkedEntries(category).contains(entry.getTitle()))
                context.fill(left, top - 2, left + entryWidth, top + entryHeight + 2, 2030023680);
        }
        RenderSystem.disableBlend();

        if (tr.getWidth(entry.getTitle()) > 150)
            context.drawText(tr, tr.trimToWidth(entry.getTitle(), 147) + "...", left + 10, top, 16777215, false);
        else context.drawText(tr, entry.getTitle(), left + 10, top, 16777215, false);
    }

    public void markEntry() {
        String category;
        if (type.equals("entry")) category = screen.activeCategory.getTitle();
        else category = "Categories";
        if (screen.markedEntries.getMarkedEntries(category) == null)
            screen.markedEntries.addCategory(category);
        else {
            if (screen.markedEntries.getMarkedEntries(category).contains(entry.getTitle()))
                screen.markedEntries.getMarkedEntries(category).remove(entry.getTitle());
            else screen.markedEntries.getMarkedEntries(category).add(entry.getTitle());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) markEntry();
        else this.button.mouseClicked(mouseX, mouseY, button);
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

    public void updateHighlight(boolean state) {
        switch (type) {
            case "category" -> {
                if (screen.selectedEntry != null)
                    screen.selectedEntry.setHighlighted(false);
                screen.selectedEntry = this;

            }
            case "entry" -> {
                if (screen.displayWidget.selectedEntry != null)
                    screen.displayWidget.selectedEntry.setHighlighted(false);
                screen.displayWidget.selectedEntry = this;
            }
        }
        setHighlighted(state);
    }

    public void setHighlighted(boolean state) {
        highlighted = state;
    }
}
