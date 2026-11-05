package dev.jax.jaxclient;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Draws chest/container wireframes in world space, including when blocks are occluded. */
final class BlockEspRenderer {
    private BlockEspRenderer() {}

    static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || !Features.chestEsp.isEnabled()) return;
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;
        var matrices = context.matrixStack();
        if (matrices == null) return;
        Vec3d camera = context.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getDebugLineStrip(2.0));
        for (WorldScanner.Found found : WorldScanner.containers()) {
            BlockPos pos = found.pos();
            Box box = new Box(pos).expand(0.002);
            WorldRenderer.drawBox(matrices, lines, box, 0.15f, 0.85f, 1.0f, 1.0f);
        }
        matrices.pop();
    }
}
