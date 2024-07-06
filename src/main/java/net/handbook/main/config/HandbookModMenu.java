package net.handbook.main.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.text.Text;

public class HandbookModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            return HandbookConfig.INSTANCE::create;
        }
        return parent -> new NoticeScreen(() -> MinecraftClient.getInstance().setScreen(parent),
                Text.of("Handbook 2.0"), Text.of("Mod requires YetAnotherConfigLib to be able to show the config."));
    }
}
