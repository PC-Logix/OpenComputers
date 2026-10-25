package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers$;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.Machine;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.CPU;
import li.cil.oc.server.machine.luac.NativeLuaArchitecture;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Collection;
import java.util.Arrays;
import java.util.Objects;

public class DriverCPU implements Item, MutableProcessor, CallBudget {
    public static final DriverCPU INSTANCE = new DriverCPU();

    public DriverCPU() {}

    private boolean matchesAny(ItemStack stack, ItemInfo... infos) {
        var item = Items.get(stack);
        return Arrays.stream(infos).filter(Objects::nonNull).anyMatch(info -> Objects.equals(item, info));
    }

    @Override public boolean worksWith(ItemStack stack) {
        return matchesAny(stack,
                Items.get(Constants.ItemName$.MODULE$.CPUTier1()),
                Items.get(Constants.ItemName$.MODULE$.CPUTier2()),
                Items.get(Constants.ItemName$.MODULE$.CPUTier3()),
                Items.get(Constants.ItemName$.MODULE$.CPUTier4()));
    }
    @Override public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, li.cil.oc.api.network.EnvironmentHost host) {
        return new li.cil.oc.server.component.CPU(tier(stack));
    }
    @Override public String slot(ItemStack stack) { return Slot.CPU; }
    @Override public int tier(ItemStack stack) { return cpuTier(stack); }
    public int cpuTier(ItemStack stack) {
        return stack.getItem() instanceof CPU cpu ? cpu.cpuTier() : Tier.One;
    }
    @Override public int supportedComponents(ItemStack stack) {
        return Settings.get().cpuComponentSupport()[cpuTier(stack)];
    }
    @Override public Collection<Class<? extends Architecture>> allArchitectures() {
        return Machine.architectures();
    }
    @Override public Class<? extends Architecture> architecture(ItemStack stack) {
        CompoundTag tag = ItemUtils.getTag(stack);
        if (tag != null) {
            String name = tag.getString(Settings.namespace() + "archClass");
            if (name.equals(NativeLuaArchitecture.class.getName())) {
                name = Machine.LuaArchitecture.getName();
            }
            if (!name.isEmpty()) {
                try {
                    return Class.forName(name).asSubclass(Architecture.class);
                }
                catch (Throwable error) {
                    OpenComputers$.MODULE$.log().warn("Failed getting class for CPU architecture. Resetting CPU to use the default.", error);
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
                        data.remove(Settings.namespace() + "archClass");
                        data.remove(Settings.namespace() + "archName");
                    });
                }
            }
        }
        return Machine.architectures().stream().findFirst().orElse(null);
    }
    @Override public void setArchitecture(ItemStack stack, Class<? extends Architecture> architecture) {
        if (!worksWith(stack)) throw new IllegalArgumentException("Unsupported processor type.");
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            data.putString(Settings.namespace() + "archClass", architecture.getName());
            data.putString(Settings.namespace() + "archName", Machine.getArchitectureName(architecture));
        });
    }
    @Override public double getCallBudget(ItemStack stack) {
        int tier = Math.max(Tier.One, Math.min(tier(stack), Tier.Four));
        return Settings.get().callBudgets()[tier];
    }
}
