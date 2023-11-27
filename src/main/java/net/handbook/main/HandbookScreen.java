package net.handbook.main;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.resources.Category;
import net.handbook.main.resources.Entry;
import net.handbook.main.widget.DisplayWidget;
import net.handbook.main.widget.ListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HandbookScreen extends Screen {

    public static final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

    public static ListWidget categoriesWidget;
    public static TextFieldWidget searchBox;
    public static ListWidget optionsWidget;
    public static DisplayWidget displayWidget;

    public static final List<Category> categories = new ArrayList<>();

    static int line1x;
    static int line2x;
    private int lastKey;

    protected HandbookScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        if (categories.size() == 0) {
            close();
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("No handbook categories found! Json files must be missing."));
            return;
        }

        int maxWidth = 0;

        for (Category category : categories) {
            int width = MinecraftClient.getInstance().textRenderer.getWidth(category.getTitle());
            if (width > maxWidth) maxWidth = width;
        }

        maxWidth = maxWidth + 20;
        line1x = maxWidth + 11;

        this.addDrawableChild(categoriesWidget = new ListWidget(
                maxWidth, screenHeight - 40, 31, screenHeight - 10));
        categoriesWidget.setLeftPos(20);
        categoriesWidget.setEntries(categories);

        maxWidth = 0;

        for (Entry entry : categories.get(0).getEntries()) {
            int width = MinecraftClient.getInstance().textRenderer.getWidth(entry.getTitle());
            if (width > maxWidth) maxWidth = width;
        }
        maxWidth = maxWidth + 10;

        this.addDrawableChild(searchBox = new TextFieldWidget(
                tr, 25 + categoriesWidget.listWidth, 16, maxWidth, 12, Text.of("")));

        this.addDrawableChild(optionsWidget = new ListWidget(
                maxWidth + 6, screenHeight - 70, 31, screenHeight - 10));
        optionsWidget.setLeftPos(25 + categoriesWidget.listWidth);
        optionsWidget.setEntries(categories.get(0).getEntries());
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = width - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        this.addDrawableChild(displayWidget = new DisplayWidget(
                30 + categoriesWidget.listWidth + optionsWidget.listWidth, 20, maxWidth, screenHeight - 40, Text.of("")));

        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        RenderSystem.enableBlend();
        context.fill(0, 0, this.width, 15, 0, 548055807);
        context.getMatrices().push();
        context.getMatrices().scale(1.5f, 1.5f, 1);
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                (int) (this.width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1, -1, false);
        context.getMatrices().pop();
        RenderSystem.disableBlend();

        context.fill(line1x, 15, line1x + 1, this.height - 10, 100, -1);
        context.fill(line2x, 15, line2x + 1, this.height - 10, 100, -1);

        super.render(context, mouseX, mouseY, delta);
    }

    public static void setEntries(List<? extends Entry> entries) {
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int maxWidth = 0;

        for (Entry entry : entries) {
            int width = MinecraftClient.getInstance().textRenderer.getWidth(entry.getTitle());
            if (width > maxWidth) maxWidth = width;
        }

        maxWidth = maxWidth + 10;

        searchBox.setWidth(maxWidth);
        optionsWidget.updateSize(maxWidth + 6, screenHeight - 70, 31, screenHeight - 10);
        optionsWidget.listWidth = maxWidth + 6;
        optionsWidget.setLeftPos(25 + categoriesWidget.listWidth);
        optionsWidget.setEntries(entries);
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = screenWidth - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        displayWidget.setWidth(maxWidth);
        displayWidget.setX(30 + categoriesWidget.listWidth + optionsWidget.listWidth);
        displayWidget.setEntry(null);
        HandbookClient.LOGGER.info("Setting list width: " + maxWidth);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        lastKey = keyCode;

        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void close() {
        if (lastKey != 69) {
            client.player.closeScreen();
            super.close();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}
