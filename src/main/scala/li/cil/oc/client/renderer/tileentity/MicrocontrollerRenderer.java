package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.client.Textures;
import li.cil.oc.client.Textures$;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.Microcontroller;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class MicrocontrollerRenderer implements BlockEntityRenderer<Microcontroller> {
    public MicrocontrollerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Microcontroller mcu, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering");
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        stack.pushPose();
        stack.translate(0.5, 0.5, 0.5);

        Direction yaw = mcu.yaw();
        if (yaw == Direction.WEST) stack.mulPose(Axis.YP.rotationDegrees(-90));
        else if (yaw == Direction.NORTH) stack.mulPose(Axis.YP.rotationDegrees(180));
        else if (yaw == Direction.EAST) stack.mulPose(Axis.YP.rotationDegrees(90));

        stack.translate(-0.5, 0.5, 0.505);
        RenderState.mirrorScale(stack, 1.0f, -1.0f, 1.0f);
        VertexConsumer vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);

        renderFrontOverlay(stack, Textures.Block$.MODULE$.MicrocontrollerFrontLight(), vertices);
        if (mcu.isRunning()) {
            renderFrontOverlay(stack, Textures.Block$.MODULE$.MicrocontrollerFrontOn(), vertices);
        } else if (mcu.hasErrored() && RenderUtil.shouldShowErrorLight(mcu.hashCode())) {
            renderFrontOverlay(stack, Textures.Block$.MODULE$.MicrocontrollerFrontError(), vertices);
        }

        stack.popPose();
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }

    private void renderFrontOverlay(PoseStack stack, ResourceLocation texture, VertexConsumer vertices) {
        TextureAtlasSprite icon = Textures$.MODULE$.getSprite(texture);
        var matrix = stack.last().pose();
        vertices.addVertex(matrix, 0, 1, 0).setUv(icon.getU0(), icon.getV1());
        vertices.addVertex(matrix, 1, 1, 0).setUv(icon.getU1(), icon.getV1());
        vertices.addVertex(matrix, 1, 0, 0).setUv(icon.getU1(), icon.getV0());
        vertices.addVertex(matrix, 0, 0, 0).setUv(icon.getU0(), icon.getV0());
    }
}
