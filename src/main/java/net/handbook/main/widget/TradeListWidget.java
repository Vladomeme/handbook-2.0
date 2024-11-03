package net.handbook.main.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.HandbookClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

@Environment(EnvType.CLIENT)
public class TradeListWidget extends ElementListWidget<TradeListWidgetEntry> {

	private boolean scrolling;
	public final int listWidth;

	public TradeListWidget(int left, int width, int height, int y) {
		super(MinecraftClient.getInstance(), width, height, y, 24);

		listWidth = width;
		setX(left);

		setRenderBackground(false);
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
		return getX() + getWidth() - 10;
	}

	@Override
	public int getRowWidth() {
		return listWidth;
	}

	@Override
	protected void updateScrollingState(double mouseX, double mouseY, int button) {
		scrolling = button == 0 && mouseX >= (double) getScrollbarPositionX() && mouseX < (double) (getScrollbarPositionX() + 6);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		updateScrollingState(mouseX, mouseY, button);
		if (isMouseOver(mouseX, mouseY)) {
			TradeListWidgetEntry entry = getEntryAtPosition(mouseX, mouseY);
			if (entry != null) {
				if (entry.mouseClicked(mouseX, mouseY, button)) {
					TradeListWidgetEntry entry2 = getFocused();
					if (entry2 != entry && entry2 != null) ((ParentElement) entry2).setFocused(null);
					setFocused(entry);
					setDragging(true);
					return true;
				}
				return scrolling;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
		else if (button == 0 && scrolling) {
			if (mouseY < (double) getY()) setScrollAmount(0.0);
			else if (mouseY > (double)this.getBottom()) setScrollAmount(getMaxScroll());
			else {
				double d = Math.max(1, this.getMaxScroll());
				int i = this.height;
				int j = MathHelper.clamp((int) ((float) (i * i) / (float) getMaxPosition()), 32, i - 8);
				double e = Math.max(1.0, d / (double) (i - j));
				setScrollAmount(getScrollAmount() + deltaY * e);
			}
			return true;
		}
		else return false;
	}
}