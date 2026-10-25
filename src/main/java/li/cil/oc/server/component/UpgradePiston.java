package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.server.SoundEffectPacketSender;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.material.PushReaction;

import java.util.Map;

public abstract class UpgradePiston extends AbstractManagedEnvironment implements DeviceInfo {
    private final EnvironmentHost host;
    private final ComponentConnector node;

    protected UpgradePiston(EnvironmentHost host) {
        this.host = host;
        this.node = Network.newNode(this, Visibility.Network).withComponent("piston").withConnector().create();
    }

    public EnvironmentHost host() { return host; }

    @Override
    public ComponentConnector node() { return node; }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.Generic.toString(),
                DeviceAttribute.Description, "Piston upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Displacer II+");
    }

    public boolean isSticky() { return false; }

    protected BlockPosition pushOrigin(Direction side) {
        return BlockPositionsJava.apply(host);
    }

    protected abstract Direction pushDirection(Arguments args, int index);

    @Callback(doc = "function():boolean -- Returns true if the piston is sticky, i.e. it can also pull.")
    public Object[] isSticky(Context context, Arguments args) {
        return ResultWrapper.result(isSticky());
    }

    protected Object[] doPistonAction(Context context, Direction side, boolean extending) {
        ResourceLocation sound = (extending ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT).getLocation();
        BlockPos hostPos = pushOrigin(side).toBlockPos();
        PistonBaseBlock piston = (PistonBaseBlock) (isSticky() ? Blocks.STICKY_PISTON : Blocks.PISTON);

        if (!extending) {
            if (!isSticky()) {
                // Preserve the original failure for calling pull on a non-sticky piston.
                throw new NoSuchMethodError("piston is not sticky. does not have pull");
            }
            BlockPos innerBlockPos = hostPos.relative(side);
            var innerBlockState = host.getEnvironmentLevel().getBlockState(innerBlockPos);
            if (innerBlockState != null && !innerBlockState.isAir()
                    && innerBlockState.getPistonPushReaction() != PushReaction.DESTROY) {
                return ResultWrapper.result(false, "path is obstructed");
            }
        }

        if (piston.moveBlocks(host.getEnvironmentLevel(), hostPos, side, extending)) {
            synchronized (host) {
                SoundEffectPacketSender.sendEffectUnchecked(
                        host.getEnvironmentLevel(), hostPos.getX(), hostPos.getY(), hostPos.getZ(),
                        sound, SoundSource.BLOCKS, 15.0);
            }
            context.pause(1.0 / 20.0);
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false, "move failed");
    }

    @Callback(doc = "function([side:number]):boolean -- Tries to push the block on the specified side of the container of the upgrade. Defaults to front.")
    public Object[] push(Context context, Arguments args) {
        return doPistonAction(context, pushDirection(args, 0), true);
    }

    public static class Drone extends UpgradePiston {
        public Drone(li.cil.oc.api.internal.Drone drone) { super(drone); }
        @Override protected Direction pushDirection(Arguments args, int index) {
            return new ExtendedArgumentsJava(args).optSideAny(index, Direction.SOUTH);
        }
    }

    public static class Rotatable extends UpgradePiston {
        protected final li.cil.oc.api.internal.Rotatable rotatable;
        public Rotatable(li.cil.oc.api.internal.Rotatable rotatable) {
            super((EnvironmentHost) rotatable);
            this.rotatable = rotatable;
        }
        @Override protected Direction pushDirection(Arguments args, int index) {
            return rotatable.toGlobal(new ExtendedArgumentsJava(args).optSideForAction(index, Direction.SOUTH));
        }
    }

    public static class Tablet extends Rotatable {
        private final li.cil.oc.api.internal.Tablet tablet;
        public Tablet(li.cil.oc.api.internal.Tablet tablet) {
            super(tablet);
            this.tablet = tablet;
        }
        @Override protected BlockPosition pushOrigin(Direction side) {
            if (side == Direction.DOWN && tablet.player().getEyeHeight() > 1.0f) {
                return super.pushOrigin(side).offset(Direction.DOWN);
            }
            return super.pushOrigin(side);
        }
    }
}




