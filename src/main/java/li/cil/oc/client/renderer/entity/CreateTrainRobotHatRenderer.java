package li.cil.oc.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.equipment.hats.EntityHats;
import li.cil.oc.common.entity.TrainRobot;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.level.block.Blocks;

/** Renders Create's train hat without making the base renderer require Create at runtime. */
public final class CreateTrainRobotHatRenderer {
    private CreateTrainRobotHatRenderer() {}

    public static void render(TrainRobot entity, PoseStack pose, MultiBufferSource buffer, int light) {
        if (!EntityHats.shouldRenderTrainHat(entity)) return;

        pose.pushPose();
        pose.translate(0.5, 0.86, 0.5);
        pose.scale(0.8f, 0.8f, 0.8f);
        SuperByteBuffer hat = CachedBuffers.partial(AllPartialModels.TRAIN_HAT, Blocks.AIR.defaultBlockState());
        hat.disableDiffuse();
        hat.light(light);
        hat.renderInto(pose, buffer.getBuffer(Sheets.cutoutBlockSheet()));
        pose.popPose();
    }
}
