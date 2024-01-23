package net.handbook.main.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.handbook.main.config.HandbookConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {

    @Unique
    TexturedButtonWidget button;

    @Inject(method = "init", at = @At("TAIL"))
    public void init(CallbackInfo ci) {
        if (!HandbookConfig.INSTANCE.enabled) return;
        addDrawableChild(button = new TexturedButtonWidget(width - 51, 1, 50, 11, 0, 0, 11,
                new Identifier("handbook", "textures/button.png"), 50, 22, button -> HandbookClient.openScreen()));
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!HandbookConfig.INSTANCE.enabled) return;
        RenderSystem.enableBlend();
        button.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
    }

    protected AdvancementsScreenMixin(Text title) {
        super(title);
    }
}