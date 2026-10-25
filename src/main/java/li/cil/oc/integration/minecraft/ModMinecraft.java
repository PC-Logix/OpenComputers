package li.cil.oc.integration.minecraft;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.util.BlockPosition;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.neoforged.neoforge.common.NeoForge;

public final class ModMinecraft implements ModProxy, BundledRedstone.RedstoneProvider {
    public static final ModMinecraft INSTANCE = new ModMinecraft();

    private ModMinecraft() {}

    @Override
    public Mod getMod() { return Mods.Minecraft(); }

    @Override
    public void initialize() {
        Driver.add(DriverBeacon.INSTANCE);
        Driver.add(DriverBrewingStand.INSTANCE);
        Driver.add(DriverComparator.INSTANCE);
        Driver.add(DriverFurnace.INSTANCE);
        Driver.add(DriverBlastFurnace.INSTANCE);
        Driver.add(DriverSmoker.INSTANCE);
        Driver.add(DriverMobSpawner.INSTANCE);
        Driver.add(DriverNoteBlock.INSTANCE);
        Driver.add(DriverRecordPlayer.INSTANCE);

        Driver.add(DriverBeacon.PROVIDER);
        Driver.add(DriverBrewingStand.PROVIDER);
        Driver.add(DriverComparator.PROVIDER);
        Driver.add(DriverFurnace.PROVIDER);
        Driver.add(DriverBlastFurnace.PROVIDER);
        Driver.add(DriverSmoker.PROVIDER);
        Driver.add(DriverMobSpawner.PROVIDER);
        Driver.add(DriverNoteBlock.PROVIDER);
        Driver.add(DriverRecordPlayer.PROVIDER);

        if (Settings.get().enableInventoryDriver()) Driver.add(new DriverInventory());
        if (Settings.get().enableTankDriver()) {
            Driver.add(new DriverFluidHandler());
            Driver.add(new DriverFluidTank());
        }
        if (Settings.get().enableCommandBlockDriver()) Driver.add(DriverCommandBlock.INSTANCE);

        Driver.add(ConverterFluidContainerItem.INSTANCE);
        Driver.add(ConverterFluidStack.INSTANCE);
        Driver.add(ConverterFluidTankInfo.INSTANCE);
        Driver.add(ConverterFluidTankProperties.INSTANCE);
        Driver.add(ConverterItemStack.INSTANCE);
        Driver.add(ConverterNBT.INSTANCE);
        Driver.add(ConverterLevel.INSTANCE);

        BundledRedstone.addProvider(this);
        NeoForge.EVENT_BUS.register(EventHandlerVanilla.INSTANCE);
    }

    @Override
    public int computeInput(BlockPosition pos, Direction side) {
        var world = pos.worldOptional().orElseThrow();
        var neighbor = pos.offset(side).toBlockPos();
        int signal = Math.max(world.getDirectSignal(neighbor, side), world.getSignal(neighbor, side));
        var state = world.getBlockState(neighbor);
        int wire = state.getBlock() == Blocks.REDSTONE_WIRE ? state.getValue(RedStoneWireBlock.POWER) : 0;
        return Math.max(signal, wire);
    }

    @Override
    public int[] computeBundledInput(BlockPosition pos, Direction side) { return null; }
}
