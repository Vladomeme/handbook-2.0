package net.handbook.main.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.handbook.main.HandbookClient;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {

    @Shadow @Nullable
    private AdvancementTab selectedTab;

    @Unique
    TexturedButtonWidget button;

    @Inject(method = "init", at = @At("TAIL"))
    public void init(CallbackInfo ci) {
        addDrawableChild(button = new TexturedButtonWidget(this.width - 51, 1, 50, 11, 0, 0, 11,
                new Identifier("handbook", "textures/button.png"), 50, 22, button -> HandbookClient.openScreen()));
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RenderSystem.enableBlend();
        button.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"))
    public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_T) {
            assert selectedTab != null;
            for (AdvancementWidget advancement : ((WidgetsAccessor) selectedTab).getWidgets().values()) {
                HandbookClient.LOGGER.info(((DisplayAccessor)advancement).getDisplay().getTitle().getString() + "\n"
                        + ((DisplayAccessor)advancement).getDisplay().getDescription().toString());
            }
        }
    }

    @Mixin(AdvancementTab.class)
    public interface WidgetsAccessor {
        @Accessor("widgets")
        Map<Advancement, AdvancementWidget> getWidgets();
    }

    @Mixin(AdvancementWidget.class)
    public interface DisplayAccessor {
        @Accessor("display")
        AdvancementDisplay getDisplay();
    }

    protected AdvancementsScreenMixin(Text title) {
        super(title);
    }
}
