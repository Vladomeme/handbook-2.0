package net.handbook.main.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HandbookConfigScreen {
        public static Screen create(Screen parent) {

                HandbookConfig currentConfig = HandbookConfig.INSTANCE, defaultConfig = new HandbookConfig();

                ConfigBuilder builder = ConfigBuilder.create()
                        .setParentScreen(parent)
                        .setTitle(Text.of("Handbook 2.0"))
                        .setSavingRunnable(currentConfig::write);

                ConfigCategory category = builder.getOrCreateCategory(Text.of("Settings"));
                ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                category.addEntry(entryBuilder.startBooleanToggle(Text.of("Enabled"), currentConfig.enabled)
                        .setSaveConsumer(newConfig -> currentConfig.enabled = newConfig)
                        .setDefaultValue(defaultConfig.enabled)
                        .build());

                category.addEntry(entryBuilder.startBooleanToggle(Text.of("Enable scanner"), currentConfig.enableScanner)
                        .setTooltip(Text.of("If enabled, will scan all nearby NPCs and add them to the" +
                                "\nhandbook. Also scans villager trades whenever you open them." +
                                "\nMay kill your performance so don't play with this enabled."))
                        .setSaveConsumer(newConfig -> currentConfig.enableScanner = newConfig)
                        .setDefaultValue(defaultConfig.enableScanner)
                        .build());

                return builder.build();
        }
}
