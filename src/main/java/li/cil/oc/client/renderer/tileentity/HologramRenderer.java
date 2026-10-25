package li.cil.oc.client.renderer.tileentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import li.cil.oc.Settings;
import li.cil.oc.common.blockentity.Hologram;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Quaternionf;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class HologramRenderer implements BlockEntityRendererProvider<Hologram>, BlockEntityRenderer<Hologram> {
    public static final HologramRenderer INSTANCE = new HologramRenderer();
    private static final Cache<Hologram, VertexBuffer> CACHE = createCache();
    private static final double SQRT_2 = Math.sqrt(2);

    private final Random random = new Random();

    private HologramRenderer() {}

    private static Cache<Hologram, VertexBuffer> createCache() {
        RemovalListener<Hologram, VertexBuffer> listener = notification -> notification.getValue().close();
        return CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .removalListener(listener)
            .build();
    }

    @Override
    public BlockEntityRenderer<Hologram> create(BlockEntityRendererProvider.Context context) {
        return new HologramRenderer();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        CACHE.cleanUp();
    }

    @Override
    public void render(Hologram hologram, float partialTick, PoseStack stack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!hologram.hasPower()) return;

        RenderState.checkError(getClass().getName() + ".render: entering");

        var pos = hologram.getBlockPos();
        var eye = Minecraft.getInstance().player.getEyePosition(partialTick);
        double dx = eye.x - (pos.getX() + 0.5);
        double dy = eye.y - (pos.getY() + 0.5);
        double dz = eye.z - (pos.getZ() + 0.5);
        double distSq = dx * dx + dy * dy + dz * dz;

        double fadeDistSq = hologram.getFadeStartDistanceSquared();
        double maxDistSq = hologram.getViewDistance() * hologram.getViewDistance();
        if (distSq > maxDistSq) return;
        float alpha = 0.75f * (distSq > fadeDistSq && maxDistSq > fadeDistSq
            ? Math.max(0f, 1f - (float) ((distSq - fadeDistSq) / (maxDistSq - fadeDistSq)))
            : 1f);

        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) bufferSource.endBatch();

        RenderState.makeItBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        stack.pushPose();
        stack.translate(0.5, 0.5, 0.5);

        if (hologram.yaw() == Direction.WEST) stack.mulPose(Axis.YP.rotationDegrees(-90));
        else if (hologram.yaw() == Direction.NORTH) stack.mulPose(Axis.YP.rotationDegrees(180));
        else if (hologram.yaw() == Direction.EAST) stack.mulPose(Axis.YP.rotationDegrees(90));
        if (hologram.pitch() == Direction.DOWN) stack.mulPose(Axis.XP.rotationDegrees(90));
        else if (hologram.pitch() == Direction.UP) stack.mulPose(Axis.XP.rotationDegrees(-90));

        applyRotation(stack, hologram.rotationAngle(), hologram.rotationX(), hologram.rotationY(), hologram.rotationZ());
        float animAngle = hologram.rotationSpeed() *
            (hologram.getLevel().getGameTime() % (360 * 20 - 1) + partialTick) / 20f;
        applyRotation(stack, animAngle, hologram.rotationSpeedX(), hologram.rotationSpeedY(), hologram.rotationSpeedZ());

        stack.scale(1.001f, 1.001f, 1.001f);
        var translation = hologram.translation();
        stack.translate(
            (translation.x * hologram.width() / 16.0 - 1.5) * hologram.scale(),
            translation.y * hologram.height() / 16.0 * hologram.scale(),
            (translation.z * hologram.width() / 16.0 - 1.5) * hologram.scale());

        if (Settings.get().hologramFlickerFrequency() > 0 && random.nextDouble() < Settings.get().hologramFlickerFrequency()) {
            stack.scale(
                Math.max(1f + (float) (random.nextGaussian() * 0.01), 0.001f),
                Math.max(1f + (float) (random.nextGaussian() * 0.001), 0.001f),
                Math.max(1f + (float) (random.nextGaussian() * 0.01), 0.001f));
        }

        // Hologram voxels occupy [0..width] x [0..height] x [0..width] in world units/16.
        RenderState.mirrorScale(stack, (float) (hologram.scale() / 16.0),
            (float) (hologram.scale() / 16.0), (float) (hologram.scale() / 16.0));

        VertexBuffer vbo;
        try {
            vbo = CACHE.get(hologram, () -> {
                hologram.needsRendering_$eq(true);
                return new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            });
        } catch (ExecutionException exception) {
            throw new RuntimeException(exception.getCause());
        }
        if (hologram.needsRendering()) {
            rebuildVBO(hologram, vbo);
            hologram.needsRendering_$eq(false);
        }

        if (hologram.visibleQuads() > 0) {
            var modelView = stack.last().pose();
            var projection = RenderSystem.getProjectionMatrix();
            var shader = GameRenderer.getPositionColorShader();

            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();

            // Depth pre-pass followed by a color pass prevents semi-transparent voxels from double-blending.
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(true);
            vbo.bind();
            vbo.drawWithShader(modelView, projection, shader);
            VertexBuffer.unbind();

            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(false);
            RenderSystem.depthFunc(514); // GL_EQUAL
            vbo.bind();
            vbo.drawWithShader(modelView, projection, shader);
            VertexBuffer.unbind();

            RenderSystem.depthFunc(515); // GL_LEQUAL
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
        }

        stack.popPose();
        RenderSystem.defaultBlendFunc();
        RenderState.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }

    private static void applyRotation(PoseStack stack, float degrees, float x, float y, float z) {
        if (degrees != 0 && x * x + y * y + z * z > 1e-6f) {
            stack.mulPose(new Quaternionf().rotationAxis(degrees * (float) (Math.PI / 180.0), x, y, z));
        }
    }

    private static void rebuildVBO(Hologram hologram, VertexBuffer vbo) {
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1 << 20);
        BufferBuilder builder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        hologram.visibleQuads_$eq(0);

        for (int x = 0; x < hologram.width(); x++) {
            for (int y = 0; y < hologram.height(); y++) {
                for (int z = 0; z < hologram.width(); z++) {
                    if (colorAt(hologram, x, y, z) == 0) continue;
                    int color = hologram.colors()[colorAt(hologram, x, y, z) - 1];
                    int red = color & 0xFF;
                    int green = (color >> 8) & 0xFF;
                    int blue = (color >> 16) & 0xFF;

                    if (!isSolid(hologram, x, y, z + 1)) {
                        vertex(builder, x + 1, y + 1, z + 1, red, green, blue); vertex(builder, x, y + 1, z + 1, red, green, blue);
                        vertex(builder, x, y, z + 1, red, green, blue); vertex(builder, x + 1, y, z + 1, red, green, blue);
                        incrementVisibleQuads(hologram);
                    }
                    if (!isSolid(hologram, x, y, z - 1)) {
                        vertex(builder, x + 1, y, z, red, green, blue); vertex(builder, x, y, z, red, green, blue);
                        vertex(builder, x, y + 1, z, red, green, blue); vertex(builder, x + 1, y + 1, z, red, green, blue);
                        incrementVisibleQuads(hologram);
                    }
                    if (!isSolid(hologram, x + 1, y, z)) {
                        vertex(builder, x + 1, y + 1, z + 1, red, green, blue); vertex(builder, x + 1, y, z + 1, red, green, blue);
                        vertex(builder, x + 1, y, z, red, green, blue); vertex(builder, x + 1, y + 1, z, red, green, blue);
                        incrementVisibleQuads(hologram);
                    }
                    if (!isSolid(hologram, x - 1, y, z)) {
                        vertex(builder, x, y, z + 1, red, green, blue); vertex(builder, x, y + 1, z + 1, red, green, blue);
                        vertex(builder, x, y + 1, z, red, green, blue); vertex(builder, x, y, z, red, green, blue);
                        incrementVisibleQuads(hologram);
                    }
                    if (!isSolid(hologram, x, y + 1, z)) {
                        vertex(builder, x + 1, y + 1, z, red, green, blue); vertex(builder, x, y + 1, z, red, green, blue);
                        vertex(builder, x, y + 1, z + 1, red, green, blue); vertex(builder, x + 1, y + 1, z + 1, red, green, blue);
                        incrementVisibleQuads(hologram);
                    }
                    if (!isSolid(hologram, x, y - 1, z)) {
                        vertex(builder, x + 1, y, z + 1, red, green, blue); vertex(builder, x, y, z + 1, red, green, blue);
                        vertex(builder, x, y, z, red, green, blue); vertex(builder, x + 1, y, z, red, green, blue);
                        incrementVisibleQuads(hologram);
                    }
                }
            }
        }

        try {
            if (hologram.visibleQuads() > 0) {
                vbo.bind();
                try {
                    vbo.upload(builder.buildOrThrow());
                } finally {
                    VertexBuffer.unbind();
                }
            }
        } finally {
            byteBuffer.close();
        }
    }

    private static int colorAt(Hologram hologram, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= hologram.width() || y >= hologram.height() || z >= hologram.width()) return 0;
        return hologram.getColor(x, y, z);
    }

    private static boolean isSolid(Hologram hologram, int x, int y, int z) {
        return colorAt(hologram, x, y, z) != 0;
    }

    private static void incrementVisibleQuads(Hologram hologram) {
        hologram.visibleQuads_$eq(hologram.visibleQuads() + 1);
    }

    private static void vertex(BufferBuilder builder, int x, int y, int z, int red, int green, int blue) {
        builder.addVertex((float) x, (float) y, (float) z).setColor(red, green, blue, 255);
    }

    @Override
    public AABB getRenderBoundingBox(Hologram entity) {
        double cx = entity.x() + 0.5;
        double cy = entity.y() + 0.5;
        double cz = entity.z() + 0.5;
        double horizontalScale = entity.width() / 16 * entity.scale() * SQRT_2;
        double verticalScale = entity.height() / 16 * entity.scale() * SQRT_2;
        var translation = entity.translation();
        return new AABB(
            cx + (-0.5 + translation.x) * horizontalScale,
            cy + translation.y * verticalScale,
            cz + (-0.5 + translation.z) * horizontalScale,
            cx + (0.5 + translation.x) * horizontalScale,
            cy + (1 + translation.y) * verticalScale,
            cz + (0.5 + translation.x) * horizontalScale);
    }
}
