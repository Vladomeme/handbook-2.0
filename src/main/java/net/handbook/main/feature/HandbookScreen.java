package net.handbook.main.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.resources.category.BaseCategory;
import net.handbook.main.resources.category.MarkCategory;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.widget.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOfferList;

import java.util.ArrayList;
import java.util.List;

public class HandbookScreen extends Screen {

    public static final HandbookScreen INSTANCE = new HandbookScreen(Text.of(""));

    public MinecraftClient client;
    public TextRenderer tr;

    //First column
    public ListWidget categoriesWidget;
    public TexturedButtonWidget openTradesScreen;
    //Second column
    public ListWidget optionsWidget;
    public TextFieldWidget searchBox;
    //Display widget
    public DisplayWidget displayWidget;
    public TexturedButtonWidget setWaypoint;
    public TexturedButtonWidget openTrades;
    public TexturedButtonWidget shareLocation;
    public TexturedButtonWidget delete;
    public TexturedButtonWidget resetTrades;
    //Trade widget
    public TradesWidget tradesWidget;
    public TradeListWidget tradeList;
    public TexturedButtonWidget back;
    public TexturedButtonWidget shareCost;
    public TexturedButtonWidget shareTrader;
    public TexturedButtonWidget shareFull;
    //Chat selection
    public TexturedButtonWidget shareGlobal;
    public TexturedButtonWidget shareLocal;
    public TexturedButtonWidget shareWorld;
    public TexturedButtonWidget shareLFG;
    public TexturedButtonWidget shareReply;
    public TexturedButtonWidget shareCancel;
    //Other
    public TexturedButtonWidget clearWaypoint;
    public TexturedButtonWidget continueWaypoint;

    public final List<BaseCategory> categories = new ArrayList<>();
    public final MarkCategory markedEntries = MarkCategory.read();
    public BaseCategory activeCategory;
    public ListWidgetEntry selectedEntry;

    int line1x;
    int line2x;
    private int lastKey;
    private String lastFilter = "";

    private HandbookScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        client = MinecraftClient.getInstance();
        tr = client.textRenderer;

        if (categories.isEmpty()) {
            close();
            client.inGameHud.getChatHud().addMessage(Text.of("No handbook categories found! Json files must be missing."));
            return;
        }
        activeCategory = categories.get(0);

        addElements();

        super.init();
    }

    private void addElements() {
        int screenHeight = client.getWindow().getScaledHeight();
        int screenWidth = client.getWindow().getScaledWidth();

        addDrawableChild(clearWaypoint = new TexturedButtonWidget(
                20, 2 , 76, 11,
                0, 0, 11, new Identifier("handbook", "textures/clearwaypoint_button.png"),
                76, 22, button -> WaypointManager.setState(false)));
        clearWaypoint.active = WaypointManager.isActive();
        clearWaypoint.visible = WaypointManager.isActive();

        addDrawableChild(continueWaypoint = new TexturedButtonWidget(
                99, 2 , 45, 11,
                0, 0, 11, new Identifier("handbook", "textures/continue.png"),
                45, 22, button -> WaypointManager.continueOrSkip()));
        continueWaypoint.active = WaypointManager.isActive();
        continueWaypoint.visible = WaypointManager.isActive();

        int maxWidth = 0;

        for (BaseCategory category : categories) {
            int width = tr.getWidth(category.getTitle());
            if (width > maxWidth) maxWidth = width;
        }

        maxWidth = maxWidth + 20;
        line1x = maxWidth + 11;

        addDrawableChild(categoriesWidget = new ListWidget(
                maxWidth, screenHeight - 70, 30, screenHeight - 40));
        categoriesWidget.setLeftPos(20);
        categoriesWidget.setEntries(categories, "category");
        categoriesWidget.children().get(0).updateHighlight(true);
        activeCategory = (BaseCategory) categoriesWidget.children().get(0).entry;

        maxWidth = 0;

        addDrawableChild(openTradesScreen = new TexturedButtonWidget(
                line1x / 2 - 37, screenHeight - 30, 75, 11,
                0, 0, 11, new Identifier("handbook", "textures/trade_search.png"),
                75, 22, button -> client.setScreen(HandbookClient.tradeScreen)));

        for (Entry entry : ((BaseCategory) categoriesWidget.children().get(0).entry).getEntries()) {
            int width = tr.getWidth(entry.getTitle());
            if (width > maxWidth) maxWidth = width;
        }
        maxWidth = Math.min(maxWidth, 150);
        maxWidth = maxWidth + 10;

        addDrawableChild(optionsWidget = new ListWidget(
                maxWidth + 6, screenHeight - 60, 30, screenHeight - 30));
        optionsWidget.setLeftPos(25 + categoriesWidget.listWidth);
        optionsWidget.setEntries(((BaseCategory) categoriesWidget.children().get(0).entry).getEntries(), "entry");
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = width - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        addDrawableChild(searchBox = new TextFieldWidget(
                tr, line1x + 1, 16, line2x - line1x - 1, 12, Text.of("")));
        searchBox.setPlaceholder(Text.of("Search...").getWithStyle(Style.EMPTY.withItalic(true).withColor(-10197916)).get(0));

        addDrawableChild(setWaypoint = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 42, 65, 11,
                0, 0, 11, new Identifier("handbook", "textures/waypoint_button.png"),
                65, 22, button -> displayWidget.setWaypoint()));

        addDrawableChild(shareLocation = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 54, 76, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_button.png"),
                76, 22, button -> worldButtonsState(true)));

        addDrawableChild(delete = new TexturedButtonWidget(
                screenWidth - 40, screenHeight - 30, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/delete.png"),
                36, 22, button -> displayWidget.deleteEntry()));

        addDrawableChild(resetTrades = new TexturedButtonWidget(
                screenWidth - 73, screenHeight - 42, 69, 11,
                0, 0, 11, new Identifier("handbook", "textures/reset_trades.png"),
                69, 22, button -> {
            resetTrades.active = false;
            resetTrades.visible = false;
            openTrades.active = false;
            openTrades.visible = false;
            displayWidget.deleteTrade();
        }));

        addDrawableChild(shareGlobal = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 90, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_global.png"),
                36, 22, button -> {
            if (displayWidget.visible) displayWidget.shareLocation("g");
            else tradesWidget.share("g");
        }));

        addDrawableChild(shareLocal = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 78, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_local.png"),
                36, 22, button -> {
            if (displayWidget.visible) displayWidget.shareLocation("l");
            else tradesWidget.share("l");
        }));

        addDrawableChild(shareWorld = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 66, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_world.png"),
                36, 22, button -> {
            if (displayWidget.visible) displayWidget.shareLocation("wc");
            else tradesWidget.share("wc");
        }));

        addDrawableChild(shareLFG = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 54, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_lfg.png"),
                36, 22, button -> {
            if (displayWidget.visible) displayWidget.shareLocation("lfg");
            else tradesWidget.share("lfg");
        }));

        addDrawableChild(shareReply = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 42, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_reply.png"),
                36, 22, button -> {
            if (displayWidget.visible) displayWidget.shareLocation("r");
            else tradesWidget.share("r");
        }));

        addDrawableChild(shareCancel = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_cancel.png"),
                36, 22, button -> {
            if (displayWidget.visible) worldButtonsState(false);
            else tradesWidget.cancelSharing();
        }));

        addDrawableChild(openTrades = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 65, 11,
                0, 0, 11, new Identifier("handbook", "textures/trades_button.png"),
                65, 22, button -> openTrades(displayWidget.getEntry().getOffers(), displayWidget.getEntry().getTitle())));

        addDrawableChild(back = new TexturedButtonWidget(
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 26, 11,
                0, 0, 11, new Identifier("handbook", "textures/back.png"),
                26, 22, button -> openDisplay()));
        back.active = false;
        back.visible = false;

        addDrawableChild(shareCost = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 54, 39, 11,
                0, 0, 11, new Identifier("handbook", "textures/trade_cost.png"),
                39, 22, button -> tradesWidget.selectMode(TradesWidget.Mode.COST)));

        addDrawableChild(shareTrader = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 42, 39, 11,
                0, 0, 11, new Identifier("handbook", "textures/trade_trader.png"),
                39, 22, button -> tradesWidget.selectMode(TradesWidget.Mode.TRADER)));

        addDrawableChild(shareFull = new TexturedButtonWidget(
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 39, 11,
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

        addDrawableChild(tradeList = new TradeListWidget(10000, 130, screenHeight - 100, 50, screenHeight - 50));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        RenderSystem.enableBlend();
        context.fill(0, 0, width, 15, 0, 548055807);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.scale(1.5f, 1.5f, 1);
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                (int) (width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1, -1, false);
        matrices.pop();

        if (optionsWidget.children().isEmpty())
            context.drawText(tr, Text.of("Nothing found :("),
                    line1x + (line2x - line1x) / 2 - tr.getWidth("Nothing found :(") / 2, 35, -1, false);

        context.fill(line1x, 15, line1x + 1, height - 10, 100, -1);
        context.fill(line2x, 15, line2x + 1, height - 10, 100, -1);

        super.render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
    }

    public void openTrades(TradeOfferList trades, String name) {
        tradesWidget.visible = true;
        tradesWidget.setName(name);
        tradeList.setLeftPos(40 + categoriesWidget.listWidth + optionsWidget.listWidth);
        tradeList.setEntries(trades, displayWidget.getEntry().getID());

        displayWidget.visible = false;
        back.active = true;
        back.visible = true;

        displayButtonsState(false);
        worldButtonsState(false);
    }

    public void openDisplay() {
        tradesWidget.visible = false;
        tradeList.setLeftPos(10000);

        displayWidget.visible = true;
        back.active = false;
        back.visible = false;

        displayButtonsState(true);
        worldButtonsState(false);
        tradeButtonsState(false);
        moveWorldButtons(120 + categoriesWidget.listWidth + optionsWidget.listWidth,
                client.getWindow().getScaledHeight() - 30);
        displayWidget.setEntry(displayWidget.getEntry());
    }

    public void worldButtonsState(boolean state) {
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

    public void displayButtonsState(boolean state) {
        setWaypoint.active = state;
        setWaypoint.visible = state;
        shareLocation.active = state;
        shareLocation.visible = state;
        openTrades.active = state;
        openTrades.visible = state;
        delete.active = state;
        delete.visible = state;
        resetTrades.active = state;
        resetTrades.visible = state;
    }

    public void tradeButtonsState(boolean state) {
        shareCost.active = state;
        shareCost.visible = state;
        shareTrader.active = state;
        shareTrader.visible = state;
        shareFull.active = state;
        shareFull.visible = state;
    }

    public void moveWorldButtons(int x, int y) {
        shareGlobal.setPosition(x, y - 60);
        shareLocal.setPosition(x, y - 48);
        shareWorld.setPosition(x, y - 36);
        shareLFG.setPosition(x, y - 24);
        shareReply.setPosition(x, y - 12);
        shareCancel.setPosition(x, y);
    }

    public void moveTradeButtons(int x, int y) {
        shareCost.setPosition(x, y - 24);
        shareTrader.setPosition(x, y - 12);
        shareFull.setPosition(x, y);
    }

    public void setEntries(BaseCategory category) {
        int screenHeight = client.getWindow().getScaledHeight();
        int screenWidth = client.getWindow().getScaledWidth();
        int maxWidth = 0;

        for (Entry entry : category.getEntries()) {
            int width = tr.getWidth(entry.getTitle());
            if (width > maxWidth) maxWidth = width;
        }
        maxWidth = Math.min(maxWidth, 150);
        maxWidth = maxWidth + 10;

        optionsWidget.updateSize(maxWidth + 6, screenHeight - 60, 30, screenHeight - 30);
        optionsWidget.listWidth = maxWidth + 6;
        optionsWidget.setLeftPos(25 + categoriesWidget.listWidth);
        optionsWidget.setEntries(category.getEntries(), "entry");
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;
        searchBox.setWidth(line2x - line1x - 1);

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

        moveWorldButtons(120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30);
        worldButtonsState(false);

        activeCategory = category;
        searchBox.setText("");
    }

    public void filterEntries() {
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public void close() {
        if (lastKey != 69) {
            displayWidget.renderImage = false;
            client.player.closeScreen();
            super.close();
        }
    }
}
