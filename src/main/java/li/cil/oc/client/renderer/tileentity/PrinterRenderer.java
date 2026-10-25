package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.client.Textures;
import li.cil.oc.common.blockentity.Printer;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

public final class PrinterRenderer implements BlockEntityRendererProvider<Printer> {
    public static final PrinterRenderer INSTANCE = new PrinterRenderer();

    private PrinterRenderer() {}

    @Override
    public BlockEntityRenderer<Printer> create(Context context) {
        return new PrinterBlockEntityRenderer();
    }
}

final class PrinterBlockEntityRenderer implements BlockEntityRenderer<Printer> {
    @Override
    public void render(Printer printer, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering");

        if (!printer.data().stateOff().isEmpty()) {
            var stack = printer.data().createItemStack();
            pose.pushPose();
            pose.translate(0.5, 0.8, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() % 20000) / 20000f * 360));
            pose.scale(0.75f, 0.75f, 0.75f);

            Textures.Block.bind();
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                    light, overlay, pose, buffer, printer.getLevel(), 0);
            pose.popPose();
        }

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
