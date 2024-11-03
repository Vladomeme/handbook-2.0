package net.handbook.main.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.*;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public class HandbookConfig {

    private static final File FILE = new File(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook").toFile(), "config.json");

    public static final HandbookConfig INSTANCE = read();

    public static HandbookConfig read() {
        if (!FILE.exists())
            return new HandbookConfig().write();

        Reader reader = null;
        try {
            return new Gson().fromJson(reader = new FileReader(FILE), HandbookConfig.class);
        }
        catch (Exception e) {
            HandbookClient.LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
        finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public HandbookConfig write() {
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            if (!FILE.exists()) Files.createDirectories(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook"));
            writer = gson.newJsonWriter(new FileWriter(FILE));
            writer.setIndent("    ");
            gson.toJson(gson.toJsonTree(this, HandbookConfig.class), writer);
        }
        catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save config");
            HandbookClient.LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
        finally {
            IOUtils.closeQuietly(writer);
        }
        return this;
    }

    //GENERAL
    public boolean enabled = true;
    public boolean enableScanner = true;
    public boolean editorMode = false;
    public boolean quickSaveTrades = false;
    public boolean autoClose = true;
    public boolean alwaysContinue = false;
    public boolean editMessages = true;

    //VISUAL
    public boolean monuParticles = false;
    public boolean renderBeacon = true;
    //COLORS
    public int beaconColor = 3847130;
    public int screenHeadColor = 548055807;
    public int bordersColor = -1;
    public int textColor = -1;
    public int buttonInactiveColor = -2236963;
    public int buttonNegativeColor = -1823700;
    public int buttonPositiveColor = -6226016;
    public int buttonActiveColor = -9737764;
    public int tradeBackgroundColor = 866822826;
    public int favouriteColor = 2030023680;
    public int highlightColor = 1688906410;
    public int highlightFavColor = 1358935040;

    public Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .save(this::write)
                .title(Text.literal("Handbook 2.0."))

                //GENERAL
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("General"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Enabled"))
                                .binding(true, () -> enabled, newVal -> enabled = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Enable scanner"))
                                .description(OptionDescription.of(Text.literal("""
                                If enabled, checks all nearby NPCs and adds them to the
                                handbook. Also records villager trades whenever you open them.""")))
                                .binding(true, () -> enableScanner, newVal -> enableScanner = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Editor mode"))
                                .description(OptionDescription.of(Text.literal("""
                                If enabled, gives you access to a bunch of options to add,
                                edit and remove entries.""")))
                                .binding(false, () -> editorMode, newVal -> editorMode = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Quick-save trades"))
                                .description(OptionDescription.of(Text.literal("""
                                If enabled, NPC trading screen will be instantly closed, but
                                trades will be saved with a notification.""")))
                                .binding(false, () -> quickSaveTrades, newVal -> quickSaveTrades = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Close after adding entries"))
                                .description(OptionDescription.of(Text.literal("""
                                If enabled, entry editing/adding screen will be auto closed
                                if action was successful.""")))
                                .binding(true, () -> autoClose, newVal -> autoClose = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Edit chat messages"))
                                .description(OptionDescription.of(Text.literal("""
                                If enabled, will add a waypoint ClickEvent to messages with
                                correctly formatted coordinates.""")))
                                .binding(true, () -> editMessages, newVal -> editMessages = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Auto-continue paths"))
                                .description(OptionDescription.of(Text.literal("""
                                If enabled, will not pause a waypoint path when an action
                                 is required before continuing.""")))
                                .binding(false, () -> alwaysContinue, newVal -> alwaysContinue = newVal)
                                .controller(TickBoxControllerBuilder::create).build())
                        .build())

                //VISUAL
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Visual"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Render beacon"))
                                .description(OptionDescription.of(Text.literal(
                                        "If enabled, a beacon will be rendered at waypoint position.")))
                                .binding(true, () -> renderBeacon, newVal -> renderBeacon = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Use monumenta particles"))
                                .description(OptionDescription.of(Text.literal("""
                                If enabled, waypoint display will use same particles as the
                                monumenta quest compass (green particles).""")))
                                .binding(false, () -> monuParticles, newVal -> monuParticles = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Beacon color"))
                                .binding(new Color(3847130),
                                        () -> new Color(beaconColor), newVal -> beaconColor = newVal.getRGB())
                                .controller(ColorControllerBuilder::create).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Screen head color"))
                                .binding(new Color(548055807, true),
                                        () -> new Color(screenHeadColor, true), newVal -> screenHeadColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Borders color"))
                                .binding(new Color(-1, true),
                                        () -> new Color(bordersColor, true), newVal -> bordersColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Text color"))
                                .binding(new Color(-1, true),
                                        () -> new Color(textColor, true), newVal -> textColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Inactive button color"))
                                .binding(new Color(-2236963, true),
                                        () -> new Color(buttonInactiveColor, true), newVal -> buttonInactiveColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Negative button color"))
                                .binding(new Color(-1823700, true),
                                        () -> new Color(buttonNegativeColor, true), newVal -> buttonNegativeColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Positive button color"))
                                .binding(new Color(-6226016, true),
                                        () -> new Color(buttonPositiveColor, true), newVal -> buttonPositiveColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Active button color"))
                                .binding(new Color(-9737764, true),
                                        () -> new Color(buttonActiveColor, true), newVal -> buttonActiveColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Trade entry background color"))
                                .binding(new Color(866822826, true),
                                        () -> new Color(tradeBackgroundColor, true), newVal -> tradeBackgroundColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Favourite entry color"))
                                .binding(new Color(2030023680, true),
                                        () -> new Color(favouriteColor, true), newVal -> favouriteColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Highlighted entry color"))
                                .binding(new Color(1688906410, true),
                                        () -> new Color(highlightColor, true), newVal -> highlightColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Highlighted favourite entry color"))
                                .binding(new Color(1358935040, true),
                                        () -> new Color(highlightFavColor, true), newVal -> highlightFavColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .build())
                .build()
                .generateScreen(parent);
    }
}
