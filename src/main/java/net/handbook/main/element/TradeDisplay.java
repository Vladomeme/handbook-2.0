package net.handbook.main.element;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.util.math.MatrixStack;

public class TradeDisplay implements Drawable {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;

    public boolean visible = false;
    public int x;
    private final int y;

    private String name;

    public TradeDisplay(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 100);
        matrices.scale(1.75f, 1.75f, 1);

        context.drawText(tr, name, 5, 0, 16777215, true);

        matrices.pop();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setX(int x) {
        this.x = x;
    }
}
