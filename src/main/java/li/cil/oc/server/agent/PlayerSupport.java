package li.cil.oc.server.agent;

import com.mojang.authlib.GameProfile;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.event.RobotExhaustionEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.Connector;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import scala.Option;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Shared helpers for the fake player representing an autonomous agent. */
public final class PlayerSupport {
    private PlayerSupport() { }

    public static GameProfile profileFor(Agent agent) {
        String randomId = Integer.toString(agent.getEnvironmentLevel().getRandom().nextInt(0xFFFFFF) + 1);
        String name = Settings.get().nameFormat().replace("$player$", agent.ownerName()).replace("$random$", randomId);
        return new GameProfile(agent.ownerUUID(), name);
    }

    public static UUID determineUUID(Option<UUID> playerUUID) {
        String format = Settings.get().uuidFormat();
        UUID randomUUID = UUID.randomUUID();
        try {
            return UUID.fromString(format.replace("$random$", randomUUID.toString())
                    .replace("$player$", playerUUID.isDefined() ? playerUUID.get().toString() : randomUUID.toString()));
        } catch (Throwable error) {
            OpenComputers.log().warn("Failed determining robot UUID, check your config's `uuidFormat` entry!", error);
            return randomUUID;
        }
    }

    public static <T extends Entity> Option<Entity> closestEntity(Player player, Agent agent, Class<T> entityClass,
                                                                   Direction side) {
        Optional<Entity> closest = closestEntityJava(player, agent, entityClass, side);
        return closest.isPresent() ? Option.apply(closest.get()) : Option.empty();
    }

    public static <T extends Entity> Optional<Entity> closestEntityJava(Player player, Agent agent,
                                                                         Class<T> entityClass, Direction side) {
        AABB bounds = BlockPositionsJava.apply(agent).offset(side).bounds();
        List<T> candidates = player.level().getEntitiesOfClass(entityClass, bounds, entity -> true);
        T closest = candidates.stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        return Optional.ofNullable(closest);
    }

    public static <T extends Entity> List<T> entitiesOnSide(Player player, Agent agent, Class<T> entityClass,
                                                              Direction side) {
        return entitiesInBlock(player, entityClass, BlockPositionsJava.apply(agent).offset(side));
    }

    public static <T extends Entity> List<T> entitiesInBlock(Player player, Class<T> entityClass,
                                                               BlockPosition blockPosition) {
        return player.level().getEntitiesOfClass(entityClass, blockPosition.bounds(), entity -> true);
    }

    public static void setItemSlot(Player player, Agent agent, EquipmentSlot slot, ItemStack stack, Runnable vanillaSetter) {
        Runnable setter = vanillaSetter;
        if (slot == EquipmentSlot.MAINHAND) {
            agent.equipmentInventory().setItem(0, stack);
            setter = () -> {
                int selected = player.getInventory().selected;
                if (selected < 0) player.getInventory().selected = ~selected;
                vanillaSetter.run();
                player.getInventory().selected = selected;
            };
        } else if (slot == EquipmentSlot.OFFHAND) {
            player.getInventory().offhand.set(0, stack);
        }
        setter.run();
    }

    public static ItemStack getItemBySlot(Player player, Agent agent, EquipmentSlot slot,
                                           Supplier<ItemStack> vanillaGetter) {
        if (slot == EquipmentSlot.MAINHAND) return agent.equipmentInventory().getItem(0);
        if (slot == EquipmentSlot.OFFHAND) return player.getInventory().offhand.get(0);
        return vanillaGetter.get();
    }

    public static ItemEntity drop(Player player, Agent agent, ItemStack stack, boolean dropAround) {
        java.util.Optional<Direction> direction = dropAround ? java.util.Optional.empty() : java.util.Optional.of(player.facing());
        return InventoryUtils.spawnStackInWorld(BlockPositionsJava.apply(agent), stack, direction);
    }

    public static void causeFoodExhaustion(Agent agent, float amount) {
        double cost = Settings.get().robotExhaustionCost();
        if (cost > 0 && agent.machine().node() instanceof Connector connector) {
            connector.changeBuffer(-cost * amount);
        }
        NeoForge.EVENT_BUS.post(new RobotExhaustionEvent(agent, amount));
    }

    public static int permissionLevel(Player player) {
        var server = player.getServer();
        var playerList = server.getPlayerList();
        if (!playerList.isOp(player.getGameProfile())) return 0;
        ServerOpListEntry operator = playerList.getOps().get(player.getGameProfile());
        return operator != null ? operator.getLevel() : server.getOperatorUserPermissionLevel();
    }

    public static boolean canHarmPlayer() {
        return Settings.get().canAttackPlayers();
    }

    public static void updatePositionAndRotation(Player player, Direction facing, Direction side) {
        player.facing_$eq(facing);
        player.side_$eq(side);
        Vec3 direction = new Vec3(facing.getStepX() + side.getStepX(), facing.getStepY() + side.getStepY(),
                facing.getStepZ() + side.getStepZ()).normalize();
        float yaw = (float) Math.toDegrees(-Math.atan2(direction.x, direction.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(direction.y,
                Math.sqrt(direction.x * direction.x + direction.z * direction.z))) * 0.99f;
        player.setPos(player.agent().xPosition(), player.agent().yPosition(), player.agent().zPosition());
        player.setYRot(pitch % 360f);
        player.setXRot(yaw % 360f);
        player.xRotO = player.getXRot();
        player.yRotO = player.getYRot();
    }

    public static void setPlayerInventoryItems(Player player) {
        Agent agent = player.agent();
        for (int i = 0; i < 4; i++) copyOrEmpty(player.getInventory().armor, agent.equipmentInventory(), i);
        int size = Math.min(player.getInventory().items.size(), agent.mainInventory().getContainerSize());
        for (int i = 0; i < size; i++) copyOrEmpty(player.getInventory().items, agent.mainInventory(), i);
        player.inventoryMenu.broadcastChanges();
    }

    private static void copyOrEmpty(NonNullList<ItemStack> inventory, Container source, int slot) {
        ItemStack item = source.getItem(slot);
        inventory.set(slot, item == null ? ItemStack.EMPTY : item.copy());
    }

    public static void detectPlayerInventoryChanges(Player player) {
        Agent agent = player.agent();
        player.inventoryMenu.broadcastChanges();
        for (int i = 0; i < 4; i++) copyIfChanged(agent.equipmentInventory(), i, player.getInventory().armor.get(i));
        int size = Math.min(player.getInventory().items.size(), agent.mainInventory().getContainerSize());
        for (int i = 0; i < size; i++) copyIfChanged(agent.mainInventory(), i, player.getInventory().items.get(i));
    }

    private static void copyIfChanged(Container inventory, int slot, ItemStack item) {
        ItemStack value = item == null ? ItemStack.EMPTY : item.copy();
        if (!ItemStack.matches(value, inventory.getItem(slot))) inventory.setItem(slot, value);
    }
}
