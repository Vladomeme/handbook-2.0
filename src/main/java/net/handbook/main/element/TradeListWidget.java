package net.handbook.main.element;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.resources.HandbookTradeOffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.util.math.MathHelper;

import java.util.List;

@Environment(EnvType.CLIENT)
public class TradeListWidget extends ElementListWidget<TradeListWidgetEntry> {

	private boolean scrolling;
	public final int listWidth;

	public TradeListWidget(int left, int width, int height, int y) {
		super(MinecraftClient.getInstance(), width, height, y, 24);

		listWidth = width;
		setX(left);

		setRenderHeader(false, 0);
	}

	public void setEntries(List<HandbookTradeOffer> trades, String id) {
		clearEntries();

		for (int i = 0; i < trades.size(); i++)
			addEntry(new TradeListWidgetEntry(trades.get(i), id + "&" + i, listWidth));
		this.setScrollAmount(0);
	}

	public void addEntries(List<HandbookTradeOffer> trades, String id) {
		for (int i = 0; i < trades.size(); i++)
			addEntry(new TradeListWidgetEntry(trades.get(i), id + "&" + i, listWidth));
		this.setScrollAmount(0);
	}

	public void addEntry(HandbookTradeOffer trade, String id) {
		addEntry(new TradeListWidgetEntry(trade, id, listWidth));
		this.setScrollAmount(0);
	}

	public void clear() {
		clearEntries();
	}

	@Override
	protected int getScrollbarX() {
		return getX() + getWidth() - 10;
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

	@Override
	protected void updateScrollingState(double mouseX, double mouseY, int button) {
		scrolling = button == 0 && mouseX >= (double) getScrollbarX() && mouseX < (double) (getScrollbarX() + 6);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		updateScrollingState(mouseX, mouseY, button);
		if (scrolling) return true;
		if (isMouseOver(mouseX, mouseY)) {
			TradeListWidgetEntry entry = getEntryAtPosition(mouseX, mouseY);
			if (entry != null) {
				if (entry.mouseClicked(mouseX, mouseY, button)) {
					TradeListWidgetEntry entry2 = getFocused();
					if (entry2 != entry && entry2 != null) entry2.setFocused(null);
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
			else if (mouseY > (double) getBottom()) setScrollAmount(getMaxScroll());
			else {
				double d = Math.max(1, getMaxScroll());
				int i = height;
				int j = MathHelper.clamp((int) ((float) (i * i) / (float) getMaxPosition()), 32, i - 8);
				double e = Math.max(1.0, d / (double) (i - j));
				setScrollAmount(getScrollAmount() + deltaY * e / 2);
			}
			return true;
		}
		else return false;
	}
}