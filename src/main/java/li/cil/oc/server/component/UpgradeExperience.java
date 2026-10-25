package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

public class UpgradeExperience extends AbstractManagedEnvironment implements DeviceInfo {
    private static final int MAX_LEVEL = 30;

    private final Agent host;
    private final ComponentConnector node;
    private double experience;
    private int level;

    public UpgradeExperience(Agent host) {
        this.host = host;
        this.node = Network.newNode(this, Visibility.Network)
                .withComponent("experience")
                .withConnector(30 * li.cil.oc.Settings.get().bufferPerLevel())
                .create();
        updateXpInfo();
    }

    public Agent host() { return host; }
    public int MaxLevel() { return MAX_LEVEL; }
    @Override public ComponentConnector node() { return node; }
    public double experience() { return experience; }
    public void experience_$eq(double value) { experience = value; }
    public int level() { return level; }
    public void level_$eq(int value) { level = value; }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.Generic.toString(),
                DeviceAttribute.Description, "Knowledge database",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "ERSO (Event Recorder and Self-Optimizer)",
                DeviceAttribute.Capacity, "30");
    }

    public double xpForNextLevel() {
        return li.cil.oc.util.UpgradeExperience.xpForLevel(level + 1);
    }

    public void addExperience(double value) {
        if (level < MAX_LEVEL) {
            experience += value;
            if (experience >= xpForNextLevel()) updateXpInfo();
            var world = host.getEnvironmentLevel();
            var player = host.player();
            var position = player.blockPosition();
            ExperienceOrb orb = new ExperienceOrb(world, position.getX() + 0.5D,
                    position.getY() + 0.5D, position.getZ() + 0.5D, (int) value);
            player.takeXpDelay = 0;
            orb.playerTouch(player);
        }
    }

    public void updateXpInfo() {
        int oldLevel = level;
        level = li.cil.oc.util.UpgradeExperience.calculateLevelFromExperience(experience);
        if (node != null) {
            if (level != oldLevel) updateClient();
            node.setLocalBufferSize(li.cil.oc.Settings.get().bufferPerLevel() * level);
        }
    }

    @Callback(direct = true, doc = "function():number -- The current level of experience stored in this experience upgrade.")
    public Object[] level(Context context, Arguments args) {
        return ResultWrapper.result(li.cil.oc.util.UpgradeExperience.calculateExperienceLevel(level, experience));
    }

    @Callback(doc = "function():boolean -- Tries to consume an enchanted item to add experience to the upgrade.")
    public Object[] consume(Context context, Arguments args) {
        if (level >= MAX_LEVEL) return ResultWrapper.result(null, "max level");
        var inventory = host.mainInventory();
        var stack = inventory.getItem(host.selectedSlot());
        if (stack.isEmpty()) return ResultWrapper.result(null, "no item");

        int xp = 0;
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            xp += 3 + host.getEnvironmentLevel().getRandom().nextInt(5)
                    + host.getEnvironmentLevel().getRandom().nextInt(5);
        }
        else {
            for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
                var enchantment = entry.getKey().value();
                int enchantmentLevel = entry.getIntValue();
                if (enchantment != null) xp += enchantment.getMinCost(enchantmentLevel);
            }
            if (xp <= 0) return ResultWrapper.result(null, "could not extract experience from item");
        }

        var consumed = inventory.removeItem(host.selectedSlot(), 1);
        if (consumed.isEmpty()) return ResultWrapper.result(null, "could not consume item");
        addExperience(xp * li.cil.oc.Settings.get().constantXpGrowth());
        return ResultWrapper.result(true);
    }

    private void updateClient() {
        if (host instanceof Robot robot) {
            robot.synchronizeSlot(robot.componentSlot(node.address()));
        }
    }

    @Override
    public void saveData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveData(nbt, provider);
        li.cil.oc.util.UpgradeExperience.setExperience(nbt, experience);
    }

    @Override
    public void loadData(CompoundTag nbt, HolderLookup.Provider provider) throws UnrecoverablePersistanceException {
        super.loadData(nbt, provider);
        experience = li.cil.oc.util.UpgradeExperience.getExperience(nbt);
        updateXpInfo();
    }
}
