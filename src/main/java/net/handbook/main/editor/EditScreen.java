package net.handbook.main.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.DataManager;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.mixin.ScreenAccessor;
import net.handbook.main.resources.EntryType;
import net.handbook.main.resources.entry.*;
import net.handbook.main.element.TextButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;

//todo prevent changing of internally used categories?
public class EditScreen extends Screen {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;

    private final BaseEntry entry;
    private final boolean isCategory;
    private final EntryType type;
    private final Category<?> parentCategory;

    private TextFieldWidget typeField;
    private TextFieldWidget titleField;
    private TextFieldWidget textField;
    private TextFieldWidget positionField;
    private TextFieldWidget areaField;
    private TextFieldWidget iconField;
    private TextButton moveButton;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private TextButton cancelButton;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private TextButton saveButton;

    private int lastKey = 0;
    final int centerX;
    final int centerY;

    private EditScreen(BaseEntry entry, EntryType type, Category<?> parentCategory) {
        super(Text.of(""));

        this.entry = entry;
        this.isCategory = parentCategory == null;
        this.type = type;
        this.parentCategory = parentCategory;
        this.centerX = client.getWindow().getScaledWidth() / 2;
        this.centerY = client.getWindow().getScaledHeight() / 2;
    }

    public static void open(BaseEntry entry, EntryType type, Category<?> parentCategory) {
        if (type.equals(EntryType.waypoint)) {
            client.inGameHud.getChatHud().addMessage(Text.of("Can't add or edit an entry of this type."));
            return;
        }
        if (type.equals(EntryType.trader) && entry == null) {
            client.inGameHud.getChatHud().addMessage(Text.of("Can't add an entry of this type."));
            return;
        }
        client.setScreen(new EditScreen(entry, type, parentCategory));
    }

    @Override
    protected void init() {
        addElements();
        super.init();
    }

    private void addElements() {
        Style style = Style.EMPTY.withItalic(true).withColor(-10197916);

        addDrawableChild(typeField = new TextFieldWidget(tr, centerX - 80, centerY - 84, 160, 12, Text.of("")));
        typeField.setPlaceholder(Text.of("type").getWithStyle(style).getFirst());
        typeField.setChangedListener(this::updateTypeColour);
        typeField.setMaxLength(9999);

        addDrawableChild(titleField = new TextFieldWidget(tr, centerX - 80, centerY - 68, 160, 12, Text.of("")));
        titleField.setPlaceholder(Text.of("name").getWithStyle(style).getFirst());
        titleField.setMaxLength(9999);

        addDrawableChild(textField = new TextFieldWidget(tr, centerX - 80, centerY - 52, 160, 12, Text.of("")));
        textField.setPlaceholder(Text.of("text").getWithStyle(style).getFirst());
        textField.setMaxLength(9999);

        addDrawableChild(positionField = new TextFieldWidget(tr, centerX - 80, centerY - 36, 160, 12, Text.of("")));
        positionField.setPlaceholder(Text.of("position").getWithStyle(style).getFirst());
        positionField.setChangedListener(text -> checkCoordinates(text, 3));
        positionField.setMaxLength(9999);

        addDrawableChild(areaField = new TextFieldWidget(tr, centerX - 80, centerY - 20, 160, 12, Text.of("")));
        areaField.setPlaceholder(Text.of("area").getWithStyle(style).getFirst());
        areaField.setChangedListener(text -> checkCoordinates(text, 6));
        areaField.setMaxLength(9999);

        addDrawableChild(iconField = new TextFieldWidget(tr, centerX - 80, centerY - 4, 160, 12, Text.of("")));
        iconField.setPlaceholder(Text.of("icon").getWithStyle(style).getFirst());
        iconField.setChangedListener(text -> checkIconPath());
        iconField.setMaxLength(9999);

        addDrawableChild(moveButton = new TextButton(centerX + 85, centerY - 36, 30, 11,
                "Here", button -> changePosition()));

        addDrawableChild(cancelButton = new TextButton(TextButton.Type.Negative, centerX - 60, centerY + 16, 36, 11,
                "Cancel", button -> close()));

        addDrawableChild(saveButton = new TextButton(centerX + 24, centerY + 16, 36, 11,
                "Save", button -> {
            if (isCategory) saveCategory();
            else saveEntry();
        }));

        setVisibility();
        if (entry == null) {
            if (type.equals(EntryType.position) || type.equals(EntryType.area)) changePosition();
            return;
        }

        Text text = Text.of("unavailable").getWithStyle(style).getFirst();
        typeField.active = false;
        typeField.setPlaceholder(text);

        fillFields();
    }

    private void setVisibility() {
        Style style = Style.EMPTY.withItalic(true).withColor(-10197916);
        Text text = Text.of("unavailable").getWithStyle(style).getFirst();

        if (isCategory) {
            textField.active = false;
            textField.setPlaceholder(text);
            positionField.active = false;
            positionField.setPlaceholder(text);
            areaField.active = false;
            areaField.setPlaceholder(text);
            iconField.active = false;
            iconField.setPlaceholder(text);
            moveButton.active = false;
            return;
        }
        typeField.active = false;
        typeField.setPlaceholder(text);
        switch (type) {
            case normal, trader -> {
                positionField.active = false;
                positionField.setPlaceholder(text);
                areaField.active = false;
                areaField.setPlaceholder(text);
                moveButton.active = false;
            }
            case position -> {
                areaField.active = false;
                areaField.setPlaceholder(text);
            }
            case area -> {}
        }
        if (type.equals(EntryType.normal)) {
            iconField.active = false;
            iconField.setPlaceholder(text);
        }
    }

    private void fillFields() {
        if (entry.title() != null) titleField.setText(entry.title());

        if (isCategory) return;

        if (entry.text() != null) textField.setText(entry.text());
        switch (type) {
            case position -> {
                int[] pos = ((Entry) entry).position();
                if (pos != null) positionField.setText(pos[0] + ", " + pos[1] + ", " + pos[2]);
            }
            case area -> {
                int[] pos = ((Entry) entry).position();
                if (pos != null) positionField.setText(pos[0] + ", " + pos[1] + ", " + pos[2]);
                pos = ((Entry) entry).area();
                if (pos != null) areaField.setText(pos[0] + ", " + pos[1] + ", " + pos[2] + ", " + pos[3] + ", " + pos[4] + ", " + pos[5]);
            }
        }
        if (iconField.active) {
            String icon = ((Entry) entry).icon();
            if (icon != null && !icon.isEmpty()) iconField.setText(icon);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client.player == null) return;
        renderBackground(context, mouseX, mouseY, delta);

        context.fill(0, 0, width, 15, 1, HandbookConfig.INSTANCE.screenHeadColor);
        MatrixStack matrices = context.getMatrices();
        RenderSystem.enableBlend();
        matrices.push();
        matrices.scale(1.5f, 1.5f, 1);
        matrices.translate(0, 0, 1);
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).getFirst(),
                (int) (width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1, HandbookConfig.INSTANCE.textColor, false);
        matrices.pop();

        matrices.push();
        matrices.translate(0, 0, 1);
        context.fill(centerX - 130, centerY - 116, centerX + 130, centerY + 31, 0, HandbookConfig.INSTANCE.tradeBackgroundColor);
        context.drawBorder(centerX - 131, centerY - 117, 262, 149, HandbookConfig.INSTANCE.bordersColor);
        context.drawCenteredTextWithShadow(tr, entry == null ? "Adding entry..." : "Editing entry...",
                centerX, centerY - 109, HandbookConfig.INSTANCE.textColor);

        context.drawText(tr, Text.of("Type"), centerX - 85 - tr.getWidth("Type"), centerY - 82,
                HandbookConfig.INSTANCE.textColor, false);
        context.drawText(tr, Text.of("Name"), centerX - 85 - tr.getWidth("Name"), centerY - 66,
                HandbookConfig.INSTANCE.textColor, false);
        context.drawText(tr, Text.of("Text"), centerX - 85 - tr.getWidth("Text"), centerY - 50,
                HandbookConfig.INSTANCE.textColor, false);
        context.drawText(tr, Text.of("Position"), centerX - 85 - tr.getWidth("Position"), centerY - 34,
                HandbookConfig.INSTANCE.textColor, false);
        context.drawText(tr, Text.of("Area"), centerX - 85 - tr.getWidth("Area"), centerY - 18,
                HandbookConfig.INSTANCE.textColor, false);
        context.drawText(tr, Text.of("Icon"), centerX - 85 - tr.getWidth("Icon"), centerY - 2,
                HandbookConfig.INSTANCE.textColor, false);

        for (Drawable drawable : ((ScreenAccessor) this).drawables())
            drawable.render(context, mouseX, mouseY, delta);
        matrices.pop();
        RenderSystem.disableBlend();
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private void saveCategory() {
        if (titleField.getText().isEmpty()) return;
        if (entry == null) {
            EntryType type;
            try {
                type = EntryType.valueOf(typeField.getText());
            }
            catch (IllegalArgumentException e) {
                return;
            }
            String title = titleField.getText();
            DataManager.writers.add(
                    switch (type) {
                        case normal -> new CategoryWriter<>(new Category<>(type, title, false));
                        case position -> new CategoryWriter<PositionEntry>(new Category<>(type, title, false));
                        case area -> new CategoryWriter<AreaEntry>(new Category<>(type, title, false));
                        case trader -> new CategoryWriter<TraderEntry>(new Category<>(type, title, false));
                        default -> throw new IllegalStateException("Unexpected value: " + type);
                    });
        }
        else entry.update(titleField.getText(), entry.text());
        close();
    }

    private void saveEntry() {
        if (titleField.getText().isEmpty()) return;
        switch (entry) {
            case null -> { //new entry
                for (CategoryWriter<? extends Entry> writer : DataManager.writers) {
                    if (!writer.category.equals(parentCategory)) continue;

                    switch (type) {
                        case normal -> writer.add(new Entry(titleField.getText(), textField.getText(), ""));
                        case position -> {
                            int[] pos = checkCoordinates(positionField.getText(), 3);
                            if (pos != null) {
                                writer.add(new PositionEntry(titleField.getText(), textField.getText(),
                                            "", WaypointManager.getShard(), pos, iconField.getText().isEmpty() ? "default" : iconField.getText()));
                            } //todo default icon name?
                        }
                        case area -> {
                            int[] pos = checkCoordinates(positionField.getText(), 3);
                            int[] area = checkCoordinates(areaField.getText(), 6);
                            if (pos != null || area != null) {
                                writer.add(new AreaEntry(titleField.getText(), textField.getText(),
                                        "", WaypointManager.getShard(), pos, area, iconField.getText().isEmpty() ? "default" : iconField.getText()));
                            } //todo default icon name?
                        }
                    }
                    break;
                }
                client.inGameHud.getChatHud().addMessage(Text.of("Entry added."));
                if (HandbookConfig.INSTANCE.autoClose) close();
                return;
            }
            case AreaEntry e -> {
                int[] pos = positionField.active ? checkCoordinates(positionField.getText(), 3) : e.position();
                int[] area = areaField.active ? checkCoordinates(areaField.getText(), 6) : e.area();
                if (pos == null || area == null) return;

                e.update(titleField.getText(), textField.getText(), pos, iconField.getText(), area);
            }
            case PositionEntry e -> {
                int[] pos = positionField.active ? checkCoordinates(positionField.getText(), 3) : e.position();
                if (pos == null) return;

                e.update(titleField.getText(), textField.getText(), pos, iconField.getText());
            }
            case Entry e -> e.update(titleField.getText(), textField.getText());
            default -> {}
        }

        for (CategoryWriter<? extends Entry> writer : DataManager.writers) {
            if (!writer.category.equals(parentCategory)) continue;

            writer.setUpdate();
            break;
        }
        client.inGameHud.getChatHud().addMessage(Text.of("Entry updated."));
        if (HandbookConfig.INSTANCE.autoClose) close();
    }

    private void changePosition() {
        if (client.player == null) return;
        positionField.setText((int) client.player.getX() + ", " + (int) client.player.getY() + ", " + (int) client.player.getZ());
    }

    @SuppressWarnings("DataFlowIssue")
    private int[] checkCoordinates(String line, int length) {
        try {
            int[] pos = Arrays.stream(line.replace(" ", "").split(",")).mapToInt(Integer::parseInt).toArray();
            if (pos.length == length) {
                (length == 3 ? positionField : areaField).setEditableColor(Formatting.WHITE.getColorValue());
                return pos;
            }
        }
        catch (Exception ignored) {}
        (length == 3 ? positionField : areaField).setEditableColor(Formatting.RED.getColorValue());
        return null;
    }

    //todo checkIconPath()
    @SuppressWarnings("EmptyMethod")
    private void checkIconPath() {

    }

    @SuppressWarnings("DataFlowIssue")
    private void updateTypeColour(String type) {
        if (type.equals("normal") || type.equals("positioned") || type.equals("area") || type.equals("trader")) {
            typeField.setEditableColor(Formatting.WHITE.getColorValue());
        }
        typeField.setEditableColor(Formatting.RED.getColorValue());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        lastKey = keyCode;

        if (!super.keyPressed(keyCode, scanCode, modifiers)
                && client.options.inventoryKey.matchesKey(keyCode, scanCode)) close();
        return true;
    }

    @Override
    public void close() {
        if (lastKey != 69) {
            if (client.player != null) client.player.closeScreen();
            super.close();
        }
    }
}
