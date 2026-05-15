package net.handbook.main.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.DataManager;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.mixin.ScreenAccessor;
import net.handbook.main.resources.*;
import net.handbook.main.resources.entry.Category;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.TraderEntry;
import net.handbook.main.element.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class TradeScreen extends Screen implements ScreenWithFilters {

    public static final HashMap<String, List<HandbookTradeOffer>> offers = new HashMap<>();

    private MinecraftClient client;
    private TextRenderer tr;

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private TextButton openMainScreen;
    @SuppressWarnings({"unused"})
    private TextButton openMapScreen;
    private TextFieldWidget searchBox;
    private TradeListWidget favouritesWidget;
    private TradeListWidget resultsWidget;
    private TextButton openTrader;
    private TextButton share;

    private TextButton shareCost;
    private TextButton shareTrader;
    private TextButton shareFull;
    private TextButton shareGlobal;
    private TextButton shareLocal;
    private TextButton shareWorld;
    private TextButton shareLFG;
    private TextButton shareReply;
    private TextButton shareCancel;

    private TraderEntry trader;
    public TradeListWidgetEntry selectedEntry;
    private ShareMode shareMode;

    private int lastKey;
    private String lastFilter = "";

    public TradeScreen() {
        super(Text.empty());
    }

    @Override
    protected void init() {
        client = MinecraftClient.getInstance();
        tr = client.textRenderer;

        addElements();

        offers.forEach((id, offers) -> resultsWidget.addEntries(offers, id));

        List<String> favourite = DataManager.getMarkedEntries("favTrades");
        offers.forEach((id, offers) -> {
            for (int i = 0; i < offers.size(); i++)
                if (favourite.contains(id + "&" + i)) favouritesWidget.addEntry(offers.get(i), id + "&" + i);
        });

        lastFilter = "";
        trader = null;
        selectedEntry = null;
        cancelSharing();
        super.init();
    }

    private void addElements() {
        int screenHeight = client.getWindow().getScaledHeight();

        addDrawableChild(openMainScreen = new TextButton(28, screenHeight - 30, 75, 11,
                "Handbook", button -> HandbookClient.openHandbookScreen()));

        //todo update part 2
//        addDrawableChild(openMapScreen = new TextButton(40, screenHeight - 18, 75, 11,
//                "Map", button -> HandbookClient.openMapScreen()));

        addDrawableChild(searchBox = new TextFieldWidget(
                tr, 131, 15, 260, 14, Text.of("")));
        searchBox.setPlaceholder(Text.of("Search...").getWithStyle(Style.EMPTY.withItalic(true).withColor(-10197916)).getFirst());

        addDrawableChild(favouritesWidget = new TradeListWidget(5, 125, screenHeight - 70, 30));
        addDrawableChild(resultsWidget = new TradeListWidget(135, 125, screenHeight - 70, 30));

        addDrawableChild(openTrader = new TextButton(265, screenHeight - 42, 70, 11,
                "Open trader", button -> openTrader()));
        openTrader.active = false;
        openTrader.visible = false;

        addDrawableChild(share = new TextButton(265, screenHeight - 30, 70, 11,
                "Share", button -> startSharing()));
        share.active = false;
        share.visible = false;

        addDrawableChild(shareGlobal = new TextButton(380, screenHeight - 90, 36, 11,
                "Global", button -> share(ChatChannel.GLOBAL)));

        addDrawableChild(shareLocal = new TextButton(380, screenHeight - 78, 36, 11,
                "Local", button -> share(ChatChannel.LOCAL)));

        addDrawableChild(shareWorld = new TextButton(380, screenHeight - 66, 36, 11,
                "World", button -> share(ChatChannel.WORLD)));

        addDrawableChild(shareLFG = new TextButton(380, screenHeight - 54, 36, 11,
                "LFG", button -> share(ChatChannel.LFG)));

        addDrawableChild(shareReply = new TextButton(380, screenHeight - 42, 36, 11,
                "Reply", button -> share(ChatChannel.REPLY)));

        addDrawableChild(shareCost = new TextButton(340, screenHeight - 78, 39, 11,
                "Cost", button -> selectMode(ShareMode.COST)));

        addDrawableChild(shareTrader = new TextButton(340, screenHeight - 66, 39, 11,
                "Trader", button -> selectMode(ShareMode.TRADER)));

        addDrawableChild(shareFull = new TextButton(340, screenHeight - 54, 39, 11,
                "Full", button -> selectMode(ShareMode.FULL)));

        addDrawableChild(shareCancel = new TextButton(TextButton.Type.Negative,
                342, screenHeight - 30, 36, 11, "Cancel", button -> {
            worldButtonsState(false);
            tradeButtonsState(false);
        }));
        worldButtonsState(false);
        tradeButtonsState(false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        MatrixStack matrices = context.getMatrices();
        RenderSystem.enableBlend();
        context.fill(0, 0, width, 15, 10, HandbookConfig.INSTANCE.screenHeadColor);
        matrices.push();
        matrices.scale(1.5f, 1.5f, 1);
        matrices.translate(0, 0, 20);
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).getFirst(),
                (int) (width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1,
                HandbookConfig.INSTANCE.textColor, false);
        matrices.pop();

        matrices.push();
        matrices.scale(1.25f, 1.25f, 1);
        matrices.translate(0, 0, 20);
        context.drawText(tr, Text.of("Trade Search").getWithStyle(Style.EMPTY.withItalic(true)).getFirst(),
                15, 3, HandbookConfig.INSTANCE.textColor, false);
        matrices.pop();

        context.fill(130, 15, 131, height - 10, 100, HandbookConfig.INSTANCE.bordersColor);
        context.fill(260, 29, 261, height - 10, 100, HandbookConfig.INSTANCE.bordersColor);

        matrices.push();
        matrices.translate(0, 0, 20);
        context.drawText(tr, Text.of("Favourite"),  21, 20,
                HandbookConfig.INSTANCE.textColor, false);
        if (favouritesWidget.children().isEmpty())
            context.drawText(tr, Text.of("Right click to add"),
                    65 - tr.getWidth("Right click to add") / 2, 35,
                    HandbookConfig.INSTANCE.textColor, false);
        if (resultsWidget.children().isEmpty())
            context.drawText(tr, Text.of("Nothing found :("),
                    197 - tr.getWidth("Nothing found :(") / 2, 35,
                    HandbookConfig.INSTANCE.textColor, false);
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
        matrices.pop();

        if (trader != null) {
            matrices.push();
            matrices.translate(260, 35, 1);

            matrices.push();
            matrices.scale(1.75f, 1.75f, 1);
            context.drawText(tr, trader.title(), 5, 0, 16777215, true);
            matrices.pop();

            context.drawText(tr, "Shard: " + (trader.shard() != null ? trader.shard() : "unknown"),
                    10, 20, HandbookConfig.INSTANCE.textColor, false);

            int[] coords = trader.position();
            context.drawText(tr, "Position: " + (coords != null ? coords[0] + ", " + coords[1] + ", " + coords[2] : "unknown"),
                    10, 30, HandbookConfig.INSTANCE.textColor, false);
            matrices.pop();
        }
        for (Drawable drawable : ((ScreenAccessor) this).drawables())
            drawable.render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
    }

    public static void addEntries(List<HandbookTradeOffer> entries, String id) {
        offers.put(id, entries);
    }

    public void filterEntries() {
        String searchString = searchBox.getText().toLowerCase();
        if (searchString.equals(lastFilter)) return;

        if (searchString.isEmpty()) {
            offers.forEach((id, offers) -> resultsWidget.addEntries(offers, id));
            lastFilter = "";
            return;
        }
        resultsWidget.clear();
        offers.forEach((id, offers) -> {
            for (int i = 0; i < offers.size(); i++) {
                HandbookTradeOffer offer = offers.get(i);

                if (offer.buyItem1().getName().getString().toLowerCase().contains(searchString)
                || (offer.buyItem2().isPresent() && offer.buyItem2().get().getName().getString().toLowerCase().contains(searchString))
                || offer.sellItem().getName().getString().toLowerCase().contains(searchString)) {
                    resultsWidget.addEntry(offer, id + "&" + i);
                }
            }
        });
        resultsWidget.setScrollAmount(0);
        trader = null;
        openTrader.active = false;
        openTrader.visible = false;
        share.active = false;
        share.visible = false;
        cancelSharing();

        lastFilter = searchString;
    }

    public void setSearchText(String s) {
        searchBox.setText(s);
    }

    public void setTraderInfo(String id) {
        cancelSharing();
        for (Category<? extends Entry> category : DataManager.getCategories()) {
            if (!category.type().equals(EntryType.trader) || category.hidden()) continue;

            for (Entry entry : category.entries()) {
                if (!entry.id().equals(id)) continue;

                trader = (TraderEntry) entry;
                openTrader.active = true;
                openTrader.visible = true;
                share.active = true;
                share.visible = true;
                return;
            }
        }
        trader = new TraderEntry("Unknown trader", "Could not find a trader with this id: " + id,
                "", "unknown", new int[]{0, 0, 0});
        openTrader.active = false;
        openTrader.visible = false;
    }

    private void openTrader() {
        if (trader == null) return;

        HandbookScreen screen = Objects.requireNonNull(HandbookClient.openHandbookScreen());
        screen.openDisplay();
        screen.displayEntry(trader);
    }

    public void removeFavourite(TradeListWidgetEntry entry) {
        favouritesWidget.children().removeIf(child -> child.id.equals(entry.id));
    }

    public void addFavourite(TradeListWidgetEntry entry) {
        favouritesWidget.children().add(entry);
    }

    public void startSharing() {
        worldButtonsState(false);
        tradeButtonsState(true);
        shareCancel.active = true;
        shareCancel.visible = true;
    }

    public void selectMode(ShareMode mode) {
        shareMode = mode;
        worldButtonsState(true);
    }

    public void share(ChatChannel chatChannel) {
        trader.share(chatChannel, selectedEntry.trade, shareMode);
    }

    public void cancelSharing() {
        if (selectedEntry != null) selectedEntry.setHighlighted(false);
        selectedEntry = null;

        worldButtonsState(false);
        tradeButtonsState(false);
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

    public void tradeButtonsState(boolean state) {
        shareCost.active = state;
        shareCost.visible = state;
        shareTrader.active = state;
        shareTrader.visible = state;
        shareFull.active = state;
        shareFull.visible = state;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        favouritesWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        resultsWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
            client.player.closeScreen();
            super.close();
        }
    }

    @Override
    public void scheduledFilter() {
        filterEntries();
    }
}
