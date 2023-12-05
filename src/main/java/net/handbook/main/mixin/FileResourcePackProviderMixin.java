package net.handbook.main.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.resources.HandbookResourcePack;
import net.minecraft.resource.FileResourcePackProvider;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.function.Consumer;

@Mixin(FileResourcePackProvider.class)
public abstract class FileResourcePackProviderMixin {

    @Inject(method = "register", at = @At(value = "HEAD"))
    public void register(Consumer<ResourcePackProfile> profileAdder, CallbackInfo ci) {
        Path path =  Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/textures");
        if (path.toFile().exists()) {
            profileAdder.accept(ResourcePackProfile.create(
                    "handbook",
                    Text.of("Handbook 2.0 Resources"),
                    true,
                    name -> new HandbookResourcePack(path, null),
                    ResourceType.CLIENT_RESOURCES,
                    ResourcePackProfile.InsertionPosition.BOTTOM,
                    ResourcePackSource.BUILTIN
            ));
        }
    }
}