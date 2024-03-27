package net.handbook.main.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.resources.category.BaseCategory;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.TraderEntry;
import net.handbook.main.widget.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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

    private TexturedButtonWidget backToHandbook;
    private TextFieldWidget searchBox;
    private TradeListWidget favouritesWidget;
    private TradeListWidget resultsWidget;
    private TexturedButtonWidget openTrader;
    private TexturedButtonWidget share;

    private TexturedButtonWidget shareCost;
    private TexturedButtonWidget shareTrader;
    private TexturedButtonWidget shareFull;
    private TexturedButtonWidget shareGlobal;
    private TexturedButtonWidget shareLocal;
    private TexturedButtonWidget shareWorld;
    private TexturedButtonWidget shareLFG;
    private TexturedButtonWidget shareReply;
    private TexturedButtonWidget shareCancel;

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
        if (favourite != null) {
            offers.forEach((id, offers) -> {
                for (int i = 0; i < offers.size(); i++)
                    if (favourite.contains(id + "&" + i)) favouritesWidget.addEntry(offers.get(i), id + "&" + i);
            });
        }
        else screen.markedEntries.addCategory("favTrades");

        lastFilter = "";
        trader = null;
        selectedEntry = null;
        cancelSharing();
        super.init();
    }

    private void addElements() {
        int screenHeight = client.getWindow().getScaledHeight();

        addDrawableChild(backToHandbook = new TexturedButtonWidget(
                40, screenHeight - 30, 50, 11,
                0, 0, 11, new Identifier("handbook", "textures/button.png"),
                50, 22, button -> client.setScreen(HandbookClient.handbookScreen)));

        addDrawableChild(searchBox = new TextFieldWidget(
                tr, 131, 16, 260, 12, Text.of("")));
        searchBox.setPlaceholder(Text.of("Search...").getWithStyle(Style.EMPTY.withItalic(true).withColor(-10197916)).get(0));

        addDrawableChild(favouritesWidget = new TradeListWidget(5, 125, screenHeight - 70, 30, screenHeight - 40));
        addDrawableChild(resultsWidget = new TradeListWidget(135, 125, screenHeight - 70, 30, screenHeight - 40));

        addDrawableChild(openTrader = new TexturedButtonWidget(
                265, screenHeight - 42, 70, 11,
                0, 0, 11, new Identifier("handbook", "textures/open_trader.png"),
                70, 22, button -> openTrader()));
        openTrader.active = false;
        openTrader.visible = false;

        addDrawableChild(share = new TexturedButtonWidget(
                265, screenHeight - 30, 35, 11,
                0, 0, 11, new Identifier("handbook", "textures/share.png"),
                35, 22, button -> startSharing()));
        share.active = false;
        share.visible = false;

        addDrawableChild(shareGlobal = new TexturedButtonWidget(
                380, screenHeight - 90, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_global.png"),
                36, 22, button -> share("g")));

        addDrawableChild(shareLocal = new TexturedButtonWidget(
                380, screenHeight - 78, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_local.png"),
                36, 22, button -> share("l")));

        addDrawableChild(shareWorld = new TexturedButtonWidget(
                380, screenHeight - 66, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_world.png"),
                36, 22, button -> share("wc")));

        addDrawableChild(shareLFG = new TexturedButtonWidget(
                380, screenHeight - 54, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_lfg.png"),
                36, 22, button -> share("lfg")));

        addDrawableChild(shareReply = new TexturedButtonWidget(
                380, screenHeight - 42, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_reply.png"),
                36, 22, button -> share("r")));

        addDrawableChild(shareCost = new TexturedButtonWidget(
                340, screenHeight - 78, 39, 11,
                0, 0, 11, new Identifier("handbook", "textures/trade_cost.png"),
                39, 22, button -> selectMode(TradesWidget.Mode.COST)));

        addDrawableChild(shareTrader = new TexturedButtonWidget(
                340, screenHeight - 66, 39, 11,
                0, 0, 11, new Identifier("handbook", "textures/trade_trader.png"),
                39, 22, button -> selectMode(TradesWidget.Mode.TRADER)));

        addDrawableChild(shareFull = new TexturedButtonWidget(
                340, screenHeight - 54, 39, 11,
                0, 0, 11, new Identifier("handbook", "textures/trade_full.png"),
                39, 22, button -> selectMode(TradesWidget.Mode.FULL)));

        addDrawableChild(shareCancel = new TexturedButtonWidget(
                342, screenHeight - 30, 36, 11,
                0, 0, 11, new Identifier("handbook", "textures/location_cancel.png"),
                36, 22, button -> cancelSharing()));
        worldButtonsState(false);
        tradeButtonsState(false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        MatrixStack matrices = context.getMatrices();
        RenderSystem.enableBlend();
        context.fill(0, 0, width, 15, 0, 548055807);
        matrices.push();
        matrices.scale(1.5f, 1.5f, 1);
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                (int) (width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1, -1, false);
        matrices.pop();

        matrices.push();
        matrices.scale(1.25f, 1.25f, 1);
        context.drawText(tr, Text.of("Trade Search").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                15, 3, -1, false);
        matrices.pop();

        context.fill(130, 15, 131, height - 10, 100, -1);
        context.fill(260, 29, 261, height - 10, 100, -1);

        context.drawText(tr, Text.of("Favourite"),  21, 20, -1, false);
        if (resultsWidget.children().isEmpty())
            context.drawText(tr, Text.of("Nothing found :("),
                    197 - tr.getWidth("Nothing found :(") / 2, 35, -1, false);

        if (trader != null) {
            matrices.push();
            matrices.translate(260, 35, 1);

            matrices.push();
            matrices.scale(1.75f, 1.75f, 1);
            context.drawText(tr, trader.getTitle(), 5, 0, 16777215, true);
            matrices.pop();

            context.drawText(tr, "Shard: " + (trader.getShard() != null ? trader.getShard() : "unknown"),
                    10, 20, 16777215, false);

            int[] coords = trader.getPosition();
            context.drawText(tr, "Position: " + (coords != null ? coords[0] + ", " + coords[1] + ", " + coords[2] : "unknown"),
                    10, 30, 16777215, false);
            matrices.pop();
        }

        super.render(context, mouseX, mouseY, delta);
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
        for (BaseCategory category : screen.categories) {
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
                null, "unknown", new int[]{0, 0, 0}, "kappa123");
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

        if (MinecraftClient.getInstance().currentScreen == null) return;
        MinecraftClient.getInstance().currentScreen.close();
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
            client.player.closeScreen();
            super.close();
        }
    }
}
