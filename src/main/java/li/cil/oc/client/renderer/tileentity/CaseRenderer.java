package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.Case;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class CaseRenderer implements BlockEntityRendererProvider<Case> {
    public static final CaseRenderer INSTANCE = new CaseRenderer();

    private CaseRenderer() {}

    @Override
    public BlockEntityRenderer<Case> create(Context context) {
        return new CaseBlockEntityRenderer();
    }
}

final class CaseBlockEntityRenderer implements BlockEntityRenderer<Case> {
    @Override
    public void render(Case computer, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering");
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);

        Direction yaw = computer.yaw();
        if (yaw == Direction.WEST) pose.mulPose(Axis.YP.rotationDegrees(-90));
        else if (yaw == Direction.NORTH) pose.mulPose(Axis.YP.rotationDegrees(180));
        else if (yaw == Direction.EAST) pose.mulPose(Axis.YP.rotationDegrees(90));

        pose.translate(-0.5, 0.5, 0.505);
        RenderState.mirrorScale(pose, 1, -1, 1);
        VertexConsumer vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
        if (computer.isRunning()) {
            renderFrontOverlay(pose, Textures.Block.CaseFrontOn, vertices);
            if (System.currentTimeMillis() - computer.lastFileSystemAccess() < 400
                && computer.getLevel().random.nextDouble() > 0.1) {
                renderFrontOverlay(pose, Textures.Block.CaseFrontActivity, vertices);
            }
        } else if (computer.hasErrored() && RenderUtil.shouldShowErrorLight(computer.hashCode())) {
            renderFrontOverlay(pose, Textures.Block.CaseFrontError, vertices);
        }

        pose.popPose();
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }

    private static void renderFrontOverlay(PoseStack pose, ResourceLocation texture, VertexConsumer vertices) {
        var icon = Textures.getSprite(texture);
        var matrix = pose.last().pose();
        vertices.addVertex(matrix, 0, 1, 0).setUv(icon.getU0(), icon.getV1());
        vertices.addVertex(matrix, 1, 1, 0).setUv(icon.getU1(), icon.getV1());
        vertices.addVertex(matrix, 1, 0, 0).setUv(icon.getU1(), icon.getV0());
        vertices.addVertex(matrix, 0, 0, 0).setUv(icon.getU0(), icon.getV0());
    }
}
