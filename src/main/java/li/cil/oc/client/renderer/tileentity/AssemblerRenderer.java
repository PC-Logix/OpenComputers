package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.Assembler;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class AssemblerRenderer implements BlockEntityRendererProvider<Assembler> {
    public static final AssemblerRenderer INSTANCE = new AssemblerRenderer();

    private AssemblerRenderer() {}

    @Override
    public BlockEntityRenderer<Assembler> create(Context context) {
        return new AssemblerBlockEntityRenderer();
    }
}

final class AssemblerBlockEntityRenderer implements BlockEntityRenderer<Assembler> {
    @Override
    public void render(Assembler assembler, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering");
        RenderSystem.setShaderColor(1, 1, 1, 1);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);

        var vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
        var matrix = pose.last().pose();
        var top = Textures.getSprite(Textures.Block.AssemblerTopOn);
        vertices.addVertex(matrix, -0.5f, 0.55f, 0.5f).setUv(top.getU0(), top.getV1());
        vertices.addVertex(matrix, 0.5f, 0.55f, 0.5f).setUv(top.getU1(), top.getV1());
        vertices.addVertex(matrix, 0.5f, 0.55f, -0.5f).setUv(top.getU1(), top.getV0());
        vertices.addVertex(matrix, -0.5f, 0.55f, -0.5f).setUv(top.getU0(), top.getV0());

        float indent = 6 / 16f + 0.005f;
        for (int i = 0; i < 4; i++) {
            if (assembler.isAssembling()) {
                var side = Textures.getSprite(Textures.Block.AssemblerSideAssembling);
                vertices.addVertex(matrix, indent, 0.5f, -indent).setUv(side.getU(0.5f - indent), side.getV1());
                vertices.addVertex(matrix, indent, 0.5f, indent).setUv(side.getU(0.5f + indent), side.getV1());
                vertices.addVertex(matrix, indent, -0.5f, indent).setUv(side.getU(0.5f + indent), side.getV0());
                vertices.addVertex(matrix, indent, -0.5f, -indent).setUv(side.getU(0.5f - indent), side.getV0());
            }

            var side = Textures.getSprite(Textures.Block.AssemblerSideOn);
            vertices.addVertex(matrix, 0.5005f, 0.5f, -0.5f).setUv(side.getU0(), side.getV1());
            vertices.addVertex(matrix, 0.5005f, 0.5f, 0.5f).setUv(side.getU1(), side.getV1());
            vertices.addVertex(matrix, 0.5005f, -0.5f, 0.5f).setUv(side.getU1(), side.getV0());
            vertices.addVertex(matrix, 0.5005f, -0.5f, -0.5f).setUv(side.getU0(), side.getV0());
            pose.mulPose(Axis.YP.rotationDegrees(90));
        }

        pose.popPose();
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
