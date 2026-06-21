package net.handbook.main.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.DataManager;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.CategoryWriter;
import net.handbook.main.editor.EditScreen;
import net.handbook.main.editor.LocationWriter;
import net.handbook.main.editor.NPCWriter;
import net.handbook.main.mixin.ScreenAccessor;
import net.handbook.main.resources.*;
import net.handbook.main.resources.entry.Category;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.PositionEntry;
import net.handbook.main.resources.entry.TraderEntry;
import net.handbook.main.element.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class HandbookScreen extends Screen implements ScreenWithCategoryList, ScreenWithFilters {

    public MinecraftClient client;
    public TextRenderer tr;

    //First column
    public ListWidget categoriesWidget;
    @SuppressWarnings("unused")
    public TextButton openTradesScreen;
    @SuppressWarnings("unused")
    public TextButton openMapScreen;
    public TextButton addCategory;
    //Second column
    public ListWidget optionsWidget;
    public TextFieldWidget searchBox;
    public TexturedButtonWidget filterButton;
    public FilterWidget filterWidget;
    public TextButton addEntry;
    //Entry display
    public EntryDisplay entryDisplay;
    public TextButton setWaypoint;
    public TextButton openTrades;
    public TextButton shareLocation;
    public TextButton delete;
    public TextButton resetTrades;
    //Trade display
    public TradeDisplay tradeDisplay;
    public TradeListWidget tradeList;
    public TextButton back;
    public TextButton shareCost;
    public TextButton shareTrader;
    public TextButton shareFull;
    //Chat selection
    public TextButton shareGlobal;
    public TextButton shareLocal;
    public TextButton shareWorld;
    public TextButton shareLFG;
    public TextButton shareReply;
    public TextButton shareCancel;
    //Other
    public TextButton clearWaypoint;
    public TextButton continueWaypoint;

    public Category<? extends Entry> activeCategory;
    public TradeListWidgetEntry sharedTrade;
    private ShareMode shareMode;

    public int line1x;
    public int line2x;
    private int lastKey;
    private String lastFilter = "";
    private boolean spoofingWarn;

    public HandbookScreen() {
        super(Text.empty());
    }

    @Override
    protected void init() {
        client = MinecraftClient.getInstance();
        tr = client.textRenderer;

        if (DataManager.getCategories().isEmpty()) {
            close();
            client.inGameHud.getChatHud().addMessage(Text.of("Handbook 2.0 data is missing!"));
            return;
        }
        activeCategory = DataManager.getCategories().getFirst();
        addElements();
        spoofingWarn = WaypointManager.getShard().equals("unknown");
        super.init();
    }

    private void addElements() {
        int screenHeight = client.getWindow().getScaledHeight();
        int screenWidth = client.getWindow().getScaledWidth();

        addDrawableChild(clearWaypoint = new TextButton(20, 2, 76, 11,
                "Clear waypoint", button -> {
            WaypointManager.setState(false);
            clearWaypoint.active = false;
            clearWaypoint.visible = false;
            continueWaypoint.active = false;
            continueWaypoint.visible = false;
        }));
        clearWaypoint.active = WaypointManager.isActive();
        clearWaypoint.visible = WaypointManager.isActive();

        addDrawableChild(continueWaypoint = new TextButton(99, 2, 45, 11,
                "Continue", button -> {
            WaypointManager.continueOrSkip();
            if (!WaypointManager.isActive()) {
                clearWaypoint.active = false;
                clearWaypoint.visible = false;
                continueWaypoint.active = false;
                continueWaypoint.visible = false;
            }
        }));
        continueWaypoint.active = WaypointManager.isActive();
        continueWaypoint.visible = WaypointManager.isActive();

        int maxWidth = 0;

        for (Category<? extends Entry> category : DataManager.getCategories()) {
            int width = tr.getWidth(category.title());
            if (width > maxWidth) maxWidth = width;
        }

        maxWidth += 20;
        line1x = maxWidth + 20;

        addDrawableChild(categoriesWidget = new ListWidget(
                maxWidth + 3, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 90 : 70), 30));
        categoriesWidget.setX(20);
        categoriesWidget.setEntries(DataManager.getCategories(), ListType.Category, true);
        categoriesWidget.updateHighlight(categoriesWidget.children().getFirst(),true);
        activeCategory = (Category<? extends Entry>) categoriesWidget.children().getFirst().entry;

        maxWidth = 0;

        addDrawableChild(addCategory = new TextButton(TextButton.Type.Positive, line1x / 2 - 20, screenHeight - 45, 40, 11,
                "Add", button -> EditScreen.open(null, EntryType.normal, null)));
        addCategory.visible = HandbookConfig.INSTANCE.editorMode;
        addCategory.active = HandbookConfig.INSTANCE.editorMode;

        addDrawableChild(openTradesScreen = new TextButton(line1x / 2 - 37, screenHeight - 30, 75, 11,
                "Trades", button -> HandbookClient.openTradeScreen()));

        //todo update part 2
//        addDrawableChild(openMapScreen = new TextButton(line1x / 2 - 37, screenHeight - 18, 75, 11,
//                "Map", button -> HandbookClient.openMapScreen()));

        if (((Category<? extends Entry>) categoriesWidget.children().getFirst().entry).entries().isEmpty()) maxWidth = 110;
        else {
            for (Entry entry : ((Category<? extends Entry>) categoriesWidget.children().getFirst().entry).entries()) {
                int width = tr.getWidth(entry.title());
                if (width > maxWidth) maxWidth = width;
            }
            maxWidth = Math.min(maxWidth, 150) + 10;
        }

        addDrawableChild(optionsWidget = new ListWidget(
                maxWidth + 12, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 80 : 60), 30));
        optionsWidget.setX(30 + categoriesWidget.listWidth);
        optionsWidget.setEntries(((Category<? extends Entry>) categoriesWidget.children().getFirst().entry).entries(), ListType.Entry, true);
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = width - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        addDrawableChild(addEntry = new TextButton(TextButton.Type.Positive, line1x + (line2x - line1x) / 2 - 20, screenHeight - 45, 40, 11,
                "Add", button -> EditScreen.open(null, activeCategory.type(), activeCategory)));
        addEntry.visible = HandbookConfig.INSTANCE.editorMode;
        addEntry.active = HandbookConfig.INSTANCE.editorMode;

        addDrawableChild(searchBox = new TextFieldWidget(
                tr, line1x + 16, 15, line2x - line1x - 16, 14, Text.of("")));
        searchBox.setPlaceholder(Text.of("Search...").getWithStyle(Style.EMPTY.withItalic(true).withColor(-10197916)).getFirst());

        addDrawableChild(filterButton = new TexturedButtonWidget(line1x + 2, 16, 12, 12,
                new ButtonTextures(Identifier.of("handbook", "filter_unfocused"),
                Identifier.of("handbook", "filter_focused")),
                button -> toggleFilterWidget()));

        addDrawableChild(filterWidget = new FilterWidget(line1x + 2, 30, 0, 0));
        filterWidget.active = false;
        filterWidget.visible = false;

        addDrawableChild(setWaypoint = new TextButton(40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 42, 76, 11,
                "Set waypoint", button -> setWaypointFromDisplayed()));

        addDrawableChild(shareLocation = new TextButton(40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 54, 76, 11,
                "Share location", button -> worldButtonsState(true)));

        addDrawableChild(delete = new TextButton(TextButton.Type.Negative, screenWidth - 73, screenHeight - 30, 69, 11,
                "Delete", button -> deleteDisplayedEntry()));

        addDrawableChild(resetTrades = new TextButton(TextButton.Type.Negative, screenWidth - 73, screenHeight - 42, 69, 11,
                "Reset trades", button -> {
            resetTrades.active = false;
            resetTrades.visible = false;
            openTrades.active = false;
            openTrades.visible = false;
            deleteDisplayedTrade();
        }));

        addDrawableChild(shareGlobal = new TextButton(120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 90, 36, 11,
                "Global", button -> {
            if (entryDisplay.visible) shareLocation(ChatChannel.GLOBAL);
            else shareTrade(ChatChannel.GLOBAL);
        }));

        addDrawableChild(shareLocal = new TextButton(120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 78, 36, 11,
                "Local", button -> {
            if (entryDisplay.visible) shareLocation(ChatChannel.LOCAL);
            else shareTrade(ChatChannel.LOCAL);
        }));

        addDrawableChild(shareWorld = new TextButton(120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 66, 36, 11,
                "World", button -> {
            if (entryDisplay.visible) shareLocation(ChatChannel.WORLD);
            else shareTrade(ChatChannel.WORLD);
        }));

        addDrawableChild(shareLFG = new TextButton(120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 54, 36, 11,
                "LFG", button -> {
            if (entryDisplay.visible) shareLocation(ChatChannel.LFG);
            else shareTrade(ChatChannel.LFG);
        }));

        addDrawableChild(shareReply = new TextButton(120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 42, 36, 11,
                "Reply", button -> {
            if (entryDisplay.visible) shareLocation(ChatChannel.REPLY);
            else shareTrade(ChatChannel.REPLY);
        }));

        addDrawableChild(shareCancel = new TextButton(TextButton.Type.Negative, 120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 36, 11,
                "Cancel", button -> {
            if (entryDisplay.visible) worldButtonsState(false);
            else cancelSharingTrade();
        }));

        addDrawableChild(openTrades = new TextButton(40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 76, 11,
                "Open trades", button -> openTrades(entryDisplay.entry().offers().entries(), entryDisplay.entry().title())));

        addDrawableChild(back = new TextButton(40 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 26, 11,
                "Back", button -> openDisplay()));
        back.active = false;
        back.visible = false;

        addDrawableChild(shareCost = new TextButton(120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 54, 39, 11,
                "Cost", button -> selectShareMode(ShareMode.COST)));

        addDrawableChild(shareTrader = new TextButton(120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 42, 39, 11,
                "Trader", button -> selectShareMode(ShareMode.TRADER)));

        addDrawableChild(shareFull = new TextButton(120 + categoriesWidget.listWidth + optionsWidget.listWidth, screenHeight - 30, 39, 11,
                "Full", button -> selectShareMode(ShareMode.FULL)));

        displayButtonsState(false);
        worldButtonsState(false);
        tradeButtonsState(false);

        addDrawable(entryDisplay = new EntryDisplay(30 + categoriesWidget.listWidth + optionsWidget.listWidth, 20, maxWidth, screenHeight - 40));
        addDrawable(tradeDisplay = new TradeDisplay(30 + categoriesWidget.listWidth + optionsWidget.listWidth, 20, maxWidth));
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
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).getFirst(),
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

        for (Drawable drawable : ((ScreenAccessor) this).drawables())
            drawable.render(context, mouseX, mouseY, delta);

        if (filterWidget.filtersActive()) {
            int x = filterButton.getX();
            int y = filterButton.getY();
            context.fill(x, y, x + 1, y + 11, HandbookConfig.INSTANCE.favouriteColor);
            context.fill(x + 11, y, x + 12, y + 11, HandbookConfig.INSTANCE.favouriteColor);
            context.fill(x, y, x + 12, y + 1, HandbookConfig.INSTANCE.favouriteColor);
            context.fill(x, y + 11, x + 12, y + 12, HandbookConfig.INSTANCE.favouriteColor);
        }
        matrices.pop();
        if (spoofingWarn && !entryDisplay.visible && !tradeDisplay.visible) displaySpoofingWarn(context);
        RenderSystem.disableBlend();
    }

    private void displaySpoofingWarn(DrawContext context) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(entryDisplay.getX(), entryDisplay.getY(), 1);

        matrices.push();
        matrices.scale(1.75f, 1.75f, 1);
        context.drawText(tr, Text.literal("World Name Spoofing").setStyle(Style.EMPTY.withColor(Formatting.RED))
                .append(Text.literal(" is required.").setStyle(Style.EMPTY.withColor(Formatting.WHITE))), 5, 0, 16777215, true);
        matrices.pop();

        context.drawText(tr, Text.literal("Enable it in /peb under Technical settings.").setStyle(Style.EMPTY.withColor(Formatting.WHITE)),
                10, 20, 16777215, true);
        context.drawText(tr, Text.literal("Ignore if in playerplots, build etc.").setStyle(Style.EMPTY.withColor(Formatting.WHITE)),
                10, 30, 16777215, true);
        matrices.pop();
    }

    public void openTrades(List<HandbookTradeOffer> trades, String name) {
        tradeDisplay.visible = true;
        tradeDisplay.setName(name);
        tradeList.setX(40 + categoriesWidget.listWidth + optionsWidget.listWidth);
        tradeList.setEntries(trades, entryDisplay.entry().id());

        entryDisplay.visible = false;
        back.active = true;
        back.visible = true;

        displayButtonsState(false);
        worldButtonsState(false);
    }

    public void openDisplay() {
        tradeDisplay.visible = false;
        tradeList.setX(10000);

        entryDisplay.visible = true;
        back.active = false;
        back.visible = false;

        displayButtonsState(true);
        worldButtonsState(false);
        tradeButtonsState(false);
        moveWorldButtons(120 + categoriesWidget.listWidth + optionsWidget.listWidth,
                client.getWindow().getScaledHeight() - 30);
        displayEntry(entryDisplay.entry());
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

        if (category.entries().isEmpty()) maxWidth = 110;
        else {
            for (Entry entry : category.entries()) {
                int width = tr.getWidth(entry.title());
                if (width > maxWidth) maxWidth = width;
            }
            maxWidth = Math.min(maxWidth, 150) + 10;
        }

        optionsWidget.setDimensions(maxWidth + 10, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 80 : 60));
        optionsWidget.setPosition(30 + categoriesWidget.listWidth, 30);
        optionsWidget.listWidth = maxWidth + 10;
        optionsWidget.setEntries(category.entries(), ListType.Entry, true);
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;
        addEntry.setX(line1x + (line2x - line1x) / 2 - 20);
        searchBox.setWidth(line2x - line1x - 16);

        maxWidth = screenWidth - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        entryDisplay.setWidth(maxWidth);
        entryDisplay.setX(30 + categoriesWidget.listWidth + optionsWidget.listWidth);
        displayEntry(null);
        tradeDisplay.setWidth(maxWidth);
        tradeDisplay.setX(30 + categoriesWidget.listWidth + optionsWidget.listWidth);

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

    public void displayEntry(Entry entry) {
        entryDisplay.setEntry(entry);
        displayButtonsState(false);
        if (entry == null) return;

        if (entry.shard() != null) {
            setWaypoint.visible = true;
            setWaypoint.active = true;
            shareLocation.visible = true;
            shareLocation.active = true;
        }
        if (entry.hasOffers()) {
            openTrades.visible = true;
            openTrades.active = true;
            if (HandbookConfig.INSTANCE.editorMode) {
                resetTrades.visible = true;
                resetTrades.active = true;
            }
        }
        if (entry.waypoints() != null) {
            setWaypoint.visible = true;
            setWaypoint.active = true;
        }
        if ((HandbookConfig.INSTANCE.editorMode)) {
            delete.visible = true;
            delete.active = true;
        }
    }

    private void deleteDisplayedEntry() {
        Entry entry = entryDisplay.entry();
        switch (activeCategory.title()) {
            case "Locations" -> LocationWriter.delete((PositionEntry) entry);
            case "NPC" -> NPCWriter.delete((TraderEntry) entry);
            default -> {
                for (CategoryWriter<? extends Entry> writer : DataManager.writers) {
                    if (writer.category.equals(activeCategory)) {
                        writer.delete(entry);
                        break;
                    }
                }
            }
        }
        double scroll = optionsWidget.getScrollAmount();
        filterEntries(false);
        optionsWidget.setScrollAmount(scroll);
        displayEntry(null);
    }

    private void deleteDisplayedTrade() {
        try {
            Files.deleteIfExists(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + entryDisplay.entry().id()));
            client.inGameHud.getChatHud().addMessage(Text.of("Removed trades. Interact with the villager again to update them."));
        }
        catch (IOException ignored) {}
    }

    private void shareLocation(ChatChannel chatChannel) {
        if (client.getNetworkHandler() == null) return;

        Entry entry = entryDisplay.entry();
        String position = "Position: " + entry.position()[0] + ", " + entry.position()[1] + ", " + entry.position()[2];
        client.getNetworkHandler().sendCommand(chatChannel.chatId + " "
                + entry.clearTitle().replaceAll(" \\((.*?)\\)", "")
                + " (" + entry.shard() + ") | " + position);

        client.setScreen(null);
    }

    private void setWaypointFromDisplayed() {
        Entry entry = entryDisplay.entry();
        if (entry.waypoints() != null) {
            client.inGameHud.getChatHud().addMessage(Text.of("Path started: " + entry.title()));
            WaypointManager.setWaypointChain(List.of((entry.waypoints())));
        }
        else {
            if (entry.shard().equals(WaypointManager.getShard())) {
                client.inGameHud.getChatHud().addMessage(Text.of("Waypoint set: " + entry.title()));
                WaypointManager.setWaypoint(entry);
            }
            else {
                client.inGameHud.getChatHud().addMessage(Text.of("§cERROR: This waypoint belongs to a different shard.")
                        .getWithStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.of("If you believe the shard is correct, enable `Spoof World Names` in /peb.")))).getFirst());
            }
        }
        client.setScreen(null);
    }

    public void startSharingTrade(TradeListWidgetEntry entry) {
        cancelSharingTrade();

        sharedTrade = entry;
        entry.setHighlighted(true);

        int buttonsY = (int) client.mouse.getY() / client.options.getGuiScale().getValue() + 24;

        tradeButtonsState(true);
        moveWorldButtons(tradeDisplay.x + 182, buttonsY + 6);
        moveTradeButtons(tradeDisplay.x + 140, buttonsY - 18);
        shareCancel.setPosition(tradeDisplay.x + 141, buttonsY + 12);
        shareCancel.active = true;
        shareCancel.visible = true;
    }

    private void selectShareMode(ShareMode mode) {
        shareMode = mode;
        worldButtonsState(true);
    }

    private void shareTrade(ChatChannel chatChannel) {
        ((TraderEntry) entryDisplay.entry()).share(chatChannel, sharedTrade.trade, shareMode);
    }

    private void cancelSharingTrade() {
        if (sharedTrade != null) sharedTrade.setHighlighted(false);
        sharedTrade = null;

        worldButtonsState(false);
        tradeButtonsState(false);
    }

    public void filterEntries(boolean scheduled) {
        if (!searchBox.getText().equals(lastFilter) || !scheduled) {
            if (searchBox.getText().isEmpty() && !filterWidget.filtersActive()) {
                optionsWidget.setEntries(activeCategory.entries(), ListType.Entry, true);
                lastFilter = "";
                return;
            }
            String s = searchBox.getText().toLowerCase();
            optionsWidget.setEntries(activeCategory.entries().stream()
                    .map(entry -> new Pair<Entry, Integer>(entry, entry.clearTitle().toLowerCase().indexOf(s)))
                    .filter(this::applyFilter)
                    .sorted(Comparator.comparingInt(Pair::getRight))
                    .map(Pair::getLeft)
                    .toList(), ListType.Entry, false);
        }
        lastFilter = searchBox.getText();
    }

    private boolean applyFilter(Pair<Entry, Integer> pair) {
        return pair.getRight() >= 0 && (!filterWidget.filtersActive() || filterWidget.checkEntry(pair.getLeft()));
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
            entryDisplay.renderImage = false;
            client.player.closeScreen();
            super.close();
        }
    }

    @Override
    public Category<? extends Entry> activeCategory() {
        return activeCategory;
    }

    @Override
    public void scheduledFilter() {
        filterEntries(true);
    }
}
