package li.cil.oc.server.agent;

import com.mojang.datafixers.util.Either;
import li.cil.oc.api.internal.Agent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player.BedSleepingProblem;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import scala.Option;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/** Fake player representing an autonomous agent. */
public class Player extends FakePlayer {
    private final Agent agent;
    public final Abilities abilities;
    private Direction facing = Direction.SOUTH;
    private Direction side = Direction.SOUTH;

    public Player(Agent agent) {
        super((ServerLevel) agent.getEnvironmentLevel(), PlayerSupport.profileFor(agent));
        this.agent = agent;
        this.abilities = getAbilities();
        abilities.mayfly = true;
        abilities.invulnerable = true;
        abilities.flying = true;
        setOnGround(true);
        refreshDimensions();
        this.inventory = new Inventory(this, agent);
        this.inventoryMenu = new InventoryMenu(inventory, !level().isClientSide(), this);
        this.containerMenu = inventoryMenu;
    }

    public Agent agent() { return agent; }
    public Direction facing() { return facing; }
    public void facing_$eq(Direction facing) { this.facing = facing; }
    public Direction side() { return side; }
    public void side_$eq(Direction side) { this.side = side; }
    public Abilities abilities() { return abilities; }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return new EntityDimensions(1, 1, 1, EntityAttachments.createDefault(1, 1), true);
    }

    @Override
    public Component getName() {
        return Component.literal(agent.name());
    }

    public <T extends Entity> Option<Entity> closestEntity(Class<T> entityClass) {
        return closestEntity(entityClass, facing);
    }

    public <T extends Entity> Direction closestEntity$default$2(Class<T> entityClass) { return facing; }

    public <T extends Entity> Option<Entity> closestEntity(Class<T> entityClass, Direction side) {
        return PlayerSupport.closestEntity(this, agent, entityClass, side);
    }

    public <T extends Entity> Optional<Entity> closestEntityJava(Class<T> entityClass, Direction side) {
        return PlayerSupport.closestEntityJava(this, agent, entityClass, side);
    }

    public <T extends Entity> List<T> entitiesOnSide(Class<T> entityClass, Direction side) {
        return PlayerSupport.entitiesOnSide(this, agent, entityClass, side);
    }

    public <T extends Entity> List<T> entitiesInBlock(Class<T> entityClass, li.cil.oc.util.BlockPosition blockPos) {
        return PlayerSupport.entitiesInBlock(this, entityClass, blockPos);
    }

    @Override
    public void attack(Entity entity) {
        PlayerInteractionEvents.attack(this, agent, entity, () -> super.attack(entity));
    }

    @Override
    public InteractionResult interactOn(Entity entity, InteractionHand hand) {
        return PlayerInteractionEvents.interactOn(this, agent, entity, hand);
    }

    public ActivationType activateBlockOrUseItem(BlockPos pos, Direction side, float hitX, float hitY,
                                                   float hitZ, double duration) {
        return PlayerInteractionEvents.activateBlockOrUseItem(this, agent, pos, side, hitX, hitY, hitZ, duration);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        PlayerSupport.setItemSlot(this, agent, slot, stack, () -> super.setItemSlot(slot, stack));
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return PlayerSupport.getItemBySlot(this, agent, slot, () -> super.getItemBySlot(slot));
    }

    public PlayerInteractEvent.RightClickBlock fireRightClickBlock(BlockPos pos, Direction side) {
        return PlayerInteractionEvents.fireRightClickBlock(this, pos, side);
    }

    public PlayerInteractEvent.LeftClickBlock fireLeftClickBlock(BlockPos pos, Direction side) {
        return PlayerInteractionEvents.fireLeftClickBlock(this, pos, side);
    }

    public PlayerInteractEvent.RightClickItem fireRightClickAir() {
        return PlayerInteractionEvents.fireRightClickAir(this);
    }

    public boolean useItemWithHand(double duration, ItemStack stack) {
        return PlayerInteractionEvents.useItemWithHand(this, agent, duration, stack);
    }

    public Option<ItemStack> useEquippedItem$default$2() { return Option.empty(); }

    public boolean useEquippedItem(double duration) {
        return PlayerInteractionEvents.useEquippedItem(this, agent, duration, Option.empty());
    }

    public boolean useEquippedItem(double duration, Option<ItemStack> stackOption) {
        return PlayerInteractionEvents.useEquippedItem(this, agent, duration, stackOption);
    }

    public boolean placeBlock(int slot, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        return PlayerInteractionEvents.placeBlock(this, agent, slot, pos, side, hitX, hitY, hitZ);
    }

    public double clickBlock(BlockPos pos, Direction side) {
        return PlayerBlockBreaking.clickBlock(this, agent, pos, side);
    }

    @Override
    public ItemEntity drop(ItemStack stack, boolean dropAround, boolean traceItem) {
        return PlayerSupport.drop(this, agent, stack, dropAround);
    }

    @Override
    public void causeFoodExhaustion(float amount) {
        PlayerSupport.causeFoodExhaustion(agent, amount);
    }

    @Override public void closeContainer() { }
    @Override public void swing(InteractionHand hand) { }

    @Override
    protected int getPermissionLevel() {
        return PlayerSupport.permissionLevel(this);
    }

    @Override
    public boolean canHarmPlayer(net.minecraft.world.entity.player.Player player) {
        return PlayerSupport.canHarmPlayer();
    }

    @Override public boolean canEat(boolean value) { return false; }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
    @Override public boolean doHurtTarget(Entity entity) { return false; }
    @Override public boolean hurt(DamageSource source, float damage) { return false; }
    @Override public void heal(float amount) { }
    @Override public void setHealth(float value) { }
    @Override public void remove(RemovalReason reason) { super.remove(RemovalReason.KILLED); }
    @Override public void aiStep() { }
    @Override public void take(Entity entity, int count) { }
    @Override public void setLastHurtByMob(LivingEntity entity) { }
    @Override public void setLastHurtMob(Entity entity) { }
    @Override public boolean startRiding(Entity entity, boolean force) { return false; }

    @Override
    public Either<BedSleepingProblem, net.minecraft.util.Unit> startSleepInBed(BlockPos bedLocation) {
        return Either.left(BedSleepingProblem.OTHER_PROBLEM);
    }

    @Override public void sendSystemMessage(Component message) { }
    @Override public void openCommandBlock(CommandBlockEntity commandBlock) { }
    @Override public void sendMerchantOffers(int containerId, MerchantOffers offers, int villagerLevel,
                                               int villagerXP, boolean showProgress, boolean canRestock) { }
    @Override public OptionalInt openMenu(MenuProvider guiOwner) { return OptionalInt.empty(); }
    @Override public void openMinecartCommandBlock(BaseCommandBlock commandBlock) { }
    @Override public void openTextEdit(SignBlockEntity signTile, boolean isFront) { }
}
