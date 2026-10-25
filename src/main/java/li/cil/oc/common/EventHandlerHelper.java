package li.cil.oc.common;

import li.cil.oc.common.init.OCItemsUpgradesCatalogJava;


import li.cil.oc.api.internal.Colored;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedComponent;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.common.capabilities.CapabilitySidedComponent;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.item.traits.Chargeable;
import li.cil.oc.common.item.traits.ChargeableProvider;
import li.cil.oc.common.capabilities.TankUpgradeFluidHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

public final class EventHandlerHelper {
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // NeoForge 1.21 no longer discovers capabilities implemented directly by
        // block entities. Register providers for every OC block entity type so
        // the network can discover adjacent environments again.
        BlockEntityTypes.BLOCK_ENTITY_TYPES.getEntries().forEach(type ->
                registerOpenComputersCapabilities(event, type.get()));

        /*
        event.registerBlockEntity(NeoCapabilities.FluidHandler.BLOCK, TileEntityTypes.ROBOT.get(),
      (be, _) => be match {
        case fh: IFluidHandler => fh
        case _ => null
      })
         */
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityTypes.ROBOT.get(),
                (be, ignored) -> be
        );
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, ignored) -> new TankUpgradeFluidHandler(stack),
                OCItemsUpgradesCatalogJava.TankUpgrade.get()
        );
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item instanceof Chargeable chargeable) {
                event.registerItem(
                        Capabilities.EnergyStorage.ITEM,
                        (stack, ignored) -> new ChargeableProvider(stack, chargeable),
                        item
                );
            }
        });
    }

    private static <BE extends BlockEntity> void registerOpenComputersCapabilities(
            RegisterCapabilitiesEvent event, BlockEntityType<BE> type) {
        event.registerBlockEntity(
                li.cil.oc.common.Capabilities.EnvironmentCapability,
                type,
                (blockEntity, side) -> blockEntity instanceof Environment environment ? environment : null
        );
        event.registerBlockEntity(
                li.cil.oc.common.Capabilities.SidedEnvironmentCapability,
                type,
                (blockEntity, side) -> {
                    if (blockEntity instanceof SidedEnvironment environment) {
                        return environment;
                    }
                    if (blockEntity instanceof Environment environment && blockEntity instanceof SidedComponent) {
                        return new CapabilitySidedComponent.SidedEnvironmentAdapter(environment);
                    }
                    return null;
                }
        );
        event.registerBlockEntity(
                li.cil.oc.common.Capabilities.ColoredCapability,
                type,
                (blockEntity, side) -> blockEntity instanceof Colored colored ? colored : null
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (blockEntity, side) -> itemHandler(blockEntity, side)
        );
    }

    private static IItemHandler itemHandler(BlockEntity blockEntity, Direction side) {
        if (!(blockEntity instanceof Container inventory)) {
            return null;
        }
        if (inventory instanceof WorldlyContainer sidedInventory && side != null) {
            return new SidedInvWrapper(sidedInventory, side);
        }
        return new InvWrapper(inventory);
    }
}
