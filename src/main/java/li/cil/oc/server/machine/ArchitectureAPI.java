package li.cil.oc.server.machine;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

public abstract class ArchitectureAPI {
    public final Machine machine;

    protected ArchitectureAPI(Machine machine) {
        this.machine = machine;
    }

    public Machine machine() {
        return machine;
    }

    protected Node node() {
        return machine.node();
    }

    protected Map<String, String> components() {
        return machine.components();
    }

    public abstract void initialize();

    public void loadData(CompoundTag nbt) {
    }

    public void saveData(CompoundTag nbt) {
    }
}
