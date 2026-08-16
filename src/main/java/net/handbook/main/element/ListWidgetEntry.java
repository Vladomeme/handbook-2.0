package net.handbook.main.element;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.DataManager;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.EditScreen;
import net.handbook.main.resources.ScreenWithCategoryList;
import net.handbook.main.resources.EntryType;
import net.handbook.main.resources.ListType;
import net.handbook.main.resources.entry.Category;
import net.handbook.main.resources.entry.BaseEntry;
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
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class ListWidgetEntry extends ElementListWidget.Entry<ListWidgetEntry> {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;

    private final ListType listType;
    public final BaseEntry entry;
    private boolean highlighted = false;

    public final ButtonWidget button;
    public final List<ClickableWidget> list;

    public ListWidgetEntry(ListWidget parent, BaseEntry entry, int width, ListType listType) {
        this.entry = entry;
        this.listType = listType;

        button = ButtonWidget.builder(Text.of(""), button -> {
            parent.updateHighlight(this, true);
            entry.mouseClicked();
        }).position(0, 0).build();
        button.setDimensions(width, 12);
        list = ImmutableList.of(button);
    }

    @Override
    public void render(DrawContext context, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        button.setPosition(left, top);

        List<String> markedEntries;
        if (client.currentScreen instanceof ScreenWithCategoryList screen && listType.equals(ListType.Entry))
            markedEntries = DataManager.getMarkedEntries(screen.activeCategory().title());
        else markedEntries = DataManager.getMarkedEntries("Categories");

        RenderSystem.enableBlend();
        if (highlighted) {
            if (markedEntries.contains(entry.title()))
                context.fill(left, top - 2, left + entryWidth, top + entryHeight + 2, HandbookConfig.INSTANCE.highlightFavColor);
            else context.fill(left, top - 2, left + entryWidth, top + entryHeight + 2, HandbookConfig.INSTANCE.highlightColor);
        }
        else {
            if (markedEntries.contains(entry.title()))
                context.fill(left, top - 2, left + entryWidth, top + entryHeight + 2, HandbookConfig.INSTANCE.favouriteColor);
        }
        RenderSystem.disableBlend();

        if (tr.getWidth(entry.displayTitle()) > 150)
            context.drawText(tr, tr.trimToWidth(entry.displayTitle(), 145) + "...", left + 10, top,
                    HandbookConfig.INSTANCE.textColor, false);
        else context.drawText(tr, entry.displayTitle(), left + 10, top, HandbookConfig.INSTANCE.textColor, false);
    }

    public void markEntry() {
        List<String> markedEntries;
        if (client.currentScreen instanceof ScreenWithCategoryList screen && listType.equals(ListType.Entry))
            markedEntries = DataManager.getMarkedEntries(screen.activeCategory().title());
        else markedEntries = DataManager.getMarkedEntries("Categories");

        if (markedEntries.contains(entry.title())) markedEntries.remove(entry.title());
        else markedEntries.add(entry.title());
        DataManager.updateMarked();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        EntryType type = EntryType.normal;
        if (listType.equals(ListType.Category)) type = ((Category<?>) entry).type();
        else if (client.currentScreen instanceof ScreenWithCategoryList screen) type = screen.activeCategory().type();

        if (Screen.hasShiftDown() && HandbookConfig.INSTANCE.editorMode) {
            EditScreen.open(entry, type, listType.equals(ListType.Category) ? null
                    : ((ScreenWithCategoryList) Objects.requireNonNull(client.currentScreen)).activeCategory());
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

    public void setHighlighted(boolean state) {
        highlighted = state;
    }

    public boolean isHighlighted() {
        return highlighted;
    }
}
