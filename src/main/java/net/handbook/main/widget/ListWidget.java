package net.handbook.main.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ListWidget extends ElementListWidget<ListWidgetEntry> {

	public int listWidth;

	public ListWidget(int width, int height, int top, int bottom) {
		super(MinecraftClient.getInstance(), width, height, top, bottom, 12);

		listWidth = width;

		setRenderBackground(false);
		setRenderHorizontalShadows(false);
		setRenderHeader(false, 0);
	}

	public void setEntries(List<? extends net.handbook.main.resources.entry.Entry> entries, String type) {
		clearEntries();
		setScrollAmount(0);

		int maxWidth = 0;

		for (net.handbook.main.resources.entry.Entry entry : entries) {
			addEntry(new ListWidgetEntry(entry, listWidth, type));
			int width = MinecraftClient.getInstance().textRenderer.getWidth(entry.getTitle());
			if (width > maxWidth) maxWidth = width;
		}
		maxWidth = Math.min(maxWidth, 150);

		width = maxWidth;
	}

	@Override
	protected int getScrollbarPositionX() {
		return right - 11;
	}

	@Override
	public int getRowWidth() {
		return listWidth;
	}

	public void clear() {
		clearEntries();
	}

	public void add(net.handbook.main.resources.entry.Entry entry, String type) {
		addEntry(new ListWidgetEntry(entry, listWidth, type));
	}
}