package li.cil.oc.common.blockentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.common.blockentity.traits.Tickable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class CarpetedCapacitor extends Capacitor implements Tickable {
    private final Random random = new Random();
    private final double chance = Settings.get().carpetDamageChance();
    private long nextChanceTime;

    public CarpetedCapacitor(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.CARPETED_CAPACITOR.get(), pos, state);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put(DeviceAttribute.Class, DeviceClass.Power);
        info.put(DeviceAttribute.Description, "Battery");
        info.put(DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor());
        info.put(DeviceAttribute.Product, "CarpetedCapBank3x");
        info.put(DeviceAttribute.Capacity, Double.toString(maxCapacity()));
        return info;
    }

    private double energyFromGroup(List<LivingEntity> entities, double power) {
        if (entities.size() < 2) return 0;

        if (chance > 0 && nextChanceTime < getLevel().getGameTime()) {
            for (LivingEntity entity : entities) {
                if (scala.util.Random$.MODULE$.nextDouble() < chance) {
                    entity.hurt(getLevel().damageSources().generic(), 1);
                    entity.setLastHurtByMob(entity);
                    entity.knockback(0, 0.25, 0);
                    nextChanceTime = getLevel().getGameTime() + 20 * 60;
                    break;
                }
            }
        }
        return power;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (node() != null && (getLevel().getGameTime() + hashCode()) % 20 == 0) {
            AABB bounds = new AABB(getBlockPos().relative(Direction.UP));
            List<LivingEntity> entities = getLevel().getEntitiesOfClass(LivingEntity.class, bounds).stream()
                .filter(LivingEntity::isAlive)
                .toList();
            double sheepPower = energyFromGroup(entities.stream().filter(Sheep.class::isInstance).toList(), Settings.get().sheepPower());
            double ocelotPower = energyFromGroup(entities.stream().filter(entity -> entity instanceof Ocelot || entity instanceof Cat).toList(), Settings.get().ocelotPower());
            double totalPower = sheepPower + ocelotPower;
            if (totalPower > 0) node().changeBuffer(totalPower);
        }
    }
}
