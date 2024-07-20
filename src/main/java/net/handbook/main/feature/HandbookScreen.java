package net.handbook.main.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.EditScreen;
import net.handbook.main.resources.category.Category;
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

public class HandbookScreen extends Screen {

    public static final HandbookScreen INSTANCE = new HandbookScreen(Text.of(""));

    public MinecraftClient client;
    public TextRenderer tr;

    //First column
    public ListWidget categoriesWidget;
    @SuppressWarnings("unused")
    public HandbookButtonWidget openTradesScreen;
    public HandbookButtonWidget addCategory;
    //Second column
    public ListWidget optionsWidget;
    public TextFieldWidget searchBox;
    public TexturedButtonWidget filterButton;
    public FilterWidget filterWidget;
    public HandbookButtonWidget addEntry;
    //Display widget
    public DisplayWidget displayWidget;
    public HandbookButtonWidget setWaypoint;
    public HandbookButtonWidget openTrades;
    public HandbookButtonWidget shareLocation;
    public HandbookButtonWidget delete;
    public HandbookButtonWidget resetTrades;
    //Trade widget
    public TradesWidget tradesWidget;
    public TradeListWidget tradeList;
    public HandbookButtonWidget back;
    public HandbookButtonWidget shareCost;
    public HandbookButtonWidget shareTrader;
    public HandbookButtonWidget shareFull;
    //Chat selection
    public HandbookButtonWidget shareGlobal;
    public HandbookButtonWidget shareLocal;
    public HandbookButtonWidget shareWorld;
    public HandbookButtonWidget shareLFG;
    public HandbookButtonWidget shareReply;
    public HandbookButtonWidget shareCancel;
    //Other
    public HandbookButtonWidget clearWaypoint;
    public HandbookButtonWidget continueWaypoint;

    public MarkCategory markedEntries;
    public Category activeCategory;
    public ListWidgetEntry selectedEntry;

    public int line1x;
    public int line2x;
    private int lastKey;
    private String lastFilter = "";

    private HandbookScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        client = MinecraftClient.getInstance();
        tr = client.textRenderer;

        if (HandbookClient.getCategories().isEmpty()) {
            close();
            client.inGameHud.getChatHud().addMessage(Text.of("No handbook categories found! Json files must be missing."));
            return;
        }
        activeCategory = HandbookClient.getCategories().get(0);
        addElements();
        super.init();
    }

    private void addElements() {
        int screenHeight = client.getWindow().getScaledHeight();
        int screenWidth = client.getWindow().getScaledWidth();

        addDrawableChild(clearWaypoint = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                20, 2, 76, 11,
                "Clear waypoint", button -> WaypointManager.setState(false)));
        clearWaypoint.active = WaypointManager.isActive();
        clearWaypoint.visible = WaypointManager.isActive();

        addDrawableChild(continueWaypoint = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                99, 2, 45, 11,
                "Continue", button -> WaypointManager.continueOrSkip()));
        continueWaypoint.active = WaypointManager.isActive();
        continueWaypoint.visible = WaypointManager.isActive();

        int maxWidth = 0;

        for (Category category : HandbookClient.getCategories()) {
            int width = tr.getWidth(category.getTitle());
            if (width > maxWidth) maxWidth = width;
        }

        maxWidth = maxWidth + 20;
        line1x = maxWidth + 11;

        addDrawableChild(categoriesWidget = new ListWidget(
                maxWidth, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 90 : 70), 30,
                screenHeight - (HandbookConfig.INSTANCE.editorMode ? 60 : 40)));
        categoriesWidget.setLeftPos(20);
        categoriesWidget.setEntries(HandbookClient.getCategories(), "category");
        categoriesWidget.children().get(0).updateHighlight(true);
        activeCategory = (Category) categoriesWidget.children().get(0).entry;

        maxWidth = 0;

        addDrawableChild(addCategory = new HandbookButtonWidget(HandbookButtonWidget.Type.Positive,
                line1x / 2 - tr.getWidth("Add"), screenHeight - 45, 30, 11,
                "Add", button -> client.setScreen(new EditScreen(null, true, ""))));
        addCategory.visible = HandbookConfig.INSTANCE.editorMode;
        addCategory.active = HandbookConfig.INSTANCE.editorMode;

        addDrawableChild(openTradesScreen = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                line1x / 2 - 37, screenHeight - 30, 75, 11,
                "Trade Search", button -> client.setScreen(HandbookClient.tradeScreen)));

        for (Entry entry : ((Category) categoriesWidget.children().get(0).entry).getEntries()) {
            int width = tr.getWidth(entry.getTitle());
            if (width > maxWidth) maxWidth = width;
        }
        maxWidth = Math.min(maxWidth, 150);
        maxWidth = maxWidth + 10;

        addDrawableChild(optionsWidget = new ListWidget(
                maxWidth + 6, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 80 : 60), 30,
                screenHeight - (HandbookConfig.INSTANCE.editorMode ? 50 : 30)));
        optionsWidget.setLeftPos(25 + categoriesWidget.listWidth);
        optionsWidget.setEntries(((Category) categoriesWidget.children().get(0).entry).getEntries(), "entry");
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = width - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        addDrawableChild(addEntry = new HandbookButtonWidget(HandbookButtonWidget.Type.Positive,
                (line2x - line1x) / 2 - tr.getWidth("Add"), screenHeight - 45, 30, 11,
                "Add", button -> client.setScreen(new EditScreen(null, false, activeCategory.getType()))));
        addEntry.visible = HandbookConfig.INSTANCE.editorMode;
        addEntry.active = HandbookConfig.INSTANCE.editorMode;

        addDrawableChild(searchBox = new TextFieldWidget(
                tr, line1x + 16, 16, line2x - line1x - 16, 12, Text.of("")));
        searchBox.setPlaceholder(Text.of("Search...").getWithStyle(Style.EMPTY.withItalic(true).withColor(-10197916)).get(0));

        addDrawableChild(filterButton = new TexturedButtonWidget(line1x + 2, 16, 12, 12,
                0, 0, 12, new Identifier("handbook", "textures/filter_button.png"),
                12, 24, button -> toggleFilterWidget()));

        addDrawableChild(filterWidget = new FilterWidget(line1x + 2, 30, 0, 0));
        filterWidget.active = false;
        filterWidget.visible = false;

        addDrawableChild(setWaypoint = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 42, 76, 11,
                "Set waypoint", button -> displayWidget.setWaypoint()));

        addDrawableChild(shareLocation = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 54, 76, 11,
                "Share location", button -> worldButtonsState(true)));

        addDrawableChild(delete = new HandbookButtonWidget(HandbookButtonWidget.Type.Negative,
                screenWidth - 73, screenHeight - 30, 69, 11,
                "Delete", button -> displayWidget.deleteEntry()));

        addDrawableChild(resetTrades = new HandbookButtonWidget(HandbookButtonWidget.Type.Negative,
                screenWidth - 73, screenHeight - 42, 69, 11,
                "Reset trades", button -> {
            resetTrades.active = false;
            resetTrades.visible = false;
            openTrades.active = false;
            openTrades.visible = false;
            displayWidget.deleteTrade();
        }));

        addDrawableChild(shareGlobal = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 90, 36, 11,
                "Global", button -> {
            if (displayWidget.visible) displayWidget.shareLocation("g");
            else tradesWidget.share("g");
        }));

        addDrawableChild(shareLocal = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 78, 36, 11,
                "Local", button -> {
            if (displayWidget.visible) displayWidget.shareLocation("l");
            else tradesWidget.share("l");
        }));

        addDrawableChild(shareWorld = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 66, 36, 11,
                "World", button -> {
            if (displayWidget.visible) displayWidget.shareLocation("wc");
            else tradesWidget.share("wc");
        }));

        addDrawableChild(shareLFG = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 54, 36, 11,
                "LFG", button -> {
            if (displayWidget.visible) displayWidget.shareLocation("lfg");
            else tradesWidget.share("lfg");
        }));

        addDrawableChild(shareReply = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 42, 36, 11,
                "Reply", button -> {
            if (displayWidget.visible) displayWidget.shareLocation("r");
            else tradesWidget.share("r");
        }));

        addDrawableChild(shareCancel = new HandbookButtonWidget(HandbookButtonWidget.Type.Negative,
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 36, 11,
                "Cancel", button -> {
            if (displayWidget.visible) worldButtonsState(false);
            else tradesWidget.cancelSharing();
        }));

        addDrawableChild(openTrades = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 76, 11,
                "Open trades", button -> openTrades(displayWidget.getEntry().getOffers(), displayWidget.getEntry().getTitle())));

        addDrawableChild(back = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 26, 11,
                "Back", button -> openDisplay()));
        back.active = false;
        back.visible = false;

        addDrawableChild(shareCost = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 54, 39, 11,
                "Cost", button -> tradesWidget.selectMode(TradesWidget.Mode.COST)));

        addDrawableChild(shareTrader = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 42, 39, 11,
                "Trader", button -> tradesWidget.selectMode(TradesWidget.Mode.TRADER)));

        addDrawableChild(shareFull = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 39, 11,
                "Full", button -> tradesWidget.selectMode(TradesWidget.Mode.FULL)));

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
        context.fill(0, 0, width, 15, 0, HandbookConfig.INSTANCE.screenHeadColor);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.scale(1.5f, 1.5f, 1);
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                (int) (width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1,
                HandbookConfig.INSTANCE.textColor, false);
        matrices.pop();

        if (optionsWidget.children().isEmpty())
            context.drawText(tr, Text.of("Nothing found :("),
                    line1x + (line2x - line1x) / 2 - tr.getWidth("Nothing found :(") / 2,
                    35 + (filterWidget.active ? filterWidget.getHeight() : 0), HandbookConfig.INSTANCE.textColor, false);

        context.fill(line1x, 15, line1x + 1, height - 10, 100, HandbookConfig.INSTANCE.bordersColor);
        context.fill(line2x, 15, line2x + 1, height - 10, 100, HandbookConfig.INSTANCE.bordersColor);

        super.render(context, mouseX, mouseY, delta);
        if (filterWidget.filtersActive()) {
            int x = filterButton.getX();
            int y = filterButton.getY();
            context.fill(x, y, x + 1, y + 11, HandbookConfig.INSTANCE.favouriteColor);
            context.fill(x + 11, y, x + 12, y + 11, HandbookConfig.INSTANCE.favouriteColor);
            context.fill(x, y, x + 12, y + 1, HandbookConfig.INSTANCE.favouriteColor);
            context.fill(x, y + 11, x + 12, y + 12, HandbookConfig.INSTANCE.favouriteColor);
        }
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

    private void toggleFilterWidget() {
        int screenHeight = client.getWindow().getScaledHeight();

        filterWidget.visible = !filterWidget.visible;
        filterWidget.active = !filterWidget.active;
        if (filterWidget.active) {
            filterWidget.open();
            optionsWidget.updateSizeShrink(screenHeight - (HandbookConfig.INSTANCE.editorMode ? 82 : 62)
                    - filterWidget.getHeight(), 32 + filterWidget.getHeight());
        }
        else {
            filterWidget.unfocus();
            optionsWidget.updateSizeShrink(screenHeight - 60, 30);
        }
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

    public void setEntries(Category category) {
        int screenHeight = client.getWindow().getScaledHeight();
        int screenWidth = client.getWindow().getScaledWidth();
        int maxWidth = 0;

        for (Entry entry : category.getEntries()) {
            int width = tr.getWidth(entry.getTitle());
            if (width > maxWidth) maxWidth = width;
        }
        maxWidth = Math.min(maxWidth, 150);
        maxWidth = maxWidth + 10;

        optionsWidget.updateSize(maxWidth + 6, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 80 : 60),
                30, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 50 : 30));
        optionsWidget.listWidth = maxWidth + 6;
        optionsWidget.setLeftPos(25 + categoriesWidget.listWidth);
        optionsWidget.setEntries(category.getEntries(), "entry");
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;
        searchBox.setWidth(line2x - line1x - 16);

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
        if (filterWidget.active) toggleFilterWidget();
        filterWidget.reset();
    }

    public void filterEntries(boolean scheduled) {
        if (!searchBox.getText().equals(lastFilter) || !scheduled) {
            if (searchBox.getText().isEmpty() && !filterWidget.filtersActive()) {
                optionsWidget.setEntries(activeCategory.getEntries(), "entry");
                lastFilter = "";
                return;
            }

            optionsWidget.clear();
            for (Entry entry : activeCategory.getEntries()) {
                if (!entry.getTitle().toLowerCase().contains(searchBox.getText().toLowerCase())) continue;
                if (filterWidget.filtersActive() && !filterWidget.checkEntry(entry)) continue;
                optionsWidget.add(entry, "entry");
            }
            optionsWidget.setScrollAmount(0);
        }
        lastFilter = searchBox.getText();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        lastKey = keyCode;

        if (!super.keyPressed(keyCode, scanCode, modifiers)
                && client.options.inventoryKey.matchesKey(keyCode, scanCode)) close();
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
