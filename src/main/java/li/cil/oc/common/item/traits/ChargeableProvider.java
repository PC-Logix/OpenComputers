package li.cil.oc.common.item.traits;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class ChargeableProvider implements IEnergyStorage {
    private final ItemStack stack;
    private final Chargeable item;

    public ChargeableProvider(ItemStack stack, Chargeable item) {
        this.stack = stack;
        this.item = item;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        double requested = ChargeableUtils.convertForgeEnergyToOpenComputers(maxReceive);
        double unused = item.charge(stack, requested, simulate);
        return maxReceive - ChargeableUtils.convertOpenComputersToForgeEnergy(unused);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return canExtract() ? -receiveEnergy(-maxExtract, simulate) : 0;
    }

    @Override
    public int getEnergyStored() {
        return ChargeableUtils.convertOpenComputersToForgeEnergy(item.getCharge(stack));
    }

    @Override
    public int getMaxEnergyStored() {
        return ChargeableUtils.convertOpenComputersToForgeEnergy(item.maxCharge(stack));
    }

    @Override
    public boolean canExtract() {
        return item.canExtract(stack);
    }

    @Override
    public boolean canReceive() {
        return item.canCharge(stack);
    }
}
