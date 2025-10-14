package net.handbook.main.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.HandbookClient;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.entry.BaseEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ListWidget extends ElementListWidget<ListWidgetEntry> {

	private final HandbookScreen screen = HandbookClient.handbookScreen;

	public int listWidth;

	public ListWidget(int width, int height, int y) {
		super(MinecraftClient.getInstance(), width, height, y, 12);

		listWidth = width;

		setRenderHeader(false, 0);
	}

	public void setEntries(List<? extends BaseEntry> entries, BaseEntry.Type type) {
		clearEntries();
		setScrollAmount(0);

		int maxWidth = 0;

		List<ListWidgetEntry> favourite = new ArrayList<>();
		List<ListWidgetEntry> normal = new ArrayList<>();

		String category;

		if (type.equals(BaseEntry.Type.Entry)) category = screen.activeCategory.getTitle();
		else category = "Categories";

		for (BaseEntry entry : entries) {
			if (entry.getTitle().equals("EXCLUDE")) continue;

			if (screen.markedEntries.getMarkedEntries(category) == null) {
				screen.markedEntries.addCategory(category);
				normal.add(new ListWidgetEntry(entry, listWidth, type));
			}
			else {
				if (screen.markedEntries.getMarkedEntries(category).contains(entry.getTitle()))
					favourite.add(new ListWidgetEntry(entry, listWidth, type));
				else normal.add(new ListWidgetEntry(entry, listWidth, type));
			}

			int width = MinecraftClient.getInstance().textRenderer.getWidth(entry.getTitle());
			if (width > maxWidth) maxWidth = width;
		}
		for (ListWidgetEntry entry : favourite) addEntry(entry);
		for (ListWidgetEntry entry : normal) addEntry(entry);
		maxWidth = Math.min(maxWidth, 150);

		width = maxWidth;
	}

	public void setEntriesNoWidth(List<? extends BaseEntry> entries, BaseEntry.Type type) {
		clearEntries();
		setScrollAmount(0);

		List<ListWidgetEntry> favourite = new ArrayList<>();
		List<ListWidgetEntry> normal = new ArrayList<>();

		String category;

		category = type.equals(BaseEntry.Type.Entry) ? screen.activeCategory.getTitle() : "Categories";

		for (BaseEntry entry : entries) {
			if (entry.getTitle().equals("EXCLUDE")) continue;

			if (screen.markedEntries.getMarkedEntries(category) == null) {
				screen.markedEntries.addCategory(category);
				normal.add(new ListWidgetEntry(entry, listWidth, type));
			}
			else {
				if (screen.markedEntries.getMarkedEntries(category).contains(entry.getTitle()))
					favourite.add(new ListWidgetEntry(entry, listWidth, type));
				else normal.add(new ListWidgetEntry(entry, listWidth, type));
			}
		}
		for (ListWidgetEntry entry : favourite) addEntry(entry);
		for (ListWidgetEntry entry : normal) addEntry(entry);
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
    public void add(net.handbook.main.resources.entry.Entry entry, BaseEntry.Type type) {
		addEntry(new ListWidgetEntry(entry, listWidth, type));
	}
}