package li.cil.oc.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import li.cil.oc.Constants;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.item.UpgradeRenderer;
import li.cil.oc.api.event.RobotRenderEvent.MountPoint;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.integration.opencomputers.Item;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public final class ItemUpgradeRenderer {
    private static final ItemInfo CRAFTING = li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.CraftingUpgrade());
    private static final ItemInfo GENERATOR = li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.GeneratorUpgrade());
    private static final ItemInfo INVENTORY = li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.InventoryUpgrade());

    private ItemUpgradeRenderer() {}

    public static String preferredMountPoint(ItemStack stack, Set<String> availableMountPoints) {
        ItemInfo descriptor = li.cil.oc.api.Items.get(stack);
        if (descriptor == CRAFTING || descriptor == GENERATOR || descriptor == INVENTORY) {
            if (descriptor == GENERATOR && availableMountPoints.contains(UpgradeRenderer.MountPointName.BottomBack)) {
                return UpgradeRenderer.MountPointName.BottomBack;
            }
            if (descriptor == INVENTORY && availableMountPoints.contains(UpgradeRenderer.MountPointName.TopBack)) {
                return UpgradeRenderer.MountPointName.TopBack;
            }
            return UpgradeRenderer.MountPointName.Any;
        }
        return UpgradeRenderer.MountPointName.None;
    }

    public static boolean canRender(ItemStack stack) {
        ItemInfo descriptor = li.cil.oc.api.Items.get(stack);
        return descriptor == CRAFTING || descriptor == GENERATOR || descriptor == INVENTORY;
    }

    public static void render(PoseStack pose, MultiBufferSource buffer, int light, ItemStack stack, MountPoint mountPoint) {
        ItemInfo descriptor = li.cil.oc.api.Items.get(stack);
        if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.CraftingUpgrade())) {
            drawSimpleBlock(pose, buffer.getBuffer(RenderTypes.UPGRADE_CRAFTING), light, mountPoint, 0);
            RenderState.checkError(ItemUpgradeRenderer.class.getName() + ".renderItem: crafting upgrade");
        } else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.GeneratorUpgrade())) {
            float offset = Item.dataTag(stack).getInt("remainingTicks") > 0 ? 0.5f : 0;
            drawSimpleBlock(pose, buffer.getBuffer(RenderTypes.UPGRADE_GENERATOR), light, mountPoint, offset);
            RenderState.checkError(ItemUpgradeRenderer.class.getName() + ".renderItem: generator upgrade");
        } else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.InventoryUpgrade())) {
            drawSimpleBlock(pose, buffer.getBuffer(RenderTypes.UPGRADE_INVENTORY), light, mountPoint, 0);
            RenderState.checkError(ItemUpgradeRenderer.class.getName() + ".renderItem: inventory upgrade");
        }
    }

    private static final float MIN_X = -0.1f, MIN_Y = -0.1f, MIN_Z = -0.1f;
    private static final float MAX_X = 0.1f, MAX_Y = 0.1f, MAX_Z = 0.1f;

    private static int lightU(int light) { return light & 0xFFFF; }
    private static int lightV(int light) { return (light >> 16) & 0xFFFF; }

    private static void drawSimpleBlock(PoseStack pose, VertexConsumer vertices, int light, MountPoint mount, float frontOffset) {
        pose.mulPose(new Quaternionf().rotationAxis(mount.rotation.w * ((float) Math.PI / 180f),
            mount.rotation.x, mount.rotation.y, mount.rotation.z));
        pose.translate(mount.offset.x, mount.offset.y, mount.offset.z);
        var matrix = pose.last().pose();
        var normal = pose.last();
        int u = lightU(light), v = lightV(light);

        vertex(vertices, matrix, normal, MIN_X, MIN_Y, MAX_Z, frontOffset, 0.5f, u, v, 0, 0, 1);
        vertex(vertices, matrix, normal, MAX_X, MIN_Y, MAX_Z, frontOffset + 0.5f, 0.5f, u, v, 0, 0, 1);
        vertex(vertices, matrix, normal, MAX_X, MAX_Y, MAX_Z, frontOffset + 0.5f, 0, u, v, 0, 0, 1);
        vertex(vertices, matrix, normal, MIN_X, MAX_Y, MAX_Z, frontOffset, 0, u, v, 0, 0, 1);

        vertex(vertices, matrix, normal, MAX_X, MAX_Y, MAX_Z, 1, 0.5f, u, v, 0, 1, 0);
        vertex(vertices, matrix, normal, MAX_X, MAX_Y, MIN_Z, 1, 1, u, v, 0, 1, 0);
        vertex(vertices, matrix, normal, MIN_X, MAX_Y, MIN_Z, 0.5f, 1, u, v, 0, 1, 0);
        vertex(vertices, matrix, normal, MIN_X, MAX_Y, MAX_Z, 0.5f, 0.5f, u, v, 0, 1, 0);

        vertex(vertices, matrix, normal, MIN_X, MIN_Y, MAX_Z, 0.5f, 0.5f, u, v, 0, -1, 0);
        vertex(vertices, matrix, normal, MIN_X, MIN_Y, MIN_Z, 0.5f, 1, u, v, 0, -1, 0);
        vertex(vertices, matrix, normal, MAX_X, MIN_Y, MIN_Z, 1, 1, u, v, 0, -1, 0);
        vertex(vertices, matrix, normal, MAX_X, MIN_Y, MAX_Z, 1, 0.5f, u, v, 0, -1, 0);

        vertex(vertices, matrix, normal, MAX_X, MAX_Y, MAX_Z, 0, 0.5f, u, v, 1, 0, 0);
        vertex(vertices, matrix, normal, MAX_X, MIN_Y, MAX_Z, 0, 1, u, v, 1, 0, 0);
        vertex(vertices, matrix, normal, MAX_X, MIN_Y, MIN_Z, 0.5f, 1, u, v, 1, 0, 0);
        vertex(vertices, matrix, normal, MAX_X, MAX_Y, MIN_Z, 0.5f, 0.5f, u, v, 1, 0, 0);

        vertex(vertices, matrix, normal, MIN_X, MIN_Y, MAX_Z, 0, 1, u, v, -1, 0, 0);
        vertex(vertices, matrix, normal, MIN_X, MAX_Y, MAX_Z, 0, 0.5f, u, v, -1, 0, 0);
        vertex(vertices, matrix, normal, MIN_X, MAX_Y, MIN_Z, 0.5f, 0.5f, u, v, -1, 0, 0);
        vertex(vertices, matrix, normal, MIN_X, MIN_Y, MIN_Z, 0.5f, 1, u, v, -1, 0, 0);
    }

    private static void vertex(VertexConsumer vertices, org.joml.Matrix4f matrix, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                               float x, float y, float z, float texU, float texV, int lightU, int lightV,
                               float nx, float ny, float nz) {
        vertices.addVertex(matrix, x, y, z).setColor(0xFF, 0xFF, 0xFF, 0xFF).setUv(texU, texV)
            .setUv2(lightU, lightV).setNormal(pose, nx, ny, nz);
    }
}
