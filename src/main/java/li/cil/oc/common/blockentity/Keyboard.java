package li.cil.oc.common.blockentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Items;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class Keyboard extends KeyboardEnvironmentBlockEntity {
    private static final String KEYBOARD_TAG = Settings.namespace() + "keyboard";

    private final ManagedEnvironment keyboard;

    public Keyboard(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.KEYBOARD.get(), pos, state);
        ItemStack item = Items.get(Constants.BlockName$.MODULE$.Keyboard()).createItemStack(1);
        keyboard = Driver.driverFor(item, getClass()).createEnvironment(item, this);
    }

    public ManagedEnvironment keyboard() {
        return keyboard;
    }

    @Override
    public Direction[] validFacings() {
        return Direction.values();
    }

    @Override
    public Node node() {
        return keyboard.node();
    }

    public boolean hasNodeOnSide(Direction side) {
        return side != facing() && (isOnWall() || side.getOpposite() != forward());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean canConnect(Direction side) {
        return hasNodeOnSide(side);
    }

    @Override
    public Node sidedNode(Direction side) {
        return hasNodeOnSide(side) ? node() : null;
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{node()};
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        if (isServer()) {
            try {
                keyboard.loadData(nbt.getCompound(KEYBOARD_TAG), provider);
            } catch (UnrecoverablePersistanceException e) {
                throw new IllegalStateException("Unable to restore keyboard state", e);
            }
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        if (isServer()) {
            CompoundTag keyboardTag = new CompoundTag();
            keyboard.saveData(keyboardTag, provider);
            nbt.put(KEYBOARD_TAG, keyboardTag);
        }
    }

    private boolean isOnWall() {
        return facing() != Direction.UP && facing() != Direction.DOWN;
    }

    private Direction forward() {
        return isOnWall() ? Direction.UP : yaw();
    }
}
