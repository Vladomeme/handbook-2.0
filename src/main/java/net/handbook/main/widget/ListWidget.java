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

		this.listWidth = width;

		this.setRenderBackground(false);
		this.setRenderHorizontalShadows(false);
		this.setRenderHeader(false, 0);
	}

	public void setEntries(List<? extends net.handbook.main.resources.Entry> entries) {
		this.clearEntries();
		this.setScrollAmount(0);

		int maxWidth = 0;

		for (net.handbook.main.resources.Entry entry : entries) {
			addEntry(new ListWidgetEntry(entry, listWidth));
			int width = MinecraftClient.getInstance().textRenderer.getWidth(entry.getTitle());
			if (width > maxWidth) maxWidth = width;
		}

		this.width = maxWidth;
	}

	@Override
	protected int getScrollbarPositionX() {
		return this.right - 11;
	}

	@Override
	public int getRowWidth() {
		return this.listWidth;
	}

	public void clear() {
		clearEntries();
	}

	public void add(net.handbook.main.resources.Entry entry) {
		addEntry(new ListWidgetEntry(entry, listWidth));
	}
}