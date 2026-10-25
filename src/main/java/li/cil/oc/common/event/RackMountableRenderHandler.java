package li.cil.oc.common.event;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.mojang.math.Axis;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.client.renderer.tileentity.RenderUtil;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Arrays;
import java.util.Objects;

public final class RackMountableRenderHandler {
    public static final RackMountableRenderHandler INSTANCE = new RackMountableRenderHandler();

    private RackMountableRenderHandler() {}

    // Resolve items on first render, after item registration has completed.
    private static final class ItemTypes {
        static final ItemInfo DISK_DRIVE = Items.get(Constants.ItemName$.MODULE$.DiskDriveMountable());
        static final ItemInfo[] SERVERS = {
                Items.get(Constants.ItemName$.MODULE$.ServerTier1()),
                Items.get(Constants.ItemName$.MODULE$.ServerTier2()),
                Items.get(Constants.ItemName$.MODULE$.ServerTier3()),
                Items.get(Constants.ItemName$.MODULE$.ServerTier4()),
                Items.get(Constants.ItemName$.MODULE$.ServerCreative())
        };
        static final ItemInfo TERMINAL_SERVER = Items.get(Constants.ItemName$.MODULE$.TerminalServer());
        static final ItemInfo RACK_KVM = Items.get(Constants.ItemName$.MODULE$.RackKVM());
        static final ItemInfo CAPACITOR = Items.get(Constants.ItemName$.MODULE$.CapacitorMountable());
    }

    @SubscribeEvent
    public void onRackMountableRendering(RackMountableRenderEvent.BlockEntity event) {
        if (event.data == null) return;
        ItemInfo item = Items.get(event.rack.getItem(event.mountable));

        if (Objects.equals(item, ItemTypes.DISK_DRIVE)) {
            var disk = event.data.get(OCComponentsJava.Network.DISK_ITEM.get());
            if (disk != null && !disk.isEmpty()) {
                var matrix = event.stack;
                matrix.pushPose();
                RenderState.mirrorScale(matrix, 1, -1, 1);
                matrix.translate(10 / 16f, -(3.5f + event.mountable * 3f) / 16f, -2 / 16f);
                matrix.mulPose(Axis.XN.rotationDegrees(90));
                matrix.scale(0.5f, 0.5f, 0.5f);
                Minecraft.getInstance().getItemRenderer().renderStatic(disk.mutableCopy(),
                        ItemDisplayContext.FIXED, event.light, event.overlay, matrix,
                        event.typeBuffer, null, 0);
                matrix.popPose();
            }

            Object access = event.data.get(OCComponentsJava.Network.LAST_ACCESS.get());
            if (access instanceof Number last && System.currentTimeMillis() - last.longValue() < 400
                    && event.rack.getEnvironmentLevel().getRandom().nextDouble() > 0.1) {
                renderOverlayFromAtlas(event, Textures.Block.RackDiskDriveActivity);
            }
        } else if (Arrays.asList(ItemTypes.SERVERS).contains(item)) {
            boolean running = Boolean.TRUE.equals(event.data.get(OCComponentsJava.IS_RUNNING.get()));
            boolean errored = event.data.has(OCComponentsJava.IS_ERRORED.get());
            Object diskAccess = event.data.get(OCComponentsJava.Network.LAST_DISK_ACCESS.get());
            Object networkAccess = event.data.get(OCComponentsJava.Network.LAST_NETWORK_ACCESS.get());
            long lastDisk = diskAccess instanceof Number number ? number.longValue() : 0L;
            long lastNetwork = networkAccess instanceof Number number ? number.longValue() : 0L;

            if (running) renderOverlayFromAtlas(event, Textures.Block.RackServerOn);
            if (errored && RenderUtil.shouldShowErrorLight(event.rack.hashCode() * (event.mountable + 1))) {
                renderOverlayFromAtlas(event, Textures.Block.RackServerError);
            }
            if (System.currentTimeMillis() - lastDisk < 400
                    && event.rack.getEnvironmentLevel().getRandom().nextDouble() > 0.1) {
                renderOverlayFromAtlas(event, Textures.Block.RackServerActivity);
            }
            if (System.currentTimeMillis() - lastNetwork < 300
                    && System.currentTimeMillis() % 200 > 100 && running) {
                renderOverlayFromAtlas(event, Textures.Block.RackServerNetworkActivity);
            }
        } else if (Objects.equals(item, ItemTypes.TERMINAL_SERVER)) {
            renderOverlayFromAtlas(event, Textures.Block.RackTerminalServerOn);
            var keys = event.data.get(OCComponentsJava.KEYS.get());
            int connected = keys == null ? 0 : keys.size();
            if (connected > 0) {
                float u0 = 7 / 16f;
                float u1 = u0 + (2 * connected - 1) / 16f;
                renderOverlayFromAtlas(event, Textures.Block.RackTerminalServerPresence, u0, u1);
            }
        } else if (Objects.equals(item, ItemTypes.RACK_KVM)) {
            renderOverlayFromAtlas(event, Textures.Block.RackTerminalServerOn);
            Object selected = event.data.get(OCComponentsJava.RACK_KVM_SELECTED_SLOT.get());
            int slot = selected instanceof Number number ? number.intValue() : -1;
            int logicalIndex = -1;
            for (int physical = 0, logical = 0; physical < 4; physical++) {
                if (physical == event.mountable) continue;
                if (physical == slot) {
                    logicalIndex = logical;
                    break;
                }
                logical++;
            }
            if (logicalIndex >= 0) {
                float u0 = (7 + logicalIndex * 2) / 16f;
                renderOverlayFromAtlas(event, Textures.Block.RackKVMPresence, u0, u0 + 1 / 16f);
            }
        } else if (Objects.equals(item, ItemTypes.CAPACITOR)) {
            if (Boolean.TRUE.equals(event.data.get(OCComponentsJava.IS_POWERED.get()))) {
                renderOverlayFromAtlas(event, Textures.Block.RackCapacitorOn);
            }
        }
    }

    private static void renderOverlayFromAtlas(RackMountableRenderEvent.BlockEntity event,
                                               ResourceLocation texture) {
        renderOverlayFromAtlas(event, texture, 0, 1);
    }

    private static void renderOverlayFromAtlas(RackMountableRenderEvent.BlockEntity event,
                                               ResourceLocation texture, float u0, float u1) {
        var matrix = event.stack.last().pose();
        var renderer = event.typeBuffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
        var icon = Textures.getSprite(texture);
        renderer.addVertex(matrix, u0, event.v1, 0).setUv(icon.getU(u0), icon.getV(event.v1));
        renderer.addVertex(matrix, u1, event.v1, 0).setUv(icon.getU(u1), icon.getV(event.v1));
        renderer.addVertex(matrix, u1, event.v0, 0).setUv(icon.getU(u1), icon.getV(event.v0));
        renderer.addVertex(matrix, u0, event.v0, 0).setUv(icon.getU(u0), icon.getV(event.v0));
    }

    @SubscribeEvent
    public void onRackMountableRendering(RackMountableRenderEvent.Block event) {
        ItemInfo item = Items.get(event.rack.getItem(event.mountable));
        if (Objects.equals(item, ItemTypes.DISK_DRIVE)) {
            event.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackDiskDrive));
        } else if (Arrays.asList(ItemTypes.SERVERS).contains(item)) {
            event.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackServer));
        } else if (Objects.equals(item, ItemTypes.TERMINAL_SERVER)) {
            event.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackTerminalServer));
        } else if (Objects.equals(item, ItemTypes.RACK_KVM)) {
            event.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackKVM));
        } else if (Objects.equals(item, ItemTypes.CAPACITOR)) {
            event.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackCapacitor));
        }
    }
}
