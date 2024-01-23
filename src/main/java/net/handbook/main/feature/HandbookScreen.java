package net.handbook.main.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.resources.category.BaseCategory;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.widget.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOfferList;

import java.util.ArrayList;
import java.util.List;

public class HandbookScreen extends Screen {

    public static final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

    //First column
    public static ListWidget categoriesWidget;
    //Second column
    public static ListWidget optionsWidget;
    public static TextFieldWidget searchBox;
    //Display widget
    public static DisplayWidget displayWidget;
    public static TexturedButtonWidget setWaypoint;
    public static TexturedButtonWidget openTrades;
    public static TexturedButtonWidget shareLocation;
    //Trade widget
    public static TradesWidget tradesWidget;
    public static TradeListWidget tradeList;
    public static TexturedButtonWidget back;
    public static TexturedButtonWidget shareCost;
    public static TexturedButtonWidget shareTrader;
    public static TexturedButtonWidget shareFull;
    //Chat selection
    public static TexturedButtonWidget shareGlobal;
    public static TexturedButtonWidget shareLocal;
    public static TexturedButtonWidget shareWorld;
    public static TexturedButtonWidget shareLFG;
    public static TexturedButtonWidget shareReply;
    public static TexturedButtonWidget shareCancel;
    //Other
    public static TexturedButtonWidget clearWaypoint;

    public static final List<BaseCategory> categories = new ArrayList<>();
    public static BaseCategory activeCategory;
    public static ListWidgetEntry selectedEntry;

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

        if (categories.isEmpty()) {
            close();
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("No handbook categories found! Json files must be missing."));
            return;
        }

        addDrawableChild(clearWaypoint = new TexturedButtonWidget(
                20, 2 , 76, 11,
                0, 0, 11, new Identifier("handbook", "textures/clearwaypoint_button.png"),
                76, 22, button -> WaypointManager.setState(false)));
        clearWaypoint.active = WaypointManager.isActive();
        clearWaypoint.visible = WaypointManager.isActive();

        int maxWidth = 0;

        for (BaseCategory category : categories) {
            int width = MinecraftClient.getInstance().textRenderer.getWidth(category.getTitle());
            if (width > maxWidth) maxWidth = width;
        }

        maxWidth = maxWidth + 20;
        line1x = maxWidth + 11;

        addDrawableChild(categoriesWidget = new ListWidget(
                maxWidth, screenHeight - 40, 31, screenHeight - 10));
        categoriesWidget.setLeftPos(20);
        categoriesWidget.setEntries(categories, "category");
        categoriesWidget.children().get(0).updateHighlight(true);

        maxWidth = 0;

        for (Entry entry : categories.get(0).getEntries()) {
            int width = MinecraftClient.getInstance().textRenderer.getWidth(entry.getTitle());
            if (width > maxWidth) maxWidth = width;
        }
        maxWidth = Math.min(maxWidth, 150);
        maxWidth = maxWidth + 10;

        addDrawableChild(searchBox = new TextFieldWidget(
                tr, 25 + categoriesWidget.listWidth, 16, maxWidth, 12, Text.of("")));

        addDrawableChild(optionsWidget = new ListWidget(
                maxWidth + 6, screenHeight - 70, 31, screenHeight - 10));
        optionsWidget.setLeftPos(25 + categoriesWidget.listWidth);
        optionsWidget.setEntries(categories.get(0).getEntries(), "entry");
        activeCategory = categories.get(0);
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = width - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        addDrawableChild(setWaypoint = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 44, 65, 11,
                0, 0, 11, new Identifier("handbook", "textures/waypoint_button.png"),
                65, 22, button -> displayWidget.setWaypoint()));

        addDrawableChild(shareLocation = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 32, 76, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_button.png"),
                76, 22, button -> worldButtonsState(true)));

        addDrawableChild(shareGlobal = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 80, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_global.png"),
                36, 22, button -> {
                    if (displayWidget.visible) displayWidget.shareLocation("g");
                    else tradesWidget.share("g");
                }));

        addDrawableChild(shareLocal = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 68, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_local.png"),
                36, 22, button -> {
                    if (displayWidget.visible) displayWidget.shareLocation("l");
                    else tradesWidget.share("l");
                }));

        addDrawableChild(shareWorld = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 56, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_world.png"),
                36, 22, button -> {
                    if (displayWidget.visible) displayWidget.shareLocation("wc");
                    else tradesWidget.share("wc");
                }));

        addDrawableChild(shareLFG = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 44, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_lfg.png"),
                36, 22, button -> {
                    if (displayWidget.visible) displayWidget.shareLocation("lfg");
                    else tradesWidget.share("lfg");
                }));

        addDrawableChild(shareReply = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 32, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_reply.png"),
                36, 22, button -> {
                    if (displayWidget.visible) displayWidget.shareLocation("r");
                    else tradesWidget.share("r");
                }));

        addDrawableChild(shareCancel = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 20, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_cancel.png"),
                36, 22, button -> {
                    if (displayWidget.visible) worldButtonsState(false);
                    else tradesWidget.cancelSharing();
                }));

        addDrawableChild(openTrades = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 20, 65, 11,
                0, 0, 11, new Identifier("handbook", "textures/trades_button.png"),
                65, 22, button -> openTrades(displayWidget.getEntry().getOffers(), displayWidget.getEntry().getTitle())));

        addDrawableChild(back = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 20, 26, 11,
                0, 0, 11, new Identifier("handbook", "textures/back.png"),
                26, 22, button -> openDisplay()));
        back.active = false;
        back.visible = false;

        addDrawableChild(shareCost = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 44, 39, 11,
                0, 0, 11, new Identifier("handbook", "textures/trade_cost.png"),
                39, 22, button -> tradesWidget.selectMode(TradesWidget.Mode.COST)));

        addDrawableChild(shareTrader = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 32, 39, 11,
                0, 0, 11, new Identifier("handbook", "textures/trade_trader.png"),
                39, 22, button -> tradesWidget.selectMode(TradesWidget.Mode.TRADER)));

        addDrawableChild(shareFull = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 20, 39, 11,
                0, 0, 11, new Identifier("handbook", "textures/trade_full.png"),
                39, 22, button -> tradesWidget.selectMode(TradesWidget.Mode.FULL)));

        displayButtonsState(false);
        worldButtonsState(false);
        tradeButtonsState(false);

        addDrawableChild(displayWidget = new DisplayWidget(
                30 + categoriesWidget.listWidth + optionsWidget.listWidth, 20, maxWidth, screenHeight - 40, Text.of("")));

        addDrawableChild(tradesWidget = new TradesWidget(
                30 + categoriesWidget.listWidth + optionsWidget.listWidth, 20, maxWidth, screenHeight - 40));
        tradesWidget.visible = false;
        tradesWidget.active = false;

        addDrawableChild(tradeList = new TradeListWidget(130, screenHeight - 100, 50, screenHeight - 50));
        tradeList.setLeftPos(10000);

        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        RenderSystem.enableBlend();
        context.fill(0, 0, width, 15, 0, 548055807);
        context.getMatrices().push();
        context.getMatrices().scale(1.5f, 1.5f, 1);
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                (int) (width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1, -1, false);
        context.getMatrices().pop();
        RenderSystem.disableBlend();

        if (searchBox.getText().isEmpty() && !searchBox.isFocused()) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 1000);
            context.drawText(tr, Text.of("Search...").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                    searchBox.getX() + 3, searchBox.getY() + 3, -10197916, false);
            context.getMatrices().pop();
        }

        context.fill(line1x, 15, line1x + 1, height - 10, 100, -1);
        context.fill(line2x, 15, line2x + 1, height - 10, 100, -1);

        RenderSystem.enableBlend();
        super.render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
    }

    public static void openTrades(TradeOfferList trades, String name) {
        tradesWidget.visible = true;
        tradesWidget.setName(name);
        tradeList.setEntries(trades);
        tradeList.setLeftPos(40 + categoriesWidget.listWidth + optionsWidget.listWidth);

        displayWidget.visible = false;
        back.active = true;
        back.visible = true;

        displayButtonsState(false);
        worldButtonsState(false);
    }

    public static void openDisplay() {
        tradesWidget.visible = false;
        tradeList.setLeftPos(10000);

        displayWidget.visible = true;
        back.active = false;
        back.visible = false;

        displayButtonsState(true);
        worldButtonsState(false);
        tradeButtonsState(false);
        moveWorldButtons(120 + categoriesWidget.listWidth + optionsWidget.listWidth,
                MinecraftClient.getInstance().getWindow().getScaledHeight() - 20);
        displayWidget.setEntry(displayWidget.getEntry());
    }

    public static void worldButtonsState(boolean state) {
        shareGlobal.active = state;
        shareGlobal.visible = state;
        shareLocal.active = state;
        shareLocal.visible = state;
        shareWorld.active = state;
        shareWorld.visible = state;
        shareLFG.active = state;
        shareLFG.visible = state;
        shareReply.active = state;
        shareReply.visible = state;
        shareCancel.active = state;
        shareCancel.visible = state;
    }

    public static void displayButtonsState(boolean state) {
        setWaypoint.active = state;
        setWaypoint.visible = state;
        shareLocation.active = state;
        shareLocation.visible = state;
        openTrades.active = state;
        openTrades.visible = state;
    }

    public static void tradeButtonsState(boolean state) {
        shareCost.active = state;
        shareCost.visible = state;
        shareTrader.active = state;
        shareTrader.visible = state;
        shareFull.active = state;
        shareFull.visible = state;
    }

    public static void moveWorldButtons(int x, int y) {
        shareGlobal.setPosition(x, y - 60);
        shareLocal.setPosition(x, y - 48);
        shareWorld.setPosition(x, y - 36);
        shareLFG.setPosition(x, y - 24);
        shareReply.setPosition(x, y - 12);
        shareCancel.setPosition(x, y);
    }

    public static void moveTradeButtons(int x, int y) {
        shareCost.setPosition(x, y - 24);
        shareTrader.setPosition(x, y - 12);
        shareFull.setPosition(x, y);
    }

    public static void setEntries(BaseCategory category) {
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int maxWidth = 0;

        for (Entry entry : category.getEntries()) {
            int width = MinecraftClient.getInstance().textRenderer.getWidth(entry.getTitle());
            if (width > maxWidth) maxWidth = width;
        }
        maxWidth = Math.min(maxWidth, 150);
        maxWidth = maxWidth + 10;

        searchBox.setWidth(maxWidth);
        optionsWidget.updateSize(maxWidth + 6, screenHeight - 70, 31, screenHeight - 10);
        optionsWidget.listWidth = maxWidth + 6;
        optionsWidget.setLeftPos(25 + categoriesWidget.listWidth);
        optionsWidget.setEntries(category.getEntries(), "entry");
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = screenWidth - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        displayWidget.setWidth(maxWidth);
        displayWidget.setX(30 + categoriesWidget.listWidth + optionsWidget.listWidth);
        displayWidget.setEntry(null);
        tradesWidget.setWidth(maxWidth);
        tradesWidget.setX(30 + categoriesWidget.listWidth + optionsWidget.listWidth);

        displayButtonsState(false);
        setWaypoint.setX(40 + categoriesWidget.listWidth + optionsWidget.listWidth);
        shareLocation.setX(40 + categoriesWidget.listWidth + optionsWidget.listWidth);
        openTrades.setX(40 + categoriesWidget.listWidth + optionsWidget.listWidth);
        back.setX(40 + categoriesWidget.listWidth + optionsWidget.listWidth);

        moveWorldButtons(120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 20);
        worldButtonsState(false);

        activeCategory = category;
        searchBox.setText("");
    }

    public static void filterEntries() {
        if (!searchBox.getText().equals(lastFilter)) {
            if (searchBox.getText().isEmpty()) {
                optionsWidget.setEntries(activeCategory.getEntries(), "entry");
                lastFilter = "";
                return;
            }

            optionsWidget.clear();
            for (Entry entry : activeCategory.getEntries()) {
                if (entry.getTitle().toLowerCase().contains(searchBox.getText().toLowerCase())) optionsWidget.add(entry, "entry");
            }
            optionsWidget.setScrollAmount(0);
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
            close();
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
