package li.cil.oc.client;

import li.cil.oc.common.init.OCItemsStorageCatalogJava;

import li.cil.oc.common.init.OCItemsHardwareCatalogJava;

import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.internal.Colored;
import li.cil.oc.common.block.Case;
import li.cil.oc.common.block.ChameliumBlock;
import li.cil.oc.common.block.Screen;
import li.cil.oc.common.blockentity.Cable;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.integration.Mods;
import li.cil.oc.integration.multipart.ModCBMultipart;
import li.cil.oc.util.Color;
import li.cil.oc.util.ItemColorizer;
import li.cil.oc.util.ItemUtils;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class ColorHandler {
    public static final ColorHandler INSTANCE = new ColorHandler();

    private ColorHandler() {}

    @SubscribeEvent
    public void onRegisterBlocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, world, pos, tintIndex) -> {
            if (pos == null) return 0xFFFFFFFF;
            if (world.getBlockEntity(pos) instanceof Cable cable) return FastColor.ARGB32.opaque(cable.getColor());
            if (Mods.CBMultipart().isModAvailable()) {
                return ModCBMultipart.cableColor(world, pos)
                        .map(FastColor.ARGB32::opaque)
                        .orElse(0xFFFFFFFF);
            }
            return 0xFFFFFFFF;
        }, OCBlocks.Cable().get());

        event.register((state, world, pos, tintIndex) -> {
            if (pos != null && world.getBlockEntity(pos) instanceof Colored colored) return colored.getColor();
            if (state.getBlock() instanceof Case caseBlock) return Color.byTier(caseBlock.tier());
            return 0xFFFFFFFF;
        }, OCBlocks.CaseTier1().get(), OCBlocks.CaseTier2().get(), OCBlocks.CaseTier3().get(),
                OCBlocks.CaseTier4().get(), OCBlocks.CaseCreative().get());

        event.register((state, world, pos, tintIndex) -> Color.rgbValues(state.getValue(ChameliumBlock.COLOR)),
                OCBlocks.ChameliumBlock().get());
        event.register((state, world, pos, tintIndex) -> tintIndex, OCBlocks.Print().get());
        event.register((state, world, pos, tintIndex) -> state.getBlock() instanceof Screen screen
                        ? Color.byTier(screen.tier()) : 0xFFFFFFFF,
                OCBlocks.ScreenTier1().get(), OCBlocks.ScreenTier2().get(), OCBlocks.ScreenTier3().get());
    }

    @SubscribeEvent
    public void onRegisterItems(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> FastColor.ARGB32.opaque(
                ItemColorizer.hasColor(stack) ? ItemColorizer.getColor(stack) : Color.rgbValues(DyeColor.LIGHT_GRAY)),
                OCBlocks.Cable().get());

        event.register((stack, tintIndex) -> FastColor.ARGB32.opaque(Color.byTier(ItemUtils.caseTier(stack))),
                OCBlocks.CaseTier1().get(), OCBlocks.CaseTier2().get(), OCBlocks.CaseTier3().get(),
                OCBlocks.CaseTier4().get(), OCBlocks.CaseCreative().get());

        event.register((stack, tintIndex) -> FastColor.ARGB32.opaque(Color.rgbValues(
                        stack.getOrDefault(OCComponentsJava.CHAMELIUM_COLOR.get(), ChameliumBlock.DEFAULT_COLOR))),
                OCBlocks.ChameliumBlock().get());

        event.register((stack, tintIndex) -> 0xFFFFFFFF,
                OCBlocks.ScreenTier1().get(), OCBlocks.ScreenTier2().get(), OCBlocks.ScreenTier3().get(),
                OCBlocks.Print().get(), OCBlocks.Robot().get());

        event.register((stack, tintIndex) -> tintIndex == 1
                        ? FastColor.ARGB32.opaque(ItemColorizer.hasColor(stack) ? ItemColorizer.getColor(stack) : 0x66DD55)
                        : 0xFFFFFFFF,
                OCItemsHardwareCatalogJava.HoverBoots.get());

        event.register((stack, tintIndex) -> tintIndex == 1
                        ? stack.getOrDefault(OCComponentsJava.DISK_COLOR.get(), DyeColor.GRAY).getTextureDiffuseColor()
                        : 0xFFFFFFFF,
                OCItemsStorageCatalogJava.Floppy.get());
    }
}
