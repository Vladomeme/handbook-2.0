package net.handbook.main.element;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.resources.entry.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.widget.EmptyWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

public class EntryDisplay extends EmptyWidget implements Drawable {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;
    private static final String PATH = FabricLoader.getInstance().getConfigDir().toString() + "/handbook/textures/";

    public boolean visible = false;
    private int width;
    private final int height;

    private Entry entry;
    private String[] description = new String[]{};

    private Identifier imageID;
    private int imageWidth;
    private int imageHeight;
    public boolean renderImage = false;
    public boolean invalidImage = false;

    public EntryDisplay(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.width = width;
        this.height = height;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
        if (entry == null) return;

        description = splitText(entry.text());
        updateImage();
    }

    private void updateImage() {
        if (imageID != null) {
            client.getTextureManager().destroyTexture(imageID);
            imageID = null;
        }
        if (entry.hasImage()) {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                BufferedImage image = ImageIO.read(new File(PATH + entry.image() + ".png"));
                ImageIO.write(image, "png", os);

                imageID = client.getTextureManager().registerDynamicTexture("handbook_images",
                        new NativeImageBackedTexture(NativeImage.read(new ByteArrayInputStream(os.toByteArray()))));
                imageWidth = image.getWidth();
                imageHeight = image.getHeight();
                renderImage = true;
                invalidImage = false;
            }
            catch (IOException e) {
                HandbookClient.LOGGER.error("[Handbook 2.0] Invalid image name in entry {}", entry.title());
                renderImage = false;
                invalidImage = true;
            }
        }
        else renderImage = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (entry == null || !visible) return;

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(getX(), getY(), 1);

        matrices.push();
        matrices.scale(1.75f, 1.75f, 1);
        context.drawText(tr, entry.title(), 5, 0, 16777215, true);
        matrices.pop();

        int y = 20;
        if (entry.shard() != null) {
            context.drawText(tr, "Shard: " + entry.shard(), 10, y, HandbookConfig.INSTANCE.textColor, false);
            y += 10;
        }
        int[] coords = entry.position();
        if (coords != null) {
            context.drawText(tr, "Position: " + coords[0] + ", " + coords[1] + ", " + coords[2], 10, y,
                    HandbookConfig.INSTANCE.textColor, false);
            y += 10;
        }
        y += 3;

        for (int i = 0; i < description.length ; i++) {
            context.drawText(tr, description[i], 10, y + i * 10, HandbookConfig.INSTANCE.textColor, false);
        }

        if (renderImage) {
            if (invalidImage) {
                context.drawText(tr, "Invalid image", (int) (width * 0.5), 10, HandbookConfig.INSTANCE.textColor, false);
                return;
            }
            float scale = 1;
            if (imageWidth > width * 0.5 - 10) {
                if (imageHeight > height * 0.8) {
                    scale = (float) Math.min((width * 0.5 - 10) / imageWidth, (imageHeight * 0.8) / imageHeight);
                }
                else scale = (float) (width * 0.5 / imageWidth);
            }
            matrices.push();
            matrices.scale(scale, scale, 2);
            RenderSystem.enableBlend();
            context.drawTexture(imageID, (int) ((width * 0.5) / scale), (int) (10 / scale), 0, 0,
                    imageWidth, imageHeight, imageWidth, imageHeight);
            RenderSystem.disableBlend();
            matrices.pop();
        }
        matrices.pop();
    }

    private String[] splitText(String text) {
        StringTokenizer tokens = new StringTokenizer(text, " ");
        StringBuilder output = new StringBuilder();
        int lineLength = 0;
        int maxLength = (width / 10);
        if (maxLength <= 0) maxLength = 100;
        while (tokens.hasMoreTokens()) {
            String word = tokens.nextToken();

            if (word.contains("\n")) {
                output.append(word, 0, word.indexOf("\n") + 1);
                word = word.substring(word.indexOf("\n") + 1);
                lineLength = 0;
            }

            while (word.length() > maxLength) {
                if (maxLength - lineLength < 0) break;
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

    public Entry entry() {
        return entry;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
