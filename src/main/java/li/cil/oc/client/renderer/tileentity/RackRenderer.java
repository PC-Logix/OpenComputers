package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.common.blockentity.Rack;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.common.NeoForge;

public final class RackRenderer implements BlockEntityRendererProvider<Rack> {
    public static final RackRenderer INSTANCE = new RackRenderer();

    private RackRenderer() {}

    @Override
    public BlockEntityRenderer<Rack> create(Context context) {
        return new RackBlockEntityRenderer();
    }
}

final class RackBlockEntityRenderer implements BlockEntityRenderer<Rack> {
    private static final float V_OFFSET = 2 / 16f;
    private static final float V_SIZE = 3 / 16f;

    @Override
    public void render(Rack rack, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");
        RenderSystem.setShaderColor(1, 1, 1, 1);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);

        Direction yaw = rack.yaw();
        if (yaw == Direction.WEST) pose.mulPose(Axis.YP.rotationDegrees(-90));
        else if (yaw == Direction.NORTH) pose.mulPose(Axis.YP.rotationDegrees(180));
        else if (yaw == Direction.EAST) pose.mulPose(Axis.YP.rotationDegrees(90));

        pose.translate(-0.5, 0.5, 0.505 - 0.5f / 16f);
        RenderState.mirrorScale(pose, 1, -1, 1);

        int rackLight = LevelRenderer.getLightColor(rack.getLevel(), rack.getBlockPos().relative(rack.facing()));
        for (int i = 0; i < rack.getContainerSize(); i++) {
            if (!rack.getItem(i).isEmpty()) {
                float v0 = V_OFFSET + i * V_SIZE;
                float v1 = V_OFFSET + (i + 1) * V_SIZE;
                var savedData = rack.lastData()[i];
                var data = savedData.isDefined() ? savedData.get() : CompoundStorage.EMPTY;
                NeoForge.EVENT_BUS.post(new RackMountableRenderEvent.BlockEntity(
                        rack, i, data, pose, buffer, rackLight, overlay, v0, v1));
            }
        }

        pose.popPose();
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
