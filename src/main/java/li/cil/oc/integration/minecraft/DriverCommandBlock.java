package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class DriverCommandBlock extends DriverSidedBlockEntity {
    public static final DriverCommandBlock INSTANCE = new DriverCommandBlock();
    public static final EnvironmentProvider PROVIDER = stack ->
            !stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.COMMAND_BLOCK ? Environment.class : null;

    private DriverCommandBlock() {}

    @Override
    public Class<?> getBlockEntityClass() { return CommandBlockEntity.class; }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((CommandBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<CommandBlockEntity> implements NamedBlock {
        public Environment(CommandBlockEntity blockEntity) { super(blockEntity, "command_block"); }

        @Override
        public String preferredName() { return "command_block"; }

        @Override
        public int priority() { return 0; }

        @Callback(direct = true, doc = "function():string -- Get the command currently set in this command block.")
        public Object[] getCommand(Context context, Arguments args) {
            return ResultWrapper.result(blockEntity.getCommandBlock().getCommand());
        }

        @Callback(doc = "function(value:string) -- Set the specified command for the command block.")
        public Object[] setCommand(Context context, Arguments args) {
            blockEntity.getCommandBlock().setCommand(args.checkString(0));
            var level = blockEntity.getLevel();
            var pos = blockEntity.getBlockPos();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            return ResultWrapper.result(true);
        }

        @Callback(doc = "function():number -- Execute the currently set command. This has a slight delay to allow the command block to properly update.")
        public Object[] executeCommand(Context context, Arguments args) {
            context.pause(0.1);
            if (!ServerLifecycleHooks.getCurrentServer().isCommandBlockEnabled()) {
                return ResultWrapper.result(null, "command blocks are disabled");
            }
            var sender = blockEntity.getCommandBlock();
            sender.performCommand(blockEntity.getLevel());
            return ResultWrapper.result(sender.getSuccessCount(), sender.getLastOutput().getString());
        }
    }
}
