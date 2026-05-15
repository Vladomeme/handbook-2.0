package net.handbook.main.element;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.DataManager;
import net.handbook.main.resources.ScreenWithCategoryList;
import net.handbook.main.resources.ListType;
import net.handbook.main.resources.entry.Category;
import net.handbook.main.resources.entry.BaseEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ListWidget extends ElementListWidget<ListWidgetEntry> {

	public int listWidth;
	private ListWidgetEntry selectedEntry;

	public ListWidget(int width, int height, int y) {
		super(MinecraftClient.getInstance(), width, height, y, 12);

		listWidth = width;

		setRenderHeader(false, 0);
	}

	public void setEntries(List<? extends BaseEntry> entries, ListType listType, boolean updateWidth) {
		clearEntries();
		setScrollAmount(0);

		int maxWidth = 0;

		List<ListWidgetEntry> favourite = new ArrayList<>();
		List<ListWidgetEntry> normal = new ArrayList<>();

		List<String> markedEntries;
		if (client.currentScreen instanceof ScreenWithCategoryList screen && listType.equals(ListType.Entry))
			markedEntries = DataManager.getMarkedEntries(screen.activeCategory().title());
		else markedEntries = DataManager.getMarkedEntries("Categories");

		for (BaseEntry entry : entries) {
			if (listType.equals(ListType.Category) && ((Category<?>) entry).hidden()) continue;

			if (markedEntries.contains(entry.title()))
				favourite.add(new ListWidgetEntry(this, entry, listWidth, listType));
			else normal.add(new ListWidgetEntry(this, entry, listWidth, listType));

			if (updateWidth) {
				int width = client.textRenderer.getWidth(entry.title());
				if (width > maxWidth) maxWidth = width;
			}
		}
		for (ListWidgetEntry entry : favourite) addEntry(entry);
		for (ListWidgetEntry entry : normal) addEntry(entry);

		if (updateWidth) width = Math.min(maxWidth, 150);
	}

	public void updateHighlight(ListWidgetEntry entry, boolean state) {
		if (selectedEntry != null) selectedEntry.setHighlighted(false);
		selectedEntry = entry;
		entry.setHighlighted(state);
	}

	@Override
	protected int getScrollbarX() {
		return getX() + getWidth() + 3;
	}

	@Override
	public int getRowWidth() {
		return listWidth;
	}

	@Override
	protected void drawHeaderAndFooterSeparators(DrawContext context) {

	}

	@Override
	protected void drawMenuListBackground(DrawContext context) {

	}

	public void updateSizeShrink(int height, int y) {
		this.height = height;
		this.setY(y);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		updateScrollingState(mouseX, mouseY, button);
		if (isMouseOver(mouseX, mouseY)) {
			ListWidgetEntry entry = getEntryAtPosition(mouseX, mouseY);
			if (entry != null) {
				if (entry.mouseClicked(mouseX, mouseY, button)) {
					ListWidgetEntry entry2 = getFocused();
					if (entry2 != entry && entry2 != null) entry2.setFocused(null);
					setFocused(entry);
					setDragging(true);
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
    public void clear() {
		clearEntries();
	}

	@SuppressWarnings("unused")
    public void add(net.handbook.main.resources.entry.Entry entry, ListType listType) {
		addEntry(new ListWidgetEntry(this, entry, listWidth, listType));
	}
}