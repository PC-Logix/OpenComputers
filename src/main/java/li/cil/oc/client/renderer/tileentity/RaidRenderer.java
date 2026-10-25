package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.Raid;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public final class RaidRenderer implements BlockEntityRendererProvider<Raid> {
    public static final RaidRenderer INSTANCE = new RaidRenderer();
    private RaidRenderer() {}

    @Override
    public BlockEntityRenderer<Raid> create(Context context) {
        return new RaidBlockEntityRenderer();
    }
}

final class RaidBlockEntityRenderer implements BlockEntityRenderer<Raid> {
    private static final float U1 = 2 / 16f;
    private static final float SLOT_WIDTH = 4 / 16f;

    @Override
    public void render(Raid raid, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");
        RenderSystem.setShaderColor(1, 1, 1, 1);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        Direction yaw = raid.yaw();
        if (yaw == Direction.WEST) pose.mulPose(Axis.YP.rotationDegrees(-90));
        else if (yaw == Direction.NORTH) pose.mulPose(Axis.YP.rotationDegrees(180));
        else if (yaw == Direction.EAST) pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.translate(-0.5, 0.5, 0.505);
        RenderState.mirrorScale(pose, 1, -1, 1);
        VertexConsumer vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
        var error = Textures.getSprite(Textures.Block.RaidFrontError);
        for (int slot = 0; slot < raid.getContainerSize(); slot++) {
            if (!raid.presence()[slot]) renderSlot(pose, vertices, slot, error);
        }
        var activity = Textures.getSprite(Textures.Block.RaidFrontActivity);
        for (int slot = 0; slot < raid.getContainerSize(); slot++) {
            if (System.currentTimeMillis() - raid.lastAccess() < 400
                && raid.getEnvironmentLevel().random.nextDouble() > 0.1
                && slot == raid.lastAccess() % raid.getContainerSize()) {
                renderSlot(pose, vertices, slot, activity);
            }
        }
        pose.popPose();
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }

    private static void renderSlot(PoseStack pose, VertexConsumer vertices, int slot, TextureAtlasSprite icon) {
        float low = U1 + slot * SLOT_WIDTH;
        float high = U1 + (slot + 1) * SLOT_WIDTH;
        var matrix = pose.last().pose();
        vertices.addVertex(matrix, low, 1, 0).setUv(icon.getU(low), icon.getV1());
        vertices.addVertex(matrix, high, 1, 0).setUv(icon.getU(high), icon.getV1());
        vertices.addVertex(matrix, high, 0, 0).setUv(icon.getU(high), icon.getV0());
        vertices.addVertex(matrix, low, 0, 0).setUv(icon.getU(low), icon.getV0());
    }
}
