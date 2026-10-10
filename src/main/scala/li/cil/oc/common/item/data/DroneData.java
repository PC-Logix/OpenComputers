package li.cil.oc.common.item.data;

import com.google.common.base.Strings;
import li.cil.oc.Constants;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;

public class DroneData extends MicrocontrollerData {
    public String name = "";

    public DroneData() {
        super(Constants.ItemName$.MODULE$.Drone());
    }

    public DroneData(DataComponentHolder stack) {
        this();
        loadData(stack);
    }

    @Override
    public void loadData(DataComponentHolder holder) {
        super.loadData(holder);
        name = holder.getOrDefault(DataComponents.CUSTOM_NAME, Component.empty()).getString();
        if (Strings.isNullOrEmpty(name)) {
            CustomData tag = holder.get(DataComponents.CUSTOM_DATA);
            if (tag != null) {
                Option<String> oldName = ItemUtils.getDisplayName(tag.copyTag());
                if (oldName.isDefined()) {
                    name = oldName.get();
                    return;
                }
            }
            name = RobotData$.MODULE$.randomName();
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        if (!Strings.isNullOrEmpty(name)) holder.set(DataComponents.CUSTOM_NAME, Component.literal(name));
    }
}
