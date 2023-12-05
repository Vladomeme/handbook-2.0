package net.handbook.main.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.resources.BaseCategory;
import net.handbook.main.resources.Entry;
import net.handbook.main.widget.DisplayWidget;
import net.handbook.main.widget.ListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class HandbookScreen extends Screen {

    public static final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

    public static ListWidget categoriesWidget;
    public static TextFieldWidget searchBox;
    public static ListWidget optionsWidget;
    public static DisplayWidget displayWidget;
    public static TexturedButtonWidget clearWaypoint;
    public static TexturedButtonWidget setWaypoint;
    public static TexturedButtonWidget shareLocation;
    public static TexturedButtonWidget openTrades;

    public static final List<BaseCategory> categories = new ArrayList<>();
    public static BaseCategory activeCategory;

    static int line1x;
    static int line2x;
    private static int lastKey;
    private static String lastFilter = "";

    public HandbookScreen(Text title) {
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

        this.addDrawableChild(clearWaypoint = new TexturedButtonWidget(
                20, 2 , 76, 11,
                0, 0, 11, new Identifier("handbook", "textures/clearwaypoint_button.png"),
                76, 22, button -> Waypoint.setVisibility(false)));
        clearWaypoint.active = Waypoint.isActive();
        clearWaypoint.visible = Waypoint.isActive();

        int maxWidth = 0;

        for (BaseCategory category : categories) {
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
        activeCategory = categories.get(0);
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = width - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        this.addDrawableChild(setWaypoint = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 44, 65, 11,
                0, 0, 11, new Identifier("handbook", "textures/waypoint_button.png"),
                65, 22, button -> displayWidget.setWaypoint()));
        setWaypoint.active = false;
        setWaypoint.visible = false;

        this.addDrawableChild(shareLocation = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 32, 76, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_button.png"),
                76, 22, button -> displayWidget.shareLocation()));
        shareLocation.active = false;
        shareLocation.visible = false;

        this.addDrawableChild(openTrades = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 20, 65, 11,
                0, 0, 11, new Identifier("handbook", "textures/trades_button.png"),
                65, 22, button -> displayWidget.openTrades()));
        openTrades.active = false;
        openTrades.visible = false;

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

        if (searchBox.getText().equals("") && !searchBox.isFocused()) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 1000);
            context.drawText(tr, Text.of("Search...").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                    searchBox.getX() + 3, searchBox.getY() + 3, -10197916, false);
            context.getMatrices().pop();
        }

        context.fill(line1x, 15, line1x + 1, this.height - 10, 100, -1);
        context.fill(line2x, 15, line2x + 1, this.height - 10, 100, -1);

        RenderSystem.enableBlend();
        super.render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
    }

    public static void setEntries(BaseCategory category) {
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int maxWidth = 0;

        for (Entry entry : category.getEntries()) {
            int width = MinecraftClient.getInstance().textRenderer.getWidth(entry.getTitle());
            if (width > maxWidth) maxWidth = width;
        }

        maxWidth = maxWidth + 10;

        searchBox.setWidth(maxWidth);
        optionsWidget.updateSize(maxWidth + 6, screenHeight - 70, 31, screenHeight - 10);
        optionsWidget.listWidth = maxWidth + 6;
        optionsWidget.setLeftPos(25 + categoriesWidget.listWidth);
        optionsWidget.setEntries(category.getEntries());
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = screenWidth - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        displayWidget.setWidth(maxWidth);
        displayWidget.setX(30 + categoriesWidget.listWidth + optionsWidget.listWidth);
        displayWidget.setEntry(null);

        setWaypoint.setX(40 + categoriesWidget.listWidth + optionsWidget.listWidth);
        setWaypoint.active = false;
        setWaypoint.visible = false;

        shareLocation.setX(40 + categoriesWidget.listWidth + optionsWidget.listWidth);
        shareLocation.active = false;
        shareLocation.visible = false;

        openTrades.setX(40 + categoriesWidget.listWidth + optionsWidget.listWidth);
        openTrades.active = false;
        openTrades.visible = false;

        activeCategory = category;
        searchBox.setText("");
    }

    public static void filterEntries() {
        if (!searchBox.getText().equals(lastFilter)) {
            if (searchBox.getText().equals("")) {
                optionsWidget.setEntries(activeCategory.getEntries());
                lastFilter = "";
                return;
            }

            optionsWidget.clear();
            for (Entry entry : activeCategory.getEntries()) {
                if (entry.getTitle().toLowerCase().contains(searchBox.getText().toLowerCase())) optionsWidget.add(entry);
            }
        }
        lastFilter = searchBox.getText();
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
            displayWidget.renderImage = false;
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
