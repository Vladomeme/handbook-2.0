package net.handbook.main.mixin;

import net.handbook.main.HandbookClient;
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
        addDrawableChild(button = new TexturedButtonWidget(this.width - 55, 1, 54, 15, 0, 0, 15,
                new Identifier("handbook", "textures/button.png"), 54, 30, button -> HandbookClient.openScreen()));
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        button.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    protected AdvancementsScreenMixin(Text title) {
        super(title);
    }
}
