package net.handbook.main.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StringNbtReader.class)
public interface StringNbtReaderInvoker {

    @SuppressWarnings("RedundantThrows")
    @Invoker("parseList")
    NbtElement invokeParseList() throws CommandSyntaxException;
}
