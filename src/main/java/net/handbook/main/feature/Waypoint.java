package net.handbook.main.feature;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public class Waypoint {

    static boolean active = false;
    static int x;
    static int y;
    static int z;
    static int tick;
    static double distance = 0;

    public static final Identifier BEAM_TEXTURE = new Identifier("textures/entity/beacon_beam.png");

    public static void tick() {
        if (!active) return;

        tick++;
        emitParticles();
        if (tick >= 60) tick = 0;
    }

    public static void setPosition(String position) {
        String[] coordinates = position.replace("Position:", "").replace(" ", "").split(",", 3);
        x = Integer.parseInt(coordinates[0]);
        y = Integer.parseInt(coordinates[1]);
        z = Integer.parseInt(coordinates[2]);
        setVisibility(true);
        tick = 0;
    }

    public static void setVisibility(boolean state) {
        active = state;
        if (HandbookScreen.clearWaypoint == null) return;
        HandbookScreen.clearWaypoint.visible = state;
        HandbookScreen.clearWaypoint.active = state;
    }

    public static void emitParticles() {
        if (tick > 40) return;

        ClientWorld world = MinecraftClient.getInstance().world;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (world == null || player == null) return;

        distance = Math.sqrt(Math.pow(player.getX() - x, 2) + Math.pow(player.getY() - y, 2) + Math.pow(player.getZ() - z, 2));
        if (distance < 5) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§aWaypoint removed."));
            MinecraftClient.getInstance().world.playSound(player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 2.0f, 1.7f, false);
            setVisibility(false);
        }

        double particleX = player.getX() + ((x - player.getX()) / distance) * ((float) tick / 3);
        double particleY = player.getY() + ((y - player.getY()) / distance) * ((float) tick / 3);
        double particleZ = player.getZ() + ((z - player.getZ()) / distance) * ((float) tick / 3);

        world.addParticle(ParticleTypes.END_ROD, particleX + (Math.random() - Math.random()) * 0.5,
                particleY + 0.5 + (Math.random() - Math.random()) * 0.5,
                particleZ + (Math.random() - Math.random()) * 0.5, 0, 0, 0);
    }

    public static void renderBeacon(WorldRenderContext context) {
        ClientWorld world = MinecraftClient.getInstance().world;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (world == null || player == null) return;

        double beaconX = distance < 150 ? x - player.getX() : ((x - player.getX()) / distance) * 150;
        double beaconZ = distance < 150 ? z - player.getZ() : ((z - player.getZ()) / distance) * 150;

        context.matrixStack().push();
        context.matrixStack().translate(beaconX, - player.getY(), beaconZ);
        BeaconBlockEntityRenderer.renderBeam(context.matrixStack(), context.consumers(), BEAM_TEXTURE, 0, 1,
                MinecraftClient.getInstance().world.getTime(), 0, 1024, DyeColor.LIGHT_BLUE.getColorComponents(), 0.3f, 0.3f
        );
        context.matrixStack().pop();
    }

    public static boolean isActive() {
        return active;
    }

    public static double getDistance() {
        return distance;
    }
}
