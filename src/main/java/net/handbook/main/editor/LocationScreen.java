package net.handbook.main.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.category.BaseCategory;
import net.handbook.main.resources.entry.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class LocationScreen extends Screen {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final TextRenderer tr = client.textRenderer;

    private static TextFieldWidget textField;
    @SuppressWarnings("FieldCanBeLocal")
    private static TexturedButtonWidget submitButton;

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
        for (BaseCategory category : HandbookScreen.categories) {
            if (category.getTitle().equals("Locations")) {
                addDrawableChild(textField = new TextFieldWidget(tr, centerX - 65, centerY - 15, 130, 12, Text.of("")));
                textField.setPlaceholder(Text.of("Name").getWithStyle(Style.EMPTY.withItalic(true).withColor(-10197916)).get(0));

                addDrawableChild(submitButton = new TexturedButtonWidget(centerX + 30, centerY + 6, 36, 11,
                    0, 0, 11, new Identifier("handbook", "textures/save.png"),
                    36, 22, button -> save()));

                super.init();
                return;
            }
        }
        close();
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\"Locations\" category not found!"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client.player == null) return;
        renderBackground(context);

        RenderSystem.enableBlend();
        context.fill(0, 0, width, 15, 0, 548055807);
        context.getMatrices().push();
        context.getMatrices().scale(1.5f, 1.5f, 1);
        context.drawText(tr, Text.of("Handbook 2.0").getWithStyle(Style.EMPTY.withItalic(true)).get(0),
                (int) (width / 1.5 - tr.getWidth("Handbook 2.0") * 1.5), 1, -1, false);
        context.getMatrices().pop();

        context.fill(centerX - 75, centerY - 50, centerX + 75, centerY + 25, 1087624147);
        context.drawBorder(centerX - 76, centerY - 51, 152, 77, -2894893);
        context.drawCenteredTextWithShadow(tr, "Add location", centerX, centerY - 47, -2894893);
        context.drawCenteredTextWithShadow(tr,
                (int) client.player.getX() + ", " + (int) client.player.getY() + ", " + (int) client.player.getZ(),
                centerX, centerY - 30, -2894893);

        super.render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
    }

    private void save() {
        if (textField.getText().isEmpty()) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Can't add a location without a name."));
            close();
            return;
        }
        for (BaseCategory category : HandbookScreen.categories) {
            if (!category.getTitle().equals("Locations")) continue;
            for (Entry entry : category.getEntries()) {
                if (entry.getTitle().equals(textField.getText())) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Entry with that name already exists."));
                    close();
                    return;
                }
            }
            HandbookClient.locationWriter.addLocation(textField.getText());
            close();
        }
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
