package net.handbook.main.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.EditScreen;
import net.handbook.main.resources.category.Category;
import net.handbook.main.resources.category.MarkCategory;
import net.handbook.main.resources.entry.BaseEntry;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.widget.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.village.TradeOfferList;

import java.util.Comparator;

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
    public Category<? extends Entry> activeCategory;
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

        for (Category<? extends Entry> category : HandbookClient.getCategories()) {
            int width = tr.getWidth(category.getTitle());
            if (width > maxWidth) maxWidth = width;
        }

        maxWidth += 20;
        line1x = maxWidth + 20;

        addDrawableChild(categoriesWidget = new ListWidget(
                maxWidth + 3, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 90 : 70), 30));
        categoriesWidget.setX(20);
        categoriesWidget.setEntries(HandbookClient.getCategories(), BaseEntry.Type.Category);
        categoriesWidget.children().get(0).updateHighlight(true);
        activeCategory = (Category<? extends Entry>) categoriesWidget.children().get(0).entry;

        maxWidth = 0;

        addDrawableChild(addCategory = new HandbookButtonWidget(HandbookButtonWidget.Type.Positive,
                line1x / 2 - 20, screenHeight - 45, 40, 11,
                "Add", button -> EditScreen.open(null, true, "")));
        addCategory.visible = HandbookConfig.INSTANCE.editorMode;
        addCategory.active = HandbookConfig.INSTANCE.editorMode;

        addDrawableChild(openTradesScreen = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                line1x / 2 - 37, screenHeight - 30, 75, 11,
                "Trade Search", button -> client.setScreen(HandbookClient.tradeScreen)));

        if (((Category<? extends Entry>) categoriesWidget.children().get(0).entry).getEntries().isEmpty()) maxWidth = 110;
        else {
            for (Entry entry : ((Category<? extends Entry>) categoriesWidget.children().get(0).entry).getEntries()) {
                int width = tr.getWidth(entry.getTitle());
                if (width > maxWidth) maxWidth = width;
            }
            maxWidth = Math.min(maxWidth, 150);
            maxWidth = maxWidth + 10;
        }

        addDrawableChild(optionsWidget = new ListWidget(
                maxWidth + 12, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 80 : 60), 30));
        optionsWidget.setX(30 + categoriesWidget.listWidth);
        optionsWidget.setEntries(((Category<? extends Entry>) categoriesWidget.children().get(0).entry).getEntries(), BaseEntry.Type.Entry);
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = width - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        addDrawableChild(addEntry = new HandbookButtonWidget(HandbookButtonWidget.Type.Positive,
                        line1x + (line2x - line1x) / 2 - 20, screenHeight - 45, 40, 11,
                "Add", button -> EditScreen.open(null, false, activeCategory.getType())));
        addEntry.visible = HandbookConfig.INSTANCE.editorMode;
        addEntry.active = HandbookConfig.INSTANCE.editorMode;

        addDrawableChild(searchBox = new TextFieldWidget(
                tr, line1x + 16, 15, line2x - line1x - 16, 14, Text.of("")));
        searchBox.setPlaceholder(Text.of("Search...").getWithStyle(Style.EMPTY.withItalic(true).withColor(-10197916)).get(0));

        addDrawableChild(filterButton = new TexturedButtonWidget(line1x + 2, 16, 12, 12,
                new ButtonTextures(new Identifier("handbook", "textures/gui/sprites/filter_unfocused.png"),
                new Identifier("handbook", "textures/gui/sprites/filter_focused.png")),
                button -> toggleFilterWidget()));

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

        addDrawableChild(tradeList = new TradeListWidget(10000, 130, screenHeight - 100, 50));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        RenderSystem.enableBlend();
        context.fill(0, 0, width, 15, 1, HandbookConfig.INSTANCE.screenHeadColor);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.scale(1.5f, 1.5f, 1);
        matrices.translate(0, 0, 1);
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                (int) (width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1,
                HandbookConfig.INSTANCE.textColor, false);
        matrices.pop();

        matrices.push();
        matrices.translate(0, 0, 1);
        if (optionsWidget.children().isEmpty())
            context.drawText(tr, Text.of("Nothing found :("),
                    line1x + (line2x - line1x) / 2 - tr.getWidth("Nothing found :(") / 2,
                    35 + (filterWidget.active ? filterWidget.getHeight() : 0), HandbookConfig.INSTANCE.textColor, false);
        if (shareCost.visible) {
            context.drawText(tr, Text.of("Share..."),
                    shareCost.getX() + (shareCost.getWidth() / 2) - tr.getWidth("Share...") / 2,
                    shareCost.getY() - 14, HandbookConfig.INSTANCE.textColor, false);
        }
        if (shareGlobal.visible) {
            context.drawText(tr, Text.of("Chat..."),
                    shareGlobal.getX() + (shareGlobal.getWidth() / 2) - tr.getWidth("Chat...") / 2,
                    shareGlobal.getY() - 14, HandbookConfig.INSTANCE.textColor, false);
        }

        context.fill(line1x, 15, line1x + 1, height - 10, 100, HandbookConfig.INSTANCE.bordersColor);
        context.fill(line2x, 15, line2x + 1, height - 10, 100, HandbookConfig.INSTANCE.bordersColor);

        for (Element element : children())
            ((Drawable) element).render(context, mouseX, mouseY, delta);
        if (filterWidget.filtersActive()) {
            int x = filterButton.getX();
            int y = filterButton.getY();
            context.fill(x, y, x + 1, y + 11, HandbookConfig.INSTANCE.favouriteColor);
            context.fill(x + 11, y, x + 12, y + 11, HandbookConfig.INSTANCE.favouriteColor);
            context.fill(x, y, x + 12, y + 1, HandbookConfig.INSTANCE.favouriteColor);
            context.fill(x, y + 11, x + 12, y + 12, HandbookConfig.INSTANCE.favouriteColor);
        }
        matrices.pop();
        RenderSystem.disableBlend();
    }

    public void openTrades(TradeOfferList trades, String name) {
        tradesWidget.visible = true;
        tradesWidget.setName(name);
        tradeList.setX(40 + categoriesWidget.listWidth + optionsWidget.listWidth);
        tradeList.setEntries(trades, displayWidget.getEntry().getID());

        displayWidget.visible = false;
        back.active = true;
        back.visible = true;

        displayButtonsState(false);
        worldButtonsState(false);
    }

    public void openDisplay() {
        tradesWidget.visible = false;
        tradeList.setX(10000);

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
            optionsWidget.updateSizeShrink(screenHeight - (HandbookConfig.INSTANCE.editorMode ? 80 : 60), 30);
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

    public void setEntries(Category<? extends Entry> category) {
        int screenHeight = client.getWindow().getScaledHeight();
        int screenWidth = client.getWindow().getScaledWidth();
        int maxWidth = 0;

        if (category.getEntries().isEmpty()) maxWidth = 110;
        else {
            for (Entry entry : category.getEntries()) {
                int width = tr.getWidth(entry.getTitle());
                if (width > maxWidth) maxWidth = width;
            }
            maxWidth = Math.min(maxWidth, 150);
            maxWidth = maxWidth + 10;
        }

        optionsWidget.setDimensions(maxWidth + 10, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 80 : 60));
        optionsWidget.setPosition(30 + categoriesWidget.listWidth, 30);
        optionsWidget.listWidth = maxWidth + 10;
        optionsWidget.setEntries(category.getEntries(), BaseEntry.Type.Entry);
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;
        addEntry.setX(line1x + (line2x - line1x) / 2 - 20);
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
                optionsWidget.setEntries(activeCategory.getEntries(), BaseEntry.Type.Entry);
                lastFilter = "";
                return;
            }
            String s = searchBox.getText().toLowerCase();
            optionsWidget.setEntriesNoWidth(activeCategory.getEntries().stream()
                    .map(entry -> new Pair<Entry, Integer>(entry, entry.getClearTitle().toLowerCase().indexOf(s)))
                    .filter(this::applyFilter)
                    .sorted(Comparator.comparingInt(Pair::getRight))
                    .map(Pair::getLeft)
                    .toList(), BaseEntry.Type.Entry);
        }
        lastFilter = searchBox.getText();
    }

    private boolean applyFilter(Pair<Entry, Integer> pair) {
        return pair.getRight() >= 0 && (!filterWidget.filtersActive() || !filterWidget.checkEntry(pair.getLeft()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        lastKey = keyCode;

        if (!super.keyPressed(keyCode, scanCode, modifiers)
                && client.options.inventoryKey.matchesKey(keyCode, scanCode)) close();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        categoriesWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        optionsWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        tradeList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
