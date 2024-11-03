package net.handbook.main.mixin;

import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.NPCWriter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin  {
    //todo move to entity_load event
    @Inject(method = "onTrackedDataSet", at = @At("TAIL"))
    public void onTrackedDataSet(TrackedData<?> data, CallbackInfo ci) {
        if (!HandbookConfig.INSTANCE.enableScanner) return;

        Entity e = ((Entity) (Object) this);
        if (e.getType().equals(EntityType.VILLAGER)) NPCWriter.add(e, false);
    }
}
