package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.Adapter;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public final class AdapterRenderer implements BlockEntityRendererProvider<Adapter> {
    public static final AdapterRenderer INSTANCE = new AdapterRenderer();
    private AdapterRenderer() {}

    @Override
    public BlockEntityRenderer<Adapter> create(Context context) {
        return new AdapterBlockEntityRenderer();
    }
}

final class AdapterBlockEntityRenderer implements BlockEntityRenderer<Adapter> {
    @Override
    public void render(Adapter adapter, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering");
        RenderSystem.setShaderColor(1, 1, 1, 1);
        boolean anyOpen = false;
        for (boolean open : adapter.openSides()) anyOpen |= open;
        if (anyOpen) {
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            RenderState.mirrorScale(pose, 1.0025f, -1.0025f, 1.0025f);
            pose.translate(-0.5f, -0.5f, -0.5f);
            var vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            var icon = Textures.getSprite(Textures.Block.AdapterOn);
            var matrix = pose.last().pose();
            if (adapter.isSideOpen(Direction.DOWN)) {
                vertices.addVertex(matrix, 0, 1, 0).setUv(icon.getU1(), icon.getV0());
                vertices.addVertex(matrix, 1, 1, 0).setUv(icon.getU0(), icon.getV0());
                vertices.addVertex(matrix, 1, 1, 1).setUv(icon.getU0(), icon.getV1());
                vertices.addVertex(matrix, 0, 1, 1).setUv(icon.getU1(), icon.getV1());
            }
            if (adapter.isSideOpen(Direction.UP)) {
                vertices.addVertex(matrix, 0, 0, 0).setUv(icon.getU1(), icon.getV1());
                vertices.addVertex(matrix, 0, 0, 1).setUv(icon.getU1(), icon.getV0());
                vertices.addVertex(matrix, 1, 0, 1).setUv(icon.getU0(), icon.getV0());
                vertices.addVertex(matrix, 1, 0, 0).setUv(icon.getU0(), icon.getV1());
            }
            if (adapter.isSideOpen(Direction.NORTH)) {
                vertices.addVertex(matrix, 1, 1, 0).setUv(icon.getU0(), icon.getV1());
                vertices.addVertex(matrix, 0, 1, 0).setUv(icon.getU1(), icon.getV1());
                vertices.addVertex(matrix, 0, 0, 0).setUv(icon.getU1(), icon.getV0());
                vertices.addVertex(matrix, 1, 0, 0).setUv(icon.getU0(), icon.getV0());
            }
            if (adapter.isSideOpen(Direction.SOUTH)) {
                vertices.addVertex(matrix, 0, 1, 1).setUv(icon.getU0(), icon.getV1());
                vertices.addVertex(matrix, 1, 1, 1).setUv(icon.getU1(), icon.getV1());
                vertices.addVertex(matrix, 1, 0, 1).setUv(icon.getU1(), icon.getV0());
                vertices.addVertex(matrix, 0, 0, 1).setUv(icon.getU0(), icon.getV0());
            }
            if (adapter.isSideOpen(Direction.WEST)) {
                vertices.addVertex(matrix, 0, 1, 0).setUv(icon.getU0(), icon.getV1());
                vertices.addVertex(matrix, 0, 1, 1).setUv(icon.getU1(), icon.getV1());
                vertices.addVertex(matrix, 0, 0, 1).setUv(icon.getU1(), icon.getV0());
                vertices.addVertex(matrix, 0, 0, 0).setUv(icon.getU0(), icon.getV0());
            }
            if (adapter.isSideOpen(Direction.EAST)) {
                vertices.addVertex(matrix, 1, 1, 1).setUv(icon.getU0(), icon.getV1());
                vertices.addVertex(matrix, 1, 1, 0).setUv(icon.getU1(), icon.getV1());
                vertices.addVertex(matrix, 1, 0, 0).setUv(icon.getU1(), icon.getV0());
                vertices.addVertex(matrix, 1, 0, 1).setUv(icon.getU0(), icon.getV0());
            }
            pose.popPose();
        }
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
