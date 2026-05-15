package net.handbook.main.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.DataManager;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.EditScreen;
import net.handbook.main.mixin.ScreenAccessor;
import net.handbook.main.resources.EntryType;
import net.handbook.main.resources.ListType;
import net.handbook.main.resources.ScreenWithCategoryList;
import net.handbook.main.resources.ScreenWithFilters;
import net.handbook.main.resources.entry.Category;
import net.handbook.main.resources.entry.Entry;
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
import net.minecraft.util.Pair;

import java.util.Comparator;

public class MapScreen extends Screen implements ScreenWithCategoryList, ScreenWithFilters {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final TextRenderer tr = client.textRenderer;

    //First column
    public ListWidget categoriesWidget;
    @SuppressWarnings("unused")
    public TextButton openTradesScreen;
    @SuppressWarnings("unused")
    public TextButton openMainScreen;
    public TextButton addCategory;
    //Second column
    public ListWidget optionsWidget;
    public TextFieldWidget searchBox;
    public TextButton addEntry;
    //Map area
    public MapWidget mapWidget;
    //Other
    public TextButton clearWaypoint;
    public TextButton continueWaypoint;

    public Category<? extends Entry> activeCategory;
    public int line1x;
    public int line2x;
    private int lastKey;
    private String lastFilter = "";

    public MapScreen() {
        super(Text.empty());
    }

    @Override
    protected void init() {
        if (DataManager.getCategories().isEmpty()) {
            close();
            client.inGameHud.getChatHud().addMessage(Text.of("Handbook 2.0 data is missing! Unlucky!"));
            return;
        }
        activeCategory = DataManager.getCategories().stream().filter(this::filterCategoryType).findFirst().orElseThrow();
        addElements();
        super.init();
    }

    private void addElements() {
        int screenHeight = client.getWindow().getScaledHeight();

        addDrawableChild(clearWaypoint = new TextButton(20, 2, 76, 11,
                "Clear waypoint", button -> WaypointManager.setState(false)));
        clearWaypoint.active = WaypointManager.isActive();
        clearWaypoint.visible = WaypointManager.isActive();

        addDrawableChild(continueWaypoint = new TextButton(99, 2, 45, 11,
                "Continue", button -> WaypointManager.continueOrSkip()));
        continueWaypoint.active = WaypointManager.isActive();
        continueWaypoint.visible = WaypointManager.isActive();

        int maxWidth = 0;

        for (Category<? extends Entry> category : DataManager.getCategories()) {
            if (!filterCategoryType(category)) continue;
            int width = tr.getWidth(category.title());
            if (width > maxWidth) maxWidth = width;
        }

        maxWidth += 20;
        line1x = maxWidth + 20;

        addDrawableChild(categoriesWidget = new ListWidget(
                maxWidth + 3, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 90 : 70), 30));
        categoriesWidget.setX(20);
        categoriesWidget.setEntries(DataManager.getCategories().stream().filter(this::filterCategoryType).toList(), ListType.Category, true);
        categoriesWidget.updateHighlight(categoriesWidget.children().getFirst(), true);
        activeCategory = (Category<? extends Entry>) categoriesWidget.children().getFirst().entry;

        maxWidth = 0;

        addDrawableChild(addCategory = new TextButton(TextButton.Type.Positive, line1x / 2 - 20, screenHeight - 45, 40, 11,
                "Add", button -> EditScreen.open(null, EntryType.normal, null)));
        addCategory.visible = HandbookConfig.INSTANCE.editorMode;
        addCategory.active = HandbookConfig.INSTANCE.editorMode;

        addDrawableChild(openMainScreen = new TextButton(line1x / 2 - 37, screenHeight - 30, 75, 11,
                "Handbook", button -> HandbookClient.openHandbookScreen()));

        addDrawableChild(openTradesScreen = new TextButton(line1x / 2 - 37, screenHeight - 18, 75, 11,
                "Trades", button -> HandbookClient.openTradeScreen()));

        if (((Category<? extends Entry>) categoriesWidget.children().getFirst().entry).entries().isEmpty()) maxWidth = 110;
        else {
            for (Entry entry : ((Category<? extends Entry>) categoriesWidget.children().getFirst().entry).entries()) {
                if (!filterEntryShard(entry)) continue;
                int width = tr.getWidth(entry.title());
                if (width > maxWidth) maxWidth = width;
            }
            maxWidth = Math.min(maxWidth, 150) + 10;
        }

        addDrawableChild(optionsWidget = new ListWidget(
                maxWidth + 12, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 80 : 60), 30));
        optionsWidget.setX(30 + categoriesWidget.listWidth);
        optionsWidget.setEntries(((Category<? extends Entry>) categoriesWidget.children().getFirst().entry).entries()
                .stream().filter(this::filterEntryShard).toList(), ListType.Entry, true);
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;

        maxWidth = width - 40 - categoriesWidget.listWidth - optionsWidget.listWidth;

        addDrawableChild(addEntry = new TextButton(TextButton.Type.Positive, line1x + (line2x - line1x) / 2 - 20, screenHeight - 45, 40, 11,
                "Add", button -> EditScreen.open(null, activeCategory.type(), activeCategory)));
        addEntry.visible = HandbookConfig.INSTANCE.editorMode;
        addEntry.active = HandbookConfig.INSTANCE.editorMode;

        addDrawableChild(searchBox = new TextFieldWidget(
                tr, line1x, 15, line2x - line1x, 14, Text.of("")));
        searchBox.setPlaceholder(Text.of("Search...").getWithStyle(Style.EMPTY.withItalic(true).withColor(-10197916)).getFirst());

        addDrawableChild(mapWidget = new MapWidget(40 + categoriesWidget.listWidth + optionsWidget.listWidth, 20, maxWidth, screenHeight - 40));
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
                    35, HandbookConfig.INSTANCE.textColor, false);

        context.fill(line1x, 15, line1x + 1, height - 10, 100, HandbookConfig.INSTANCE.bordersColor);
        context.fill(line2x, 15, line2x + 1, height - 10, 100, HandbookConfig.INSTANCE.bordersColor);

        for (Drawable drawable : ((ScreenAccessor) this).drawables())
            drawable.render(context, mouseX, mouseY, delta);
        matrices.pop();
        RenderSystem.disableBlend();
    }

    public void setEntries(Category<? extends Entry> category) {
        int screenHeight = client.getWindow().getScaledHeight();
        int screenWidth = client.getWindow().getScaledWidth();
        int maxWidth = 0;

        if (category.entries().isEmpty()) maxWidth = 110;
        else {
            for (Entry entry : category.entries()) {
                if (!filterEntryShard(entry)) continue;
                int width = tr.getWidth(entry.title());
                if (width > maxWidth) maxWidth = width;

                //todo generate icon identifier map here, cast to PositionEntry to grab icon paths -> save into a String set
            }
            maxWidth = Math.max(Math.min(maxWidth, 150), 100) + 10;
        }

        optionsWidget.setDimensions(maxWidth + 10, screenHeight - (HandbookConfig.INSTANCE.editorMode ? 80 : 60));
        optionsWidget.setPosition(30 + categoriesWidget.listWidth, 30);
        optionsWidget.listWidth = maxWidth + 10;
        optionsWidget.setEntries(category.entries().stream().filter(this::filterEntryShard).toList(), ListType.Entry, true);
        line2x = 29 + categoriesWidget.listWidth + optionsWidget.listWidth;
        addEntry.setX(line1x + (line2x - line1x) / 2 - 20);
        searchBox.setWidth(line2x - line1x);

        maxWidth = screenWidth - 30 - categoriesWidget.listWidth - optionsWidget.listWidth;

        mapWidget.setWidth(maxWidth);
        mapWidget.setX(30 + categoriesWidget.listWidth + optionsWidget.listWidth);

        activeCategory = category;
        searchBox.setText("");
    }

    public void filterEntries(boolean scheduled) {
        if (!searchBox.getText().equals(lastFilter) || !scheduled) {
            if (searchBox.getText().isEmpty()) {
                optionsWidget.setEntries(activeCategory.entries(), ListType.Entry, true);
                lastFilter = "";
                return;
            }
            String s = searchBox.getText().toLowerCase();
            optionsWidget.setEntries(activeCategory.entries().stream()
                    .map(entry -> new Pair<Entry, Integer>(entry, entry.clearTitle().toLowerCase().indexOf(s)))
                    .filter(pair -> filterEntryShard(pair.getLeft()) && pair.getRight() >= 0)
                    .sorted(Comparator.comparingInt(Pair::getRight))
                    .map(Pair::getLeft)
                    .toList(), ListType.Entry, false);
        }
        lastFilter = searchBox.getText();
    }

    private boolean filterCategoryType(Category<?> category) {
        EntryType type = category.type();
        return type.equals(EntryType.position) || type.equals(EntryType.area) || type.equals(EntryType.trader);
    }

    private boolean filterEntryShard(Entry entry) {
        return entry.shard().equals(WaypointManager.getShard());
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
        mapWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
        filterEntries(true);
    }

    @Override
    public Category<? extends Entry> activeCategory() {
        return activeCategory;
    }
}
