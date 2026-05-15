package net.handbook.main.mixin;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StringNbtReader.class)
public interface StringNbtReaderAccessor {

    @Invoker("parseList")
    NbtElement parseList();
}
