package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.common.datacomponents.OCComponents$;
import li.cil.oc.common.nanomachines.ControllerImpl;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;

import java.util.UUID;

public class NanomachineData extends ItemData {
    public String uuid = "";
    public Option<CompoundTag> configuration = Option.empty();

    public NanomachineData() {
        super(Constants.ItemName$.MODULE$.Nanomachines());
    }

    public NanomachineData(DataComponentHolder stack) {
        this();
        loadData(stack);
    }

    public NanomachineData(ControllerImpl controller) {
        this();
        uuid = controller.uuid();
        CompoundTag nbt = new CompoundTag();
        controller.configuration().saveData(nbt, true);
        configuration = Option.apply(nbt);
    }

    @Override
    public void loadData(DataComponentHolder holder) {
        uuid = Option.apply(holder.get(OCComponents$.MODULE$.ID().get())).toString();
        configuration = Option.apply(holder.get(OCComponents$.MODULE$.NANOMACHINES_NETWORK_INFO().get()));
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        holder.set(OCComponents$.MODULE$.ID().get(), UUID.fromString(uuid));
        if (configuration.isDefined()) holder.set(OCComponents$.MODULE$.NANOMACHINES_NETWORK_INFO().get(), configuration.get());
        else holder.remove(OCComponents$.MODULE$.NANOMACHINES_NETWORK_INFO().get());
    }
}
