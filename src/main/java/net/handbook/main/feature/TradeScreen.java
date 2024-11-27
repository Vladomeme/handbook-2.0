package net.handbook.main.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.resources.category.Category;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.TraderEntry;
import net.handbook.main.widget.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOfferList;

import java.util.HashMap;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class TradeScreen extends Screen {

    public static final TradeScreen INSTANCE = new TradeScreen(Text.of(""));
    public final HashMap<String, TradeOfferList> offers = new HashMap<>();

    private MinecraftClient client;
    private TextRenderer tr;
    private final HandbookScreen screen = HandbookClient.handbookScreen;

    @SuppressWarnings("unused")
    private HandbookButtonWidget backToHandbook;
    private TextFieldWidget searchBox;
    private TradeListWidget favouritesWidget;
    private TradeListWidget resultsWidget;
    private HandbookButtonWidget openTrader;
    private HandbookButtonWidget share;

    private HandbookButtonWidget shareCost;
    private HandbookButtonWidget shareTrader;
    private HandbookButtonWidget shareFull;
    private HandbookButtonWidget shareGlobal;
    private HandbookButtonWidget shareLocal;
    private HandbookButtonWidget shareWorld;
    private HandbookButtonWidget shareLFG;
    private HandbookButtonWidget shareReply;
    private HandbookButtonWidget shareCancel;

    private TraderEntry trader;
    public TradeListWidgetEntry selectedEntry;
    private TradesWidget.Mode shareMode;

    private int lastKey;
    private String lastFilter = "";

    private TradeScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        client = MinecraftClient.getInstance();
        tr = client.textRenderer;

        addElements();

        offers.forEach((id, offers) -> resultsWidget.addEntries(offers, id));

        List<String> favourite = screen.markedEntries.getMarkedEntries("favTrades");
        if (favourite == null) screen.markedEntries.addCategory("favTrades");
        else offers.forEach((id, offers) -> {
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

        addDrawableChild(backToHandbook = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                40, screenHeight - 30, 50, 11,
                "Handbook", button -> client.setScreen(HandbookClient.handbookScreen)));

        addDrawableChild(searchBox = new TextFieldWidget(
                tr, 131, 15, 260, 14, Text.of("")));
        searchBox.setPlaceholder(Text.of("Search...").getWithStyle(Style.EMPTY.withItalic(true).withColor(-10197916)).get(0));

        addDrawableChild(favouritesWidget = new TradeListWidget(5, 125, screenHeight - 70, 30));
        addDrawableChild(resultsWidget = new TradeListWidget(135, 125, screenHeight - 70, 30));

        addDrawableChild(openTrader = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                265, screenHeight - 42, 70, 11,
                "Open trader", button -> openTrader()));
        openTrader.active = false;
        openTrader.visible = false;

        addDrawableChild(share = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                265, screenHeight - 30, 70, 11,
                "Share", button -> startSharing()));
        share.active = false;
        share.visible = false;

        addDrawableChild(shareGlobal = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                380, screenHeight - 90, 36, 11,
                "Global", button -> share("g")));

        addDrawableChild(shareLocal = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                380, screenHeight - 78, 36, 11,
                "Local", button -> share("l")));

        addDrawableChild(shareWorld = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                380, screenHeight - 66, 36, 11,
                "World", button -> share("wc")));

        addDrawableChild(shareLFG = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                380, screenHeight - 54, 36, 11,
                "LFG", button -> share("lfg")));

        addDrawableChild(shareReply = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                380, screenHeight - 42, 36, 11,
                "Reply", button -> share("r")));

        addDrawableChild(shareCost = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                340, screenHeight - 78, 39, 11,
                "Cost", button -> selectMode(TradesWidget.Mode.COST)));

        addDrawableChild(shareTrader = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                340, screenHeight - 66, 39, 11,
                "Trader", button -> selectMode(TradesWidget.Mode.TRADER)));

        addDrawableChild(shareFull = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                340, screenHeight - 54, 39, 11,
                "Full", button -> selectMode(TradesWidget.Mode.FULL)));

        addDrawableChild(shareCancel = new HandbookButtonWidget(HandbookButtonWidget.Type.Negative,
                342, screenHeight - 30, 36, 11,
                "Cancel", button -> {
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
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                (int) (width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1,
                HandbookConfig.INSTANCE.textColor, false);
        matrices.pop();

        matrices.push();
        matrices.scale(1.25f, 1.25f, 1);
        matrices.translate(0, 0, 20);
        context.drawText(tr, Text.of("Trade Search").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                15, 3, HandbookConfig.INSTANCE.textColor, false);
        matrices.pop();

        context.fill(130, 15, 131, height - 10, 100, HandbookConfig.INSTANCE.bordersColor);
        context.fill(260, 29, 261, height - 10, 100, HandbookConfig.INSTANCE.bordersColor);

        matrices.push();
        matrices.translate(0, 0, 20);
        context.drawText(tr, Text.of("Favourite"),  21, 20,
                HandbookConfig.INSTANCE.textColor, false);
        if (resultsWidget.children().isEmpty())
            context.drawText(tr, Text.of("Nothing found :("),
                    197 - tr.getWidth("Nothing found :(") / 2, 35,
                    HandbookConfig.INSTANCE.textColor, false);
        matrices.pop();

        if (trader != null) {
            matrices.push();
            matrices.translate(260, 35, 1);

            matrices.push();
            matrices.scale(1.75f, 1.75f, 1);
            context.drawText(tr, trader.getTitle(), 5, 0, 16777215, true);
            matrices.pop();

            context.drawText(tr, "Shard: " + (trader.getShard() != null ? trader.getShard() : "unknown"),
                    10, 20, HandbookConfig.INSTANCE.textColor, false);

            int[] coords = trader.getPosition();
            context.drawText(tr, "Position: " + (coords != null ? coords[0] + ", " + coords[1] + ", " + coords[2] : "unknown"),
                    10, 30, HandbookConfig.INSTANCE.textColor, false);
            matrices.pop();
        }
        for (Element element : children())
            ((Drawable) element).render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
    }

    public void addEntries(TradeOfferList entries, String id) {
        offers.put(id, entries);
    }

    public void filterEntries() {
        String search = searchBox.getText();
        if (search.equals(lastFilter)) return;

        if (search.isEmpty()) {
            offers.forEach((id, offers) -> resultsWidget.addEntries(offers, id));
            lastFilter = "";
            return;
        }
        resultsWidget.clear();
        offers.forEach((id, offers) -> {
            for (int i = 0; i < offers.size(); i++) {
                if (offers.get(i).getOriginalFirstBuyItem().getName().getString().toLowerCase().contains(search.toLowerCase()))
                    resultsWidget.addEntry(offers.get(i), id + "&" + i);
                else if (offers.get(i).getSecondBuyItem().getName().getString().toLowerCase().contains(search.toLowerCase()))
                    resultsWidget.addEntry(offers.get(i), id + "&" + i);
                else if (offers.get(i).getSellItem().getName().getString().toLowerCase().contains(search.toLowerCase()))
                    resultsWidget.addEntry(offers.get(i), id + "&" + i);
            }
        });
        resultsWidget.setScrollAmount(0);
        trader = null;
        openTrader.active = false;
        openTrader.visible = false;
        share.active = false;
        share.visible = false;
        cancelSharing();

        lastFilter = searchBox.getText();
    }

    public void setSearchText(String s) {
        searchBox.setText(s);
    }

    public void setTraderInfo(String id) {
        cancelSharing();
        for (Category<? extends Entry> category : HandbookClient.getCategories()) {
            if (!category.getType().equals("trader") || category.getTitle().equals("EXCLUDE")) continue;

            for (Entry entry : category.getEntries()) {
                if (!entry.getID().equals(id)) continue;

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

        HandbookClient.openHandbookScreen();
        screen.openDisplay();
        screen.displayWidget.setEntry(trader);
    }

    public void removeFavourite(TradeListWidgetEntry entry) {
        favouritesWidget.children().removeIf(child -> child.id.equals(entry.id));
    }

    public void addFavourite(TradeListWidgetEntry entry) {
        favouritesWidget.children().add(entry);
    }

    public void clear() {
        if (resultsWidget != null) resultsWidget.clear();
    }

    public void startSharing() {
        worldButtonsState(false);
        tradeButtonsState(true);
        shareCancel.active = true;
        shareCancel.visible = true;
    }

    public void selectMode(TradesWidget.Mode mode) {
        shareMode = mode;
        worldButtonsState(true);
    }

    public void share(String world) {
        if (MinecraftClient.getInstance().player == null) return;
        StringBuilder command = new StringBuilder();
        command.append(world).append(" ");
        ItemStack item;
        String position = "Position: " + trader.getPosition()[0] + ", " + trader.getPosition()[1] + ", " + trader.getPosition()[2];
        switch (shareMode) {
            case COST -> {
                item = selectedEntry.trade.getOriginalFirstBuyItem();
                command.append(item.getName().getString());
                if (item.getCount() != 1) command.append(" x").append(item.getCount());

                item = selectedEntry.trade.getSecondBuyItem();
                if (!item.isEmpty()) {
                    command.append(" + ").append(item.getName().getString());
                    if (item.getCount() != 1) command.append(" x").append(item.getCount());
                }

                item = selectedEntry.trade.getSellItem();
                command.append(" -> ").append(item.getName().getString());
                if (item.getCount() != 1) command.append(" x").append(item.getCount());

                command.append(" | ").append(trader.getClearTitle()).append(" (")
                        .append(trader.getShard()).append(")");
            }
            case TRADER -> {
                item = selectedEntry.trade.getSellItem();
                command.append(item.getName().getString());
                if (item.getCount() != 1) command.append(" x").append(item.getCount());

                command.append(" | ").append(trader.getClearTitle()).append(" (")
                        .append(trader.getShard()).append(") ")
                        .append(position);
            }
            case FULL -> {
                item = selectedEntry.trade.getOriginalFirstBuyItem();
                command.append(item.getName().getString());
                if (item.getCount() != 1) command.append(" x").append(item.getCount());

                item = selectedEntry.trade.getSecondBuyItem();
                if (!item.isEmpty()) {
                    command.append(" + ").append(item.getName().getString());
                    if (item.getCount() != 1) command.append(" x").append(item.getCount());
                }

                item = selectedEntry.trade.getSellItem();
                command.append(" -> ").append(item.getName().getString());
                if (item.getCount() != 1) command.append(" x").append(item.getCount());

                command.append(" | ").append(trader.getClearTitle()).append(" (")
                        .append(trader.getShard()).append(") ")
                        .append(position);
            }
        }
        MinecraftClient.getInstance().player.networkHandler.sendCommand(command.toString());

        MinecraftClient.getInstance().currentScreen = null;
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
}
