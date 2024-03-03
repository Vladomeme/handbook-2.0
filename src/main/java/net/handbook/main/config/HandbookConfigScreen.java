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
                        .setTooltip(Text.of("""
                                If enabled, will scan all nearby NPCs and add them to the
                                handbook. Also records villager trades whenever you open them.
                                May kill your performance so don't play with this enabled."""))
                        .setSaveConsumer(newConfig -> currentConfig.enableScanner = newConfig)
                        .setDefaultValue(defaultConfig.enableScanner)
                        .build());

                category.addEntry(entryBuilder.startBooleanToggle(Text.of("Auto-continue paths"), currentConfig.alwaysContinue)
                        .setTooltip(Text.of("""
                                If enabled, will not pause a waypoint path when an action
                                 is required before continuing."""))
                        .setSaveConsumer(newConfig -> currentConfig.alwaysContinue = newConfig)
                        .setDefaultValue(defaultConfig.alwaysContinue)
                        .build());

                category.addEntry(entryBuilder.startBooleanToggle(Text.of("Use monumenta particles"), currentConfig.monuParticles)
                        .setTooltip(Text.of("""
                                If enabled, waypoint display will use same particles as the
                                monumenta quest compass (green particles)."""))
                        .setSaveConsumer(newConfig -> currentConfig.monuParticles = newConfig)
                        .setDefaultValue(defaultConfig.monuParticles)
                        .build());

                category.addEntry(entryBuilder.startBooleanToggle(Text.of("Render beacon"), currentConfig.renderBeacon)
                        .setTooltip(Text.of("If enabled, a beacon will be rendered at current waypoint."))
                        .setSaveConsumer(newConfig -> currentConfig.renderBeacon = newConfig)
                        .setDefaultValue(defaultConfig.renderBeacon)
                        .build());

                category.addEntry(entryBuilder.startBooleanToggle(Text.of("Edit chat messages"), currentConfig.editMessages)
                        .setTooltip(Text.of("""
                                If enabled, will add a waypoint ClickEvent to messages with
                                correctly formatted coordinates."""))
                        .setSaveConsumer(newConfig -> currentConfig.editMessages = newConfig)
                        .setDefaultValue(defaultConfig.editMessages)
                        .build());

                return builder.build();
        }
}
