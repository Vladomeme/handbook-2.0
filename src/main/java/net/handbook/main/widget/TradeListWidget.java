package net.handbook.main.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

@Environment(EnvType.CLIENT)
public class TradeListWidget extends ElementListWidget<TradeListWidgetEntry> {

	public final int listWidth;

	public TradeListWidget(int left, int width, int height, int top, int bottom) {
		super(MinecraftClient.getInstance(), width, height, top, bottom, 24);

		listWidth = width;
		setLeftPos(left);

		setRenderBackground(false);
		setRenderHorizontalShadows(false);
		setRenderHeader(false, 0);
	}

	public void setEntries(TradeOfferList trades, String id) {
		clearEntries();

		for (int i = 0; i < trades.size(); i++)
			addEntry(new TradeListWidgetEntry(trades.get(i), id + "&" + i, listWidth));
		this.setScrollAmount(0);
	}

	public void addEntries(TradeOfferList trades, String id) {
		for (int i = 0; i < trades.size(); i++)
			addEntry(new TradeListWidgetEntry(trades.get(i), id + "&" + i, listWidth));
		this.setScrollAmount(0);
	}

	public void addEntry(TradeOffer trade, String id) {
		addEntry(new TradeListWidgetEntry(trade, id, listWidth));
		this.setScrollAmount(0);
	}

	public void clear() {
		clearEntries();
	}

	@Override
	protected int getScrollbarPositionX() {
		return right - 11;
	}

	@Override
	public int getRowWidth() {
		return listWidth;
	}

	public int getBottom() {
		return bottom;
	}
}