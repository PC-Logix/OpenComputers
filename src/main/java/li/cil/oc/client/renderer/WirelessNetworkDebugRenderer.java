package li.cil.oc.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc.Settings;
import li.cil.oc.server.network.WirelessNetwork;
import li.cil.oc.util.RTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;

public final class WirelessNetworkDebugRenderer {
    public static final WirelessNetworkDebugRenderer INSTANCE = new WirelessNetworkDebugRenderer();
    public static final int[] colors = {0xFF0000, 0x00FFFF, 0x00FF00, 0x0000FF, 0xFF00FF, 0xFFFF00, 0xFFFFFF, 0x000000};
    private static final RenderType RENDER_TYPE = RenderType.create(
        "oc_wireless_debug", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 131072, false, true,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
            .setTransparencyState(new RenderStateShard.TransparencyStateShard("translucent",
                () -> { RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); }, RenderSystem::disableBlend))
            .setCullState(new RenderStateShard.CullStateShard(false))
            .setDepthTestState(new RenderStateShard.DepthTestStateShard("always", GL11.GL_ALWAYS))
            .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
            .createCompositeState(false));

    private WirelessNetworkDebugRenderer() {}

    @SubscribeEvent
    public void onRenderWorldLastEvent(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !Settings.rTreeDebugRenderer()) return;
        var minecraft = Minecraft.getInstance();
        var world = minecraft.level;
        if (world == null) return;
        RTree<li.cil.oc.api.network.WirelessEndpoint> tree = WirelessNetwork.dimensions.get(world.dimension());
        if (tree == null) return;

        var player = minecraft.player;
        double partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        double px = player.xOld + (player.getX() - player.xOld) * partialTick;
        double py = player.yOld + (player.getY() - player.yOld) * partialTick;
        double pz = player.zOld + (player.getZ() - player.zOld) * partialTick;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-px, -py, -pz);
        var source = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = source.getBuffer(RENDER_TYPE);
        for (RTree.Bounds bounds : tree.allBounds()) {
            var min = bounds.min();
            var max = bounds.max();
            int color = colors[bounds.level() % colors.length];
            int red = (color >> 16) & 0xFF;
            int green = (color >> 8) & 0xFF;
            int blue = color & 0xFF;
            float size = 0.5f - bounds.level() * 0.05f;
            drawBox(pose, consumer,
                (float) min.x() - size, (float) min.y() - size, (float) min.z() - size,
                (float) max.x() + size, (float) max.y() + size, (float) max.z() + size,
                red, green, blue, 64);
        }
        source.endBatch(RENDER_TYPE);
        pose.popPose();
    }

    private static void vertex(PoseStack pose, VertexConsumer consumer, float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        consumer.addVertex(pose.last().pose(), x, y, z).setColor(red, green, blue, alpha);
    }

    private static void drawBox(PoseStack pose, VertexConsumer consumer,
                                float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                int red, int green, int blue, int alpha) {
        vertex(pose, consumer, minX, minY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, minY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, minY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, minY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, minY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, maxY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, maxY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, maxY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, maxY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, minY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, minY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, maxY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, minX, minY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, minY, minZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(pose, consumer, maxX, maxY, minZ, red, green, blue, alpha);
    }
}
