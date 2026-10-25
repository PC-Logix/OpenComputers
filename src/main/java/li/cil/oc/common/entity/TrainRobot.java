package li.cil.oc.common.entity;

import li.cil.oc.common.blockentity.RobotProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import scala.Option;

/**
 * A deliberately inert robot-shaped mob for Create train conductor seats.
 *
 * It is a Mob so vanilla lead interaction can target it. Create only needs a
 * non-player passenger in a conductor seat, so there is no computer, AI, or
 * other robot functionality attached to this entity.
 */
public class TrainRobot extends Mob implements IEntityWithComplexSpawn {
    private static final String ROBOT_FLAG_TAG = "RobotFlag";

    private Option<ResourceLocation> robotFlag = Option.empty();

    public TrainRobot(EntityType<? extends TrainRobot> type, Level level) {
        super(type, level);
        setNoAi(true);
    }

    public static boolean isHat(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.HEAD;
    }

    public static boolean replaceRobot(Level level, BlockPos pos, float yaw, RobotProxy proxy) {
        TrainRobot entity = EntityTypes.TRAIN_ROBOT.get().create(level);
        if (entity == null) return false;

        proxy.robot().node().remove();
        proxy.robot().saveComponents();
        if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) return false;

        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setRobotFlag(proxy.robot().info().flag());
        entity.getPersistentData().putBoolean("TrainHat", true);
        level.addFreshEntity(entity);
        return true;
    }

    public Option<ResourceLocation> flag() {
        return robotFlag;
    }

    public void setRobotFlag(Option<ResourceLocation> value) {
        robotFlag = value;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(robotFlag.map(ResourceLocation::toString).getOrElse(() -> ""));
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        String value = buffer.readUtf();
        setRobotFlag(Option.apply(value.isEmpty() ? null : ResourceLocation.tryParse(value)));
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag nbt) {
        setRobotFlag(nbt.contains(ROBOT_FLAG_TAG)
            ? Option.apply(ResourceLocation.tryParse(nbt.getString(ROBOT_FLAG_TAG)))
            : Option.empty());
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag nbt) {
        if (robotFlag.isDefined()) nbt.putString(ROBOT_FLAG_TAG, robotFlag.get().toString());
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (isPassenger()) faceMotion();
    }

    private void faceMotion() {
        Entity vehicle = getVehicle();
        if (vehicle == null) return;

        Vec3 delta = vehicle.position().subtract(vehicle.xo, vehicle.yo, vehicle.zo);
        Vec3 horizontal = new Vec3(delta.x, 0, delta.z);
        if (horizontal.lengthSqr() > 0.0001) {
            float yaw = (float) -Math.toDegrees(Math.atan2(horizontal.x, horizontal.z));
            setYRot(yaw);
            setYHeadRot(yaw);
        }
    }

    @Override
    public void closeRangeLeashBehaviour(Entity holder) {
        moveToward(holder, 0.12);
    }

    @Override
    public void elasticRangeLeashBehaviour(Entity holder, float distance) {
        moveToward(holder, Math.min(0.45, 0.12 + (distance - 6.0) * 0.05));
    }

    @Override
    public void leashTooFarBehaviour() {
        Entity holder = getLeashHolder();
        if (holder != null) elasticRangeLeashBehaviour(holder, distanceTo(holder));
    }

    private void moveToward(Entity holder, double speed) {
        Vec3 delta = holder.position().subtract(position());
        Vec3 horizontal = new Vec3(delta.x, 0, delta.z);
        if (horizontal.lengthSqr() > 0.01) {
            Vec3 direction = horizontal.normalize();
            Vec3 current = getDeltaMovement();
            Vec3 pull = direction.scale(Math.min(speed, horizontal.length() * 0.5));
            move(MoverType.SELF, pull);
            setDeltaMovement(0, current.y, 0);
            if (!isPassenger()) {
                setYRot((float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90));
                setYHeadRot(getYRot());
            }
        } else {
            Vec3 current = getDeltaMovement();
            setDeltaMovement(0, current.y, 0);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public Component getName() {
        return Component.translatable("entity.oc.TrainRobot");
    }
}
