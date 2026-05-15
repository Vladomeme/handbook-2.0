package net.handbook.main.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.mixin.ScreenAccessor;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.element.TextButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class LocationScreen extends Screen {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final ChatHud chat = client.inGameHud.getChatHud();
    private final TextRenderer tr = client.textRenderer;

    private static TextFieldWidget textField;

    private int lastKey = 0;
    final int centerX;
    final int centerY;

    public LocationScreen(Text title) {
        super(title);
        centerX = client.getWindow().getScaledWidth() / 2;
        centerY = client.getWindow().getScaledHeight() / 2;
    }

    @Override
    protected void init() {
        if (LocationWriter.writer != null) {
            addElements();
            textField.setFocused(true);
            super.init();
        }
        else {
            close();
            chat.addMessage(Text.of("\"Locations\" category not found!"));
        }
    }

    private void addElements() {
        addDrawableChild(textField = new TextFieldWidget(tr, centerX - 65, centerY - 15, 130, 12, Text.of("")));
        textField.setPlaceholder(Text.of("Name").getWithStyle(Style.EMPTY.withItalic(true).withColor(-10197916)).getFirst());

        addDrawableChild(new TextButton(centerX + 30, centerY + 6, 36, 11,
                "Save", button -> save()));
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
        context.fill(centerX - 75, centerY - 50, centerX + 75, centerY + 25, 0, HandbookConfig.INSTANCE.tradeBackgroundColor);
        context.drawBorder(centerX - 76, centerY - 51, 152, 77, HandbookConfig.INSTANCE.bordersColor);
        context.drawCenteredTextWithShadow(tr, "Add location", centerX, centerY - 47, HandbookConfig.INSTANCE.textColor);
        context.drawCenteredTextWithShadow(tr,
                (int) client.player.getX() + ", " + (int) client.player.getY() + ", " + (int) client.player.getZ(),
                centerX, centerY - 30, HandbookConfig.INSTANCE.textColor);

        for (Drawable drawable : ((ScreenAccessor) this).drawables())
            drawable.render(context, mouseX, mouseY, delta);
        matrices.pop();
        RenderSystem.disableBlend();
    }

    private void save() {
        if (textField.getText().isEmpty()) {
            chat.addMessage(Text.of("Can't add a location without a name."));
            close();
            return;
        }
        for (Entry entry : LocationWriter.writer.entries()) {
            if (entry.title().equals(textField.getText())) {
                chat.addMessage(Text.of("Entry with that name already exists."));
                close();
                return;
            }
        }
        LocationWriter.addLocation(textField.getText());
        close();
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
