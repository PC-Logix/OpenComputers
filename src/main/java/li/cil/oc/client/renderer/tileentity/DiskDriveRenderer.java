package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.DiskDrive;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;

public final class DiskDriveRenderer implements BlockEntityRendererProvider<DiskDrive> {
    public static final DiskDriveRenderer INSTANCE = new DiskDriveRenderer();
    private DiskDriveRenderer() {}

    @Override
    public BlockEntityRenderer<DiskDrive> create(Context context) {
        return new DiskDriveBlockEntityRenderer();
    }
}

final class DiskDriveBlockEntityRenderer implements BlockEntityRenderer<DiskDrive> {
    @Override
    public void render(DiskDrive drive, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering");
        RenderSystem.setShaderColor(1, 1, 1, 1);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        Direction yaw = drive.yaw();
        if (yaw == Direction.WEST) pose.mulPose(Axis.YP.rotationDegrees(-90));
        else if (yaw == Direction.NORTH) pose.mulPose(Axis.YP.rotationDegrees(180));
        else if (yaw == Direction.EAST) pose.mulPose(Axis.YP.rotationDegrees(90));

        var stack = drive.items()[0];
        if (!stack.isEmpty()) {
            pose.pushPose();
            pose.translate(0, 3.5 / 16.0, 6.0 / 16.0);
            pose.mulPose(Axis.XN.rotationDegrees(90));
            pose.scale(0.5f, 0.5f, 0.5f);
            int itemLight = LevelRenderer.getLightColor(drive.getLevel(), drive.getBlockPos().relative(drive.facing()));
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, itemLight,
                overlay, pose, buffer, drive.getLevel(), 0);
            pose.popPose();
        }

        if (System.currentTimeMillis() - drive.lastAccess() < 400 && drive.getLevel().random.nextDouble() > 0.1) {
            pose.translate(-0.5, 0.5, 0.505);
            RenderState.mirrorScale(pose, 1, -1, 1);
            var vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            var icon = Textures.getSprite(Textures.Block.DiskDriveFrontActivity);
            var matrix = pose.last().pose();
            vertices.addVertex(matrix, 0, 1, 0).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 1, 1, 0).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 1, 0, 0).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 0, 0, 0).setUv(icon.getU0(), icon.getV0());
        }
        pose.popPose();
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
