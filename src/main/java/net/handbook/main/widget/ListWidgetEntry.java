package net.handbook.main.widget;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.EditScreen;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.category.Category;
import net.handbook.main.resources.category.MarkCategory;
import net.handbook.main.resources.entry.BaseEntry;
import net.handbook.main.resources.entry.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ListWidgetEntry extends ElementListWidget.Entry<ListWidgetEntry> {

    private final TextRenderer tr = MinecraftClient.getInstance().textRenderer;
    private final HandbookScreen screen = HandbookClient.handbookScreen;

    private final BaseEntry.Type type;
    public final BaseEntry entry;
    private boolean highlighted = false;

    public final ButtonWidget button;
    public final List<ClickableWidget> list;

    public ListWidgetEntry(BaseEntry entry, int width, BaseEntry.Type type) {
        this.entry = entry;
        this.type = type;

        button = ButtonWidget.builder(Text.of(""), button -> {
            updateHighlight(true);
            entry.mouseClicked();
        }).position(0, 0).build();
        button.setDimensions(width, 12);
        list = ImmutableList.of(button);
    }

    @Override
    public void render(DrawContext context, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        button.setPosition(left, top);

        String category;
        if (type.equals(BaseEntry.Type.Entry)) category = screen.activeCategory.getTitle();
        else category = "Categories";
        RenderSystem.enableBlend();
        if (highlighted) {
            if (screen.markedEntries.getMarkedEntries(category).contains(entry.getTitle()))
                context.fill(left, top - 2, left + entryWidth, top + entryHeight + 2, HandbookConfig.INSTANCE.highlightFavColor);
            else context.fill(left, top - 2, left + entryWidth, top + entryHeight + 2, HandbookConfig.INSTANCE.highlightColor);
        }
        else {
            if (screen.markedEntries.getMarkedEntries(category).contains(entry.getTitle()))
                context.fill(left, top - 2, left + entryWidth, top + entryHeight + 2, HandbookConfig.INSTANCE.favouriteColor);
        }
        RenderSystem.disableBlend();

        if (tr.getWidth(entry.getTitle()) > 150)
            context.drawText(tr, tr.trimToWidth(entry.getTitle(), 147) + "...", left + 10, top,
                    HandbookConfig.INSTANCE.textColor, false);
        else context.drawText(tr, entry.getTitle(), left + 10, top, HandbookConfig.INSTANCE.textColor, false);
    }

    public void markEntry() {
        String name = type.equals(BaseEntry.Type.Entry) ? screen.activeCategory.getTitle() : "Categories";
        MarkCategory category = screen.markedEntries;
        List<String> entries = category.getMarkedEntries(name);

        if (entries == null) category.addCategory(name);
        else {
            if (entries.contains(entry.getTitle())) entries.remove(entry.getTitle());
            else entries.add(entry.getTitle());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Screen.hasShiftDown() && HandbookConfig.INSTANCE.editorMode) {
            MinecraftClient.getInstance().setScreen(new EditScreen(entry, type.equals(BaseEntry.Type.Category),
                    (entry instanceof Category<? extends Entry> c) ? c.getType() : screen.activeCategory.getType()));
            return true;
        }
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
            case Category -> {
                if (screen.selectedEntry != null)
                    screen.selectedEntry.setHighlighted(false);
                screen.selectedEntry = this;
            }
            case Entry -> {
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
