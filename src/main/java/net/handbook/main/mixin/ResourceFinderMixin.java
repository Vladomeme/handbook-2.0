package net.handbook.main.mixin;

import net.minecraft.resource.ResourceFinder;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourceFinder.class)
public abstract class ResourceFinderMixin {

    @Inject(method = "toResourceId", at = @At(value = "HEAD"), cancellable = true)
    public void toResourceId(Identifier path, CallbackInfoReturnable<Identifier> cir) {
        if (path.getNamespace().equals("handbook")) {
            cir.setReturnValue(path);
        }
    }
}
