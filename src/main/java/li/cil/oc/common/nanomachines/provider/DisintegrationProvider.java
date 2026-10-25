package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.api.prefab.AbstractProvider;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DisintegrationProvider extends AbstractProvider {
    public static final DisintegrationProvider INSTANCE = new DisintegrationProvider();

    private DisintegrationProvider() { super("c4e7e3c2-8069-4fbb-b08e-74b1bddcdfe7"); }

    @Override
    public Iterable<Behavior> createBehaviors(Player player) {
        return List.of(new DisintegrationBehavior(player));
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        return new DisintegrationBehavior(player);
    }

    public static final class DisintegrationBehavior extends AbstractBehavior {
        private Map<BlockPosition, SlowBreakInfo> breakingMap = new HashMap<>();
        private Map<BlockPosition, SlowBreakInfo> breakingMapNew = new HashMap<>();

        public DisintegrationBehavior(Player player) { super(player); }

        @Override
        public void onDisable(DisableReason reason) {
            Level world = player.level();
            for (BlockPosition pos : breakingMap.keySet()) {
                world.destroyBlockProgress(pos.hashCode(), pos.toBlockPos(), -1);
            }
            breakingMap.clear();
        }

        @Override
        public void update() {
            Level world = player.level();
            if (world.isClientSide() || player instanceof FakePlayer || !(player instanceof ServerPlayer serverPlayer)) return;

            long now = world.getGameTime();
            BlockPosition origin = BlockPositionsJava.apply(player);
            int range = Settings.get().nanomachineDisintegrationRange()
                    * Nanomachines.getController(player).getInputCount(this);

            for (int x = -range; x <= range; x++) {
                for (int y = 0; y <= range * 2; y++) {
                    for (int z = -range; z <= range; z++) {
                        BlockPosition pos = BlockPositionsJava.apply(origin.offset(x, y, z));
                        SlowBreakInfo info = breakingMap.get(pos);
                        if (info != null) {
                            if (info.checkTool(player)) {
                                breakingMapNew.put(pos, info);
                                info.update(world, now);
                            }
                            continue;
                        }

                        var event = new PlayerInteractEvent.LeftClickBlock(player, pos.toBlockPos(),
                                player.getDirection(), PlayerInteractEvent.LeftClickBlock.Action.START);
                        NeoForge.EVENT_BUS.post(event);
                        boolean allowed = !event.isCanceled() && event.getUseBlock() != TriState.FALSE
                                && event.getUseItem() != TriState.FALSE;
                        boolean placingRestricted = !(world.getLevelData() instanceof ServerLevelData data)
                                || data.getGameType().isBlockPlacingRestricted();
                        boolean adventureOk = !placingRestricted || player.mayUseItemAt(pos.toBlockPos(), null,
                                player.getItemInHand(InteractionHand.MAIN_HAND));
                        if (!allowed || !adventureOk || world.isEmptyBlock(pos.toBlockPos())) continue;

                        BlockState state = world.getBlockState(pos.toBlockPos());
                        float hardness = state.getDestroyProgress(player, world, pos.toBlockPos());
                        if (hardness <= 0) continue;
                        int timeToBreak = (int) (1 / hardness);
                        if (timeToBreak >= 20 * 30) continue;

                        var next = new SlowBreakInfo(now, now + timeToBreak, pos,
                                player.getItemInHand(InteractionHand.MAIN_HAND).copy(), state);
                        world.destroyBlockProgress(pos.hashCode(), pos.toBlockPos(), 0);
                        breakingMapNew.put(pos, next);
                    }
                }
            }

            for (var entry : breakingMap.entrySet()) {
                if (entry.getValue().timeBroken < now) {
                    breakingMapNew.remove(entry.getKey());
                    entry.getValue().finish(world, serverPlayer);
                }
            }

            for (BlockPosition pos : breakingMap.keySet()) {
                if (!breakingMapNew.containsKey(pos)) {
                    world.destroyBlockProgress(pos.hashCode(), pos.toBlockPos(), -1);
                }
            }

            Map<BlockPosition, SlowBreakInfo> old = breakingMap;
            old.clear();
            breakingMap = breakingMapNew;
            breakingMapNew = old;
        }
    }

    public static final class SlowBreakInfo {
        public final long timeStarted;
        public final long timeBroken;
        public final BlockPosition pos;
        public final ItemStack originalTool;
        public final BlockState blockState;
        private int lastDamageSent;

        public SlowBreakInfo(long timeStarted, long timeBroken, BlockPosition pos,
                             ItemStack originalTool, BlockState blockState) {
            this.timeStarted = timeStarted;
            this.timeBroken = timeBroken;
            this.pos = pos;
            this.originalTool = originalTool;
            this.blockState = blockState;
        }

        public boolean checkTool(Player player) {
            ItemStack current = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
            if (current.isEmpty() || originalTool.isEmpty()) return current.isEmpty() && originalTool.isEmpty();
            return current.getItem() == originalTool.getItem()
                    && (current.isDamageableItem() || current.getDamageValue() == originalTool.getDamageValue());
        }

        public void update(Level world, long now) {
            long total = timeBroken - timeStarted;
            if (total > 0) {
                long damage = 10 * (now - timeStarted) / total;
                if (damage != lastDamageSent) {
                    lastDamageSent = (int) damage;
                    world.destroyBlockProgress(pos.hashCode(), pos.toBlockPos(), lastDamageSent);
                }
            }
        }

        public void finish(Level world, ServerPlayer player) {
            if (world.getBlockState(pos.toBlockPos()).equals(blockState)) {
                world.destroyBlockProgress(pos.hashCode(), pos.toBlockPos(), -1);
                if (player.gameMode.destroyBlock(pos.toBlockPos())) {
                    world.levelEvent(2001, pos.toBlockPos(), Block.getId(blockState));
                }
            }
        }
    }
}
