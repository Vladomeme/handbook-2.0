package net.handbook.main.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.resources.entry.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FilterWidget extends ClickableWidget {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;

    private CheckboxWidget traderCheckbox;
    private TextFieldWidget shardField;
    private TextFieldWidget textField;

    private final Identifier checkTexture = new Identifier("handbook", "textures/gui/sprites/check.png");

    public FilterWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.of(""));
        init();
    }

    private void init() {
        traderCheckbox = CheckboxWidget.builder(Text.of(""), tr).pos(0, 0).checked(false).build();
        traderCheckbox.setDimensions(18, 18);
        shardField = new TextFieldWidget(tr, 0, 0, 100, 14, Text.of(""));
        textField = new TextFieldWidget(tr, 0, 0, 100, 14, Text.of(""));
        textField.setChangedListener(s -> onUpdate());
        shardField.setChangedListener(s -> onUpdate());
    }

    public void open() {
        int y = getY() + 4;
        int x = getX() + 3;

        textField.visible = true;
        textField.active = true;
        textField.setPosition(x, y);
        y += 16;

        boolean bl = HandbookClient.handbookScreen.activeCategory.getEntries().get(0).getShard() != null;
        if (bl) y += 1;
        shardField.visible = bl;
        shardField.active = bl;
        shardField.setPosition(x, y);
        if (bl) y += 16;

        bl = HandbookClient.handbookScreen.activeCategory.getType().equals("trader");
        if (bl) y += 1;
        traderCheckbox.visible = bl;
        traderCheckbox.active = bl;
        traderCheckbox.setPosition(x + 83, y);
        if (bl) y += 19;

        width = Math.max(107 + tr.getWidth("Trader "),
                HandbookClient.handbookScreen.line2x - HandbookClient.handbookScreen.line1x - 4);
        height = y - getY() + 1;
    }

    public void unfocus() {
        shardField.setFocused(false);
        textField.setFocused(false);
        traderCheckbox.setFocused(false);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(getX(), getY(), 0);
        RenderSystem.enableBlend();
        context.fill(0, 0, width, height, HandbookConfig.INSTANCE.tradeBackgroundColor);
        context.fill(0, 0, 1, height, HandbookConfig.INSTANCE.bordersColor);
        context.fill(width, 0, width + 1, height, HandbookConfig.INSTANCE.bordersColor);
        context.fill(0, 0, width + 1, 1, HandbookConfig.INSTANCE.bordersColor);
        context.fill(0, height, width + 1, height + 1, HandbookConfig.INSTANCE.bordersColor);
        RenderSystem.disableBlend();
        matrices.pop();

        if (shardField.visible) {
            shardField.render(context, mouseX, mouseY, delta);
            context.drawText(tr, "Shard ", shardField.getX() + 104, shardField.getY() + 2,
                    HandbookConfig.INSTANCE.textColor, false);
        }
        if (textField.visible) {
            textField.render(context, mouseX, mouseY, delta);
            context.drawText(tr, "Text ", textField.getX() + 104, textField.getY() + 2,
                    HandbookConfig.INSTANCE.textColor, false);
        }

        if (traderCheckbox.visible) {
            int x = traderCheckbox.getX();
            int y = traderCheckbox.getY();
            RenderSystem.enableBlend();
            context.fill(x, y, x + 17, y + 17, HandbookConfig.INSTANCE.highlightColor);
            int color = traderCheckbox.isMouseOver(mouseX, mouseY) ? HandbookConfig.INSTANCE.buttonInactiveColor : HandbookConfig.INSTANCE.buttonActiveColor;
            context.fill(x, y, x + 1, y + 17, color);
            context.fill(x + 17, y, x + 18, y + 17, color);
            context.fill(x, y, x + 18, y + 1, color);
            context.fill(x, y + 17, x + 18, y + 18, color);
            if (traderCheckbox.isChecked()) {
                context.drawGuiTexture(checkTexture, x, y - 1, 18, 18);
            }
            RenderSystem.disableBlend();
            context.drawText(tr, "Trader ", traderCheckbox.getX() + 22, traderCheckbox.getY() + 3,
                    HandbookConfig.INSTANCE.textColor, false);
        }
    }

    private void onUpdate() {
        HandbookClient.handbookScreen.filterEntries(false);
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean checkEntry(Entry entry) {
        //text check
        if (!textField.getText().isEmpty() && (entry.getText() == null || !entry.getText().contains(textField.getText()))) return false;
        //shard check
        if (!shardField.getText().isEmpty() && (entry.getShard() == null || !entry.getShard().contains(shardField.getText()))) return false;
        //trader check
        if (traderCheckbox.isChecked() && !entry.hasOffers()) return false;

        //all active filters passed
        return true;
    }

    public void reset() {
        if (traderCheckbox.isChecked()) traderCheckbox.onPress();
        textField.setText("");
        shardField.setText("");
    }

    public boolean filtersActive() {
        return traderCheckbox.isChecked() || !shardField.getText().isEmpty() || !textField.getText().isEmpty();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (traderCheckbox.mouseClicked(mouseX, mouseY, button)) onUpdate();
        textField.setFocused(textField.mouseClicked(mouseX, mouseY, button));
        shardField.setFocused(shardField.mouseClicked(mouseX, mouseY, button));

        if (active && visible && isValidClickButton(button) && clicked(mouseX, mouseY)) {
            onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        shardField.keyPressed(keyCode, scanCode, modifiers);
        textField.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        shardField.charTyped(chr, modifiers);
        textField.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
