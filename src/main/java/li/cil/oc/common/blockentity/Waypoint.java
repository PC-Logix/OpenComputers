package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.EventHandler;
import li.cil.oc.server.ActivityPacketSender;
import li.cil.oc.server.network.Waypoints;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolder;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.runtime.BoxedUnit;

import java.io.IOException;
import java.io.UncheckedIOException;

public class Waypoint extends WaypointEnvironmentBlockEntity {
    private final Component node = Network.newNode(this, Visibility.Network)
            .withComponent("waypoint").create();
    private String label = "";

    public Waypoint(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.WAYPOINT.get(), pos, state);
    }

    @Override
    public Component node() {
        return node;
    }

    public String label() {
        return label;
    }

    public void label_$eq(String value) {
        label = value;
    }

    @Override
    public Direction[] validFacings() {
        return Direction.values();
    }

    @Callback(doc = "function(): string -- Get the current label of this waypoint.")
    public Object[] getLabel(Context context, Arguments args) {
        return ResultWrapper.result(label);
    }

    @Callback(doc = "function(value:string) -- Set the label for this waypoint.")
    public Object[] setLabel(Context context, Arguments args) {
        String value = args.checkString(0);
        label = value.substring(0, Math.min(value.length(), 32));
        setChanged();
        try {
            ActivityPacketSender.sendWaypointLabel(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        context.pause(0.5);
        return null;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isClient()) {
            Direction facing = facing();
            Vec3 origin = position().toVec3().add(facing.getStepX() * 0.5,
                    facing.getStepY() * 0.5, facing.getStepZ() * 0.5);
            var random = getLevel().random;
            float dx = (random.nextFloat() - 0.5f) * 0.8f;
            float dy = (random.nextFloat() - 0.5f) * 0.8f;
            float dz = (random.nextFloat() - 0.5f) * 0.8f;
            float vx = (random.nextFloat() - 0.5f) * 0.2f + facing.getStepX() * 0.3f;
            float vy = (random.nextFloat() - 0.5f) * 0.2f + facing.getStepY() * 0.3f - 0.5f;
            float vz = (random.nextFloat() - 0.5f) * 0.2f + facing.getStepZ() * 0.3f;
            getLevel().addParticle(ParticleTypes.PORTAL, origin.x + dx, origin.y + dy,
                    origin.z + dz, vx, vy, vz);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        EventHandler.scheduleServer(() -> {
            Waypoints.add(this);
            return BoxedUnit.UNIT;
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        Waypoints.remove(this);
    }

    @Override
    public void loadComponentsCommon(DataComponentHolder holder) {
        super.loadComponentsCommon(holder);
        new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.LABEL.get())
            .ifPresent(value -> label = value);
    }

    @Override
    public void saveComponentsCommon(MutableDataComponentHolder holder) {
        super.saveComponentsCommon(holder);
        new ExtendedMutableDataComponentHolder(holder).setComponent(OCComponentsJava.LABEL.get(), label);
    }
}
