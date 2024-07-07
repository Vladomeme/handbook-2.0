package net.handbook.main.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.resources.entry.AreaEntry;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.PositionedEntry;
import net.handbook.main.resources.entry.TraderEntry;
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

import java.util.Arrays;

public class EditScreen extends Screen {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;

    private final Entry entry;
    private String oldTitle;
    private int[] oldPosition;

    private TextFieldWidget nameField;
    private TextFieldWidget textField;
    private TextFieldWidget positionField;
    private TextFieldWidget areaField;
    private HandbookButtonWidget moveButton;
    private HandbookButtonWidget cancelButton;
    private HandbookButtonWidget saveButton;

    private int lastKey = 0;
    final int centerX;
    final int centerY;

    public EditScreen(Entry entry) {
        super(Text.of(""));

        this.entry = entry;
        this.centerX = client.getWindow().getScaledWidth() / 2;
        this.centerY = client.getWindow().getScaledHeight() / 2;
    }

    @Override
    protected void init() {
        addElements();
        if (entry instanceof PositionedEntry) {
            oldTitle = entry.getTitle();
            oldPosition = entry.getPosition();
        }
        super.init();
    }

    private void addElements() {
        Style style = Style.EMPTY.withItalic(true).withColor(-10197916);

        addDrawableChild(nameField = new TextFieldWidget(tr, centerX - 80, centerY - 68, 160, 12, Text.of("")));
        nameField.setPlaceholder(Text.of("name").getWithStyle(style).get(0));
        nameField.setMaxLength(9999);

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
                centerX + 24, centerY, 36, 11, "Save", button -> save()));

        if (entry.getTitle() != null) nameField.setText(entry.getTitle());
        if (entry.getText() != null) textField.setText(entry.getText());

        if (entry instanceof PositionedEntry && !(entry instanceof TraderEntry)) {
            int[] pos = entry.getPosition();
            if (pos != null) positionField.setText(pos[0] + ", " + pos[1] + ", " + pos[2]);

            //pos = entry.getArea();
            //if (pos != null) positionField.setText(pos[0] + ", " + pos[1] + ", " + pos[2] + ", " + pos[3] + ", " + pos[4] + ", " + pos[5]);
        }
        else {
            positionField.active = false;
            positionField.setPlaceholder(Text.of("unavailable").getWithStyle(style).get(0));
            moveButton.active = false;
        }
        areaField.active = false;
        areaField.setPlaceholder(Text.of("unavailable").getWithStyle(style).get(0));
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

        context.fill(centerX - 130, centerY - 100, centerX + 130, centerY + 15, HandbookConfig.INSTANCE.tradeBackgroundColor);
        context.drawBorder(centerX - 131, centerY - 101, 262, 117, HandbookConfig.INSTANCE.bordersColor);
        context.drawCenteredTextWithShadow(tr, "Entry editing", centerX, centerY - 93, HandbookConfig.INSTANCE.textColor);

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

    private void save() {
        if (entry instanceof AreaEntry) {
            int[] pos = positionField.active ? checkCoordinates(positionField.getText(), 3) : entry.getPosition();
            int[] area = areaField.active ? checkCoordinates(areaField.getText(), 6) : entry.getArea();
            if (pos == null || area == null) return;

            ((AreaEntry) entry).update(nameField.getText(), textField.getText(), pos, area);
        }
        else if (entry instanceof PositionedEntry) {
            int[] pos = positionField.active ? checkCoordinates(positionField.getText(), 3) : entry.getPosition();
            if (pos == null) return;

            ((PositionedEntry) entry).update(nameField.getText(), textField.getText(), pos);
            if (entry instanceof TraderEntry) HandbookClient.npcWriter.editEntry((TraderEntry) entry);
            else HandbookClient.locationWriter.editEntry((PositionedEntry) entry, oldTitle, oldPosition);
        }
        close();
    }

    private void changePosition() {
        if (client.player == null) return;
        positionField.setText((int) client.player.getX() + ", " + (int) client.player.getY() + ", " + (int) client.player.getZ());
    }

    private int[] checkCoordinates(String line, int length) {
        try {
            int[] pos = Arrays.stream(line.replace(" ", "").split(",")).mapToInt(Integer::parseInt).toArray();
            if (pos.length == length) {
                positionField.setEditableColor(Formatting.WHITE.getColorValue());
                return pos;
            }
        }
        catch (Exception ignored) {
            //unlucky
        }
        positionField.setEditableColor(Formatting.RED.getColorValue());
        return null;
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
