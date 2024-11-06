package net.handbook.main.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.HandbookClient;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.entry.BaseEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ParentElement;
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

		setRenderBackground(false);
		setRenderHeader(false, 0);
	}

	public void setEntries(List<? extends BaseEntry> entries, String type) {
		clearEntries();
		setScrollAmount(0);

		int maxWidth = 0;

		List<ListWidgetEntry> favourite = new ArrayList<>();
		List<ListWidgetEntry> normal = new ArrayList<>();

		String category;
		//todo boolean for entry type
		if (type.equals("entry")) category = screen.activeCategory.getTitle();
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

	@Override
	protected int getScrollbarPositionX() {
		return getX() + getWidth() + 3;
	}

	@Override
	public int getRowWidth() {
		return listWidth;
	}

	public void updateSizeShrink(int height, int y) {
		this.height = height;
		this.setY(y);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		updateScrollingState(mouseX, mouseY, button);
		if (isMouseOver(mouseX, mouseY)) {
			ListWidgetEntry entry = this.getEntryAtPosition(mouseX, mouseY);
			if (entry != null) {
				if (entry.mouseClicked(mouseX, mouseY, button)) {
					ListWidgetEntry entry2 = this.getFocused();
					if (entry2 != entry && entry2 != null) ((ParentElement)entry2).setFocused(null);
					this.setFocused(entry);
					setDragging(true);
					return true;
				}
			}
		}
		return false;
	}

	public void clear() {
		clearEntries();
	}

	public void add(net.handbook.main.resources.entry.Entry entry, String type) {
		addEntry(new ListWidgetEntry(entry, listWidth, type));
	}
}