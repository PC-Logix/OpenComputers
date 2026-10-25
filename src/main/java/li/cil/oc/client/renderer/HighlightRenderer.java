package li.cil.oc.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.client.Textures;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.SettingsAccessJava;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

import java.util.Random;

public final class HighlightRenderer {
    public static final HighlightRenderer INSTANCE = new HighlightRenderer();
    private static final Random RANDOM = new Random();
    private static final RenderType TEX_HOLOGRAM = RenderTypes.createTexturedQuad(
        "hologram_effect", Textures.Model.HologramEffect, DefaultVertexFormat.POSITION_TEX_COLOR, true);

    private HighlightRenderer() {}

    private static final class TabletHolder {
        private static final Object TABLET = Items.get(Constants.ItemName$.MODULE$.Tablet());
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(RenderHighlightEvent.Block event) {
        if (event.getTarget() == null || event.getTarget().getBlockPos() == null) return;

        var hitInfo = event.getTarget();
        Level world = Minecraft.getInstance().level;
        BlockPosition blockPos = BlockPositionsJava.apply(hitInfo.getBlockPos(), world);
        PoseStack stack = event.getPoseStack();
        ItemStack heldItem = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!java.util.Objects.equals(Items.get(heldItem), TabletHolder.TABLET)) return;

        if (world.isEmptyBlock(hitInfo.getBlockPos())) return;
        var shape = world.getBlockState(hitInfo.getBlockPos()).getShape(world, hitInfo.getBlockPos(),
            CollisionContext.of(event.getCamera().getEntity()));
        float minX = (float) shape.min(Direction.Axis.X);
        float minY = (float) shape.min(Direction.Axis.Y);
        float minZ = (float) shape.min(Direction.Axis.Z);
        float maxX = (float) shape.max(Direction.Axis.X);
        float maxY = (float) shape.max(Direction.Axis.Y);
        float maxZ = (float) shape.max(Direction.Axis.Z);
        Direction sideHit = hitInfo.getDirection();
        Vec3 view = event.getCamera().getPosition();

        stack.pushPose();
        stack.translate(blockPos.x() - view.x, blockPos.y() - view.y, blockPos.z() - view.z);
        stack.scale(1.002f, 1.002f, 1.002f);

        double flickerFrequency = SettingsAccessJava.get().hologramFlickerFrequency();
        if (flickerFrequency > 0 && RANDOM.nextDouble() < flickerFrequency) {
            float sx = 1 - Math.abs(sideHit.getStepX());
            float sy = 1 - Math.abs(sideHit.getStepY());
            float sz = 1 - Math.abs(sideHit.getStepZ());
            stack.scale(
                Math.max(1f + (float) (RANDOM.nextGaussian() * 0.01), 0.001f),
                Math.max(1f + (float) (RANDOM.nextGaussian() * 0.001), 0.001f),
                Math.max(1f + (float) (RANDOM.nextGaussian() * 0.01), 0.001f));
            stack.translate((float) (RANDOM.nextGaussian() * 0.01 * sx),
                (float) (RANDOM.nextGaussian() * 0.01 * sy),
                (float) (RANDOM.nextGaussian() * 0.01 * sz));
        }

        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(TEX_HOLOGRAM);
        switch (sideHit) {
            case UP -> {
                vertex(consumer, stack, maxX, maxY + 0.002f, maxZ, maxZ * 16, maxX * 16);
                vertex(consumer, stack, maxX, maxY + 0.002f, minZ, minZ * 16, maxX * 16);
                vertex(consumer, stack, minX, maxY + 0.002f, minZ, minZ * 16, minX * 16);
                vertex(consumer, stack, minX, maxY + 0.002f, maxZ, maxZ * 16, minX * 16);
            }
            case DOWN -> {
                vertex(consumer, stack, maxX, minY - 0.002f, minZ, minZ * 16, maxX * 16);
                vertex(consumer, stack, maxX, minY - 0.002f, maxZ, maxZ * 16, maxX * 16);
                vertex(consumer, stack, minX, minY - 0.002f, maxZ, maxZ * 16, minX * 16);
                vertex(consumer, stack, minX, minY - 0.002f, minZ, minZ * 16, minX * 16);
            }
            case EAST -> {
                vertex(consumer, stack, maxX + 0.002f, maxY, minZ, minZ * 16, maxY * 16);
                vertex(consumer, stack, maxX + 0.002f, maxY, maxZ, maxZ * 16, maxY * 16);
                vertex(consumer, stack, maxX + 0.002f, minY, maxZ, maxZ * 16, minY * 16);
                vertex(consumer, stack, maxX + 0.002f, minY, minZ, minZ * 16, minY * 16);
            }
            case WEST -> {
                vertex(consumer, stack, minX - 0.002f, maxY, maxZ, maxZ * 16, maxY * 16);
                vertex(consumer, stack, minX - 0.002f, maxY, minZ, minZ * 16, maxY * 16);
                vertex(consumer, stack, minX - 0.002f, minY, minZ, minZ * 16, minY * 16);
                vertex(consumer, stack, minX - 0.002f, minY, maxZ, maxZ * 16, minY * 16);
            }
            case SOUTH -> {
                vertex(consumer, stack, maxX, maxY, maxZ + 0.002f, maxX * 16, maxY * 16);
                vertex(consumer, stack, minX, maxY, maxZ + 0.002f, minX * 16, maxY * 16);
                vertex(consumer, stack, minX, minY, maxZ + 0.002f, minX * 16, minY * 16);
                vertex(consumer, stack, maxX, minY, maxZ + 0.002f, maxX * 16, minY * 16);
            }
            default -> {
                vertex(consumer, stack, minX, maxY, minZ - 0.002f, minX * 16, maxY * 16);
                vertex(consumer, stack, maxX, maxY, minZ - 0.002f, maxX * 16, maxY * 16);
                vertex(consumer, stack, maxX, minY, minZ - 0.002f, maxX * 16, minY * 16);
                vertex(consumer, stack, minX, minY, minZ - 0.002f, minX * 16, minY * 16);
            }
        }

        stack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack stack, float x, float y, float z, float u, float v) {
        consumer.addVertex(stack.last().pose(), x, y, z).setUv(u, v).setColor(0.0F, 1.0F, 0.0F, 0.4F);
    }
}
