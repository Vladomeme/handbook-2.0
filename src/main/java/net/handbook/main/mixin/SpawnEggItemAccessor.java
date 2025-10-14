package net.handbook.main.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.SpawnEggItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SpawnEggItem.class)
public interface SpawnEggItemAccessor {

    @Accessor("SPAWN_EGGS")
    Map<EntityType<? extends MobEntity>, SpawnEggItem> getSpawnEggs();
}
