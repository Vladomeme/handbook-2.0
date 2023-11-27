package net.handbook.main.widget;

import net.handbook.main.resources.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.StringTokenizer;

public class DisplayWidget extends ClickableWidget {

    private Entry entry;
    private String[] description = new String[]{};
    private final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

    public DisplayWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
        if (entry != null) description = splitText(entry.getText());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (entry == null) return;

        context.getMatrices().translate(getX(), getY(), 1);

        context.getMatrices().push();
        context.getMatrices().scale(1.75f, 1.75f, 1);
        context.drawText(tr, entry.getTitle(), 5, 0, 16777215, true);
        context.getMatrices().pop();

        for (int i = 0; i < description.length ; i++) {
            context.drawText(tr, description[i], 10, 20 + i * 10, 16777215, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    public String[] splitText(String text) {
        StringTokenizer tokens = new StringTokenizer(text, " ");
        StringBuilder output = new StringBuilder();
        int lineLength = 0;
        int maxLength = (this.width - 50) / 6;
        while (tokens.hasMoreTokens()) {
            String word = tokens.nextToken();

            while (word.length() > maxLength) {
                output.append(word, 0, maxLength - lineLength).append("\n");
                word = word.substring(maxLength - lineLength);
                lineLength = 0;
            }

            if (lineLength + word.length() > maxLength) {
                output.append("\n");
                lineLength = 0;
            }
            output.append(word).append(" ");

            lineLength += word.length() + 1;
        }

        return output.toString().split("\n");
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
