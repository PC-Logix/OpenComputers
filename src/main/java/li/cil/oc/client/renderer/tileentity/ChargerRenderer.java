package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.Charger;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public final class ChargerRenderer implements BlockEntityRendererProvider<Charger> {
    public static final ChargerRenderer INSTANCE = new ChargerRenderer();

    private ChargerRenderer() {}

    @Override
    public BlockEntityRenderer<Charger> create(Context context) {
        return new ChargerBlockEntityRenderer();
    }
}

final class ChargerBlockEntityRenderer implements BlockEntityRenderer<Charger> {
    @Override
    public void render(Charger charger, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering");
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (charger.chargeSpeed() > 0) {
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            Direction yaw = charger.yaw();
            if (yaw == Direction.WEST) pose.mulPose(Axis.YP.rotationDegrees(-90));
            else if (yaw == Direction.NORTH) pose.mulPose(Axis.YP.rotationDegrees(180));
            else if (yaw == Direction.EAST) pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.translate(-0.5f, 0.5f, 0.5f);
            RenderState.mirrorScale(pose, 1, -1, 1);

            var vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            var matrix = pose.last().pose();
            float inverse = 1 - (float) charger.chargeSpeed();
            var front = Textures.getSprite(Textures.Block.ChargerFrontOn);
            vertices.addVertex(matrix, 0, 1, 0.005f).setUv(front.getU0(), front.getV1());
            vertices.addVertex(matrix, 1, 1, 0.005f).setUv(front.getU1(), front.getV1());
            vertices.addVertex(matrix, 1, inverse, 0.005f).setUv(front.getU1(), front.getV(inverse));
            vertices.addVertex(matrix, 0, inverse, 0.005f).setUv(front.getU0(), front.getV(inverse));

            if (charger.hasPower()) {
                var side = Textures.getSprite(Textures.Block.ChargerSideOn);
                vertices.addVertex(matrix, -0.005f, 1, -1).setUv(side.getU0(), side.getV1());
                vertices.addVertex(matrix, -0.005f, 1, 0).setUv(side.getU1(), side.getV1());
                vertices.addVertex(matrix, -0.005f, 0, 0).setUv(side.getU1(), side.getV0());
                vertices.addVertex(matrix, -0.005f, 0, -1).setUv(side.getU0(), side.getV0());
                vertices.addVertex(matrix, 1, 1, -1.005f).setUv(side.getU0(), side.getV1());
                vertices.addVertex(matrix, 0, 1, -1.005f).setUv(side.getU1(), side.getV1());
                vertices.addVertex(matrix, 0, 0, -1.005f).setUv(side.getU1(), side.getV0());
                vertices.addVertex(matrix, 1, 0, -1.005f).setUv(side.getU0(), side.getV0());
                vertices.addVertex(matrix, 1.005f, 1, 0).setUv(side.getU0(), side.getV1());
                vertices.addVertex(matrix, 1.005f, 1, -1).setUv(side.getU1(), side.getV1());
                vertices.addVertex(matrix, 1.005f, 0, -1).setUv(side.getU1(), side.getV0());
                vertices.addVertex(matrix, 1.005f, 0, 0).setUv(side.getU0(), side.getV0());
            }
            pose.popPose();
        }
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
