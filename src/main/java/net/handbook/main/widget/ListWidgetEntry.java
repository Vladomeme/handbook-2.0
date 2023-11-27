package net.handbook.main.widget;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.handbook.main.resources.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.util.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ListWidgetEntry extends ElementListWidget.Entry<ListWidgetEntry> {

    public final Entry entry;

    public final TexturedButtonWidget button;
    public final List<ClickableWidget> list;

    public ListWidgetEntry(Entry entry, int width) {
        this.entry = entry;

        this.button = new TexturedButtonWidget(0, 0, width, 12, 0, 0, 0,
                new Identifier("handbook", "empty"), button -> entry.mouseClicked());
        this.list = ImmutableList.of(this.button);
    }

    @Override
    public void render(DrawContext context, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        this.button.setPosition(left, top);
        context.drawText(MinecraftClient.getInstance().textRenderer, entry.getTitle(), left + 10, top, 16777215, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.button.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public List<? extends Element> children() {
        return this.list;
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return this.list;
    }


}
