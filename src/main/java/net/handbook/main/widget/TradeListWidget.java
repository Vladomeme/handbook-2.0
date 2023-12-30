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

	public TradeListWidget(int width, int height, int top, int bottom) {
		super(MinecraftClient.getInstance(), width, height, top, bottom, 24);

		this.listWidth = width;

		this.setRenderBackground(false);
		this.setRenderHorizontalShadows(false);
		this.setRenderHeader(false, 0);
	}

	public void setEntries(TradeOfferList trades) {
		this.clearEntries();

		for (TradeOffer trade : trades) {
			this.addEntry(new TradeListWidgetEntry(trade, listWidth));
		}
	}

	@Override
	protected int getScrollbarPositionX() {
		return this.right - 11;
	}
	@Override
	public int getRowWidth() {
		return this.listWidth;
	}

	public int getBottom() {
		return bottom;
	}
}