package net.handbook.main.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.category.Category;
import net.handbook.main.resources.entry.*;
import net.handbook.main.widget.HandbookButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;

public class EditScreen extends Screen {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;

    private final BaseEntry entry;
    private final boolean isCategory;
    private final String type;

    private TextFieldWidget typeField;
    private TextFieldWidget titleField;
    private TextFieldWidget textField;
    private TextFieldWidget positionField;
    private TextFieldWidget areaField;
    private HandbookButtonWidget moveButton;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private HandbookButtonWidget cancelButton;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private HandbookButtonWidget saveButton;

    private int lastKey = 0;
    final int centerX;
    final int centerY;

    public EditScreen(BaseEntry entry, boolean isCategory, String type) {
        super(Text.of(""));

        this.entry = entry;
        this.isCategory = isCategory;
        this.type = type;
        this.centerX = client.getWindow().getScaledWidth() / 2;
        this.centerY = client.getWindow().getScaledHeight() / 2;
    }

    @Override
    protected void init() {
        if (type.equals("waypoint")) {
            client.inGameHud.getChatHud().addMessage(Text.of("Can't edit an entry of this type."));
            close();
        }
        if (type.equals("trader") && entry == null) {
            client.inGameHud.getChatHud().addMessage(Text.of("Can't add an entry of this type."));
            close();
        }
        addElements();
        super.init();
    }

    private void addElements() {
        Style style = Style.EMPTY.withItalic(true).withColor(-10197916);

        addDrawableChild(typeField = new TextFieldWidget(tr, centerX - 80, centerY - 84, 160, 12, Text.of("")));
        typeField.setPlaceholder(Text.of("name").getWithStyle(style).get(0));
        typeField.setMaxLength(9999);

        addDrawableChild(titleField = new TextFieldWidget(tr, centerX - 80, centerY - 68, 160, 12, Text.of("")));
        titleField.setPlaceholder(Text.of("name").getWithStyle(style).get(0));
        titleField.setMaxLength(9999);

        addDrawableChild(textField = new TextFieldWidget(tr, centerX - 80, centerY - 52, 160, 12, Text.of("")));
        textField.setPlaceholder(Text.of("text").getWithStyle(style).get(0));
        textField.setMaxLength(9999);

        addDrawableChild(positionField = new TextFieldWidget(tr, centerX - 80, centerY - 36, 160, 12, Text.of("")));
        positionField.setPlaceholder(Text.of("position").getWithStyle(style).get(0));
        positionField.setChangedListener(text -> checkCoordinates(text, 3));
        positionField.setMaxLength(9999);

        addDrawableChild(areaField = new TextFieldWidget(tr, centerX - 80, centerY - 20, 160, 12, Text.of("")));
        areaField.setPlaceholder(Text.of("area").getWithStyle(style).get(0));
        areaField.setChangedListener(text -> checkCoordinates(text, 6));
        areaField.setMaxLength(9999);

        addDrawableChild(moveButton = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                centerX + 85, centerY - 36, 30, 11, "Here", button -> changePosition()));

        addDrawableChild(cancelButton = new HandbookButtonWidget(HandbookButtonWidget.Type.Negative,
                centerX - 60, centerY, 36, 11, "Cancel", button -> close()));

        addDrawableChild(saveButton = new HandbookButtonWidget(HandbookButtonWidget.Type.Normal,
                centerX + 24, centerY, 36, 11, "Save", button -> {
            if (isCategory) saveCategory();
            else saveEntry();
        }));

        setVisibility();
        if (entry == null) {
            if (type.equals("positioned") || type.equals("area")) changePosition();
            return;
        }

        Text text = Text.of("unavailable").getWithStyle(style).get(0);
        typeField.active = false;
        typeField.setPlaceholder(text);

        fillFields();
    }

    private void setVisibility() {
        Style style = Style.EMPTY.withItalic(true).withColor(-10197916);
        Text text = Text.of("unavailable").getWithStyle(style).get(0);

        if (isCategory) {
            textField.active = false;
            textField.setPlaceholder(text);
            positionField.active = false;
            positionField.setPlaceholder(text);
            areaField.active = false;
            areaField.setPlaceholder(text);
            return;
        }
        typeField.active = false;
        typeField.setPlaceholder(text);
        switch (type) {
            case "normal, trader" -> {
                positionField.active = false;
                positionField.setPlaceholder(text);
                areaField.active = false;
                areaField.setPlaceholder(text);
                moveButton.active = false;
            }
            case "positioned" -> {
                areaField.active = false;
                areaField.setPlaceholder(text);
            }
            case "area" -> {}
        }
    }

    private void fillFields() {
        if (entry.getTitle() != null) titleField.setText(entry.getTitle());

        if (isCategory) return;

        if (entry.getText() != null) textField.setText(entry.getText());
        switch (type) {
            case "positioned" -> {
                int[] pos = ((Entry) entry).getPosition();
                if (pos != null) positionField.setText(pos[0] + ", " + pos[1] + ", " + pos[2]);
            }
            case "area" -> {
                int[] pos = ((Entry) entry).getPosition();
                if (pos != null) positionField.setText(pos[0] + ", " + pos[1] + ", " + pos[2]);
                pos = ((Entry) entry).getArea();
                if (pos != null) areaField.setText(pos[0] + ", " + pos[1] + ", " + pos[2] + ", " + pos[3] + ", " + pos[4] + ", " + pos[5]);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client.player == null) return;
        renderBackground(context);

        MatrixStack matrices = context.getMatrices();
        RenderSystem.enableBlend();
        context.fill(0, 0, width, 15, 0, HandbookConfig.INSTANCE.screenHeadColor);
        matrices.push();
        matrices.scale(1.5f, 1.5f, 1);
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                (int) (width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1, HandbookConfig.INSTANCE.textColor, false);
        matrices.pop();

        context.fill(centerX - 130, centerY - 116, centerX + 130, centerY + 15, HandbookConfig.INSTANCE.tradeBackgroundColor);
        context.drawBorder(centerX - 131, centerY - 117, 262, 117, HandbookConfig.INSTANCE.bordersColor);
        context.drawCenteredTextWithShadow(tr, "Entry editing", centerX, centerY - 109, HandbookConfig.INSTANCE.textColor);

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

        super.render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
    }

    private void saveCategory() {
        if (titleField.getText().isEmpty()) return;
        if (entry == null)
            HandbookClient.writers.add(new CategoryWriter(
                new Category(typeField.getText(), titleField.getText(), "", "", new ArrayList<>())));
        else entry.update(titleField.getText(), entry.getText());
    }

    private void saveEntry() {
        if (titleField.getText().isEmpty()) return;
        if (entry == null) {
            for (CategoryWriter writer : HandbookClient.writers) {
                if (!writer.category.equals(HandbookClient.handbookScreen.activeCategory)) continue;

                switch (type) {
                    case "normal" -> writer.add(new Entry(titleField.getText(), textField.getText(), ""));
                    case "positioned" -> {
                        int[] pos = checkCoordinates(positionField.getText(), 3);
                        if (pos != null) writer.add(new PositionedEntry(titleField.getText(), textField.getText(),
                                "", WaypointManager.getShard(), pos));
                    }
                    case "area" -> {
                        int[] pos = checkCoordinates(positionField.getText(), 3);
                        int[] area = checkCoordinates(areaField.getText(), 6);
                        if (pos != null || area != null)writer.add(new AreaEntry(titleField.getText(), textField.getText(),
                                "", WaypointManager.getShard(), pos, area));
                    }
                }
                writer.shouldUpdate = true;
                break;
            }
            return;
        }

        if (entry instanceof AreaEntry e) {
            int[] pos = positionField.active ? checkCoordinates(positionField.getText(), 3) : e.getPosition();
            int[] area = areaField.active ? checkCoordinates(areaField.getText(), 6) : e.getArea();
            if (pos == null || area == null) return;

            e.update(titleField.getText(), textField.getText(), pos, area);
        }
        else if (entry instanceof PositionedEntry e) {
            int[] pos = positionField.active ? checkCoordinates(positionField.getText(), 3) : e.getPosition();
            if (pos == null) return;

            e.update(titleField.getText(), textField.getText(), pos);
        }
        else if (entry instanceof Entry e) e.update(titleField.getText(), textField.getText());

        for (CategoryWriter writer : HandbookClient.writers) {
            if (!writer.category.equals(HandbookClient.handbookScreen.activeCategory)) continue;

            writer.shouldUpdate = true;
            break;
        }
        close();
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
                positionField.setEditableColor(Formatting.WHITE.getColorValue());
                return pos;
            }
        }
        catch (Exception ignored) {}
        positionField.setEditableColor(Formatting.RED.getColorValue());
        return null;
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
