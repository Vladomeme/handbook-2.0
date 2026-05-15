package net.handbook.main.element;

import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.DataManager;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.feature.MapScreen;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.entry.PositionEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapWidget extends ClickableWidget {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;
    private final HandbookConfig config = HandbookConfig.INSTANCE;
    private static final String PATH = FabricLoader.getInstance().getConfigDir().toString() + "/handbook/textures/";
    private final Identifier DEFAULT_ICON = Identifier.of("handbook", "icons/default");

    private Identifier mapID;
    private int mapWidth;
    private int mapHeight;
    private int mapXAbs;
    private int mapYAbs;

    int centerX;
    int centerY;
    float scale;
    float minScale;

    public MapWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty());
        loadMap();
    }

    private void loadMap() {
        if (mapID != null) {
            client.getTextureManager().destroyTexture(mapID);
            mapID = null;
        }
        String shard = WaypointManager.getShard();
        File mapFile = new File(PATH + shard + "_map.png");
        if (mapFile.exists()) {
            try {
                mapID = getDynamicImageIdentifier(shard + "_map");
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                BufferedImage image = ImageIO.read(mapFile);
                ImageIO.write(image, "png", os);

                mapID = client.getTextureManager().registerDynamicTexture("handbook_images",
                        new NativeImageBackedTexture(NativeImage.read(new ByteArrayInputStream(os.toByteArray()))));
                mapWidth = image.getWidth();
                mapHeight = image.getHeight();
                centerX = mapWidth / 2;
                centerY = mapHeight / 2;
                scale = Math.max((float) width / mapWidth, (float) height / mapHeight);
                HandbookClient.LOGGER.info("{} {} {}", width, height, scale);
                minScale = scale;

                DataManager.MapPosition pos = DataManager.getMapCoordinates(shard);
                mapXAbs = pos.x();
                mapYAbs = pos.z(); //z coordinate is saved as y because the map is displayed in 2D, and it just makes more sense in code
            }
            catch (IOException e) {
                HandbookClient.LOGGER.error("[Handbook 2.0] Failed to read a map image file {}", WaypointManager.getShard() + "_map.png");
            }
        }
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (mapID == null) {
            context.drawText(tr, "Map is unavailable for this shard :(", getX(), getY(), config.textColor, false);
            return;
        }
        HandbookClient.LOGGER.info("{} {} {}", centerX, centerY, scale);

        int textureWidth = (int) (getWidth() / scale);
        int textureHeight = (int) (getHeight() / scale);
        int u = centerX - textureWidth / 2;
        int v = centerY - textureHeight / 2;

        context.drawTexture(mapID, getX(), getY(), 100, u, v, getWidth(), getHeight(), textureWidth, textureHeight);

        renderMarkers(context, mouseX, mouseY);
    }

    //todo preset difficulty icons for PoI entries??
    @SuppressWarnings("EmptyMethod")
    public void renderMarkers(DrawContext context, int mouseX, int mouseY) {
        int centerXAbs = centerX + mapXAbs;
        int centerYAbs = centerY + mapYAbs;

        int widgetCenterX = getX() - getWidth() / 2;
        int widgetCenterY = getY() - getHeight() / 2;

        assert client.currentScreen != null;
        assert client.currentScreen instanceof MapScreen;

        boolean tooltipDisplayed = false;
        for (ListWidgetEntry widgetEntry : ((MapScreen) client.currentScreen).optionsWidget.children()) {
            PositionEntry marker = (PositionEntry) widgetEntry.entry;
            int[] position = marker.position();
            //entry's in-world position -> position relative to current map's center point -> position from the widget's center with applied scaled vector
            int markerX = (int) (widgetCenterX + (position[0] - centerXAbs) * scale);
            int markerY = (int) (widgetCenterY + (position[2] - centerYAbs) * scale);

            //out of bounds check
            if (markerX + 8 < getX() || markerX - 8 > getX() + getWidth() || markerY + 8 < getY() || markerY - 8 > getY() + getHeight()) continue;

            //Marker icon (15x16)
            //todo WTF do not generate a new dynamic identifier each frame
            Identifier icon = marker.icon() == null || !marker.icon().isEmpty() ? DEFAULT_ICON : getDynamicImageIdentifier(marker.icon());
            context.drawTexture(icon, markerX - 8, markerY - 16, 110, 0, 0, 16, 16, 16, 16);
            //Marker title & title background
            int halfTitleWidth = tr.getWidth(marker.clearTitle()) / 2;
            context.fill(markerX - halfTitleWidth - 1, markerY - 30, markerX + halfTitleWidth + 1, markerY - 18, 120,
                    widgetEntry.isHighlighted() ? config.highlightColor : config.tradeBackgroundColor);
            context.drawTextWithShadow(tr, marker.title(), markerX - halfTitleWidth, markerY - 29, config.textColor);

            //Mouse bound check & tooltip rendering
            if (!tooltipDisplayed && mouseX > markerX - 8 && mouseX < markerX + 8 && mouseY > markerY - 16 && mouseY < markerY) {
                renderTooltip(context, mouseX, mouseY, marker);
                tooltipDisplayed = true;
            }
        }
    }

    //todo add text formatting
    //todo click to open trade list for trader entries (tooltip shouldn't really be clickable, turn it into a separate widget?)
    private void renderTooltip(DrawContext context, int mouseX, int mouseY, PositionEntry marker) {
        List<Text> tooltip = new ArrayList<>();

        tooltip.add(Text.of(marker.title()));

        int[] position = marker.position();
        tooltip.add(Text.of("Position: " + position[0] + ", " + position[1] + ", " + position[2]));

        context.drawTooltip(tr, tooltip, mouseX, mouseY);
    }

    private Identifier getDynamicImageIdentifier(String id) {
        File file = new File(PATH, id + ".png");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedImage image;
        try {
            image = ImageIO.read(file);
        }
        catch (IOException e) {
            HandbookClient.LOGGER.error("Failed to read a texture file {}", file.getPath());
            HandbookClient.LOGGER.error(e.getMessage());
            return MissingSprite.getMissingSpriteId();
        }
        try {
            ImageIO.write(image, "png", os);
            return client.getTextureManager().registerDynamicTexture("handbook_images",
                    new NativeImageBackedTexture(NativeImage.read(new ByteArrayInputStream(os.toByteArray()))));
        }
        catch (IOException e) {
            HandbookClient.LOGGER.error("Failed to create a dynamic texture for a map widget: {}", file.getPath());
            return MissingSprite.getMissingSpriteId();
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clicked(mouseX, mouseY) && button == 0) {
            onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        centerX -= (int) (deltaX / scale);
        centerY -= (int) (deltaY / scale);

        int halfWidth = (int) (((float) mapWidth / 2) / scale);
        if (centerX > halfWidth) centerX = halfWidth;
        if (mapWidth - centerX < halfWidth) centerX = mapWidth - halfWidth;

        int halfHeight = (int) (((float) mapHeight / 2) / scale);
        if (centerX > halfHeight) centerY = halfHeight;
        if (mapHeight - centerY < halfHeight) centerY = mapHeight - halfHeight;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scale = Math.max(scale + (float) (verticalAmount * 0.1), minScale);
        return true;
    }
}
