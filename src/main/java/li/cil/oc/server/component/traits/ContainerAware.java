package li.cil.oc.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.StackOption;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public interface ContainerAware {
    Player fakePlayer();

    Container inventory();

    int selectedSlot();

    void selectedSlot_$eq(int value);

    default List<Integer> insertionSlots() {
        List<Integer> slots = new ArrayList<>(inventory().getContainerSize());
        for (int index = selectedSlot(); index < inventory().getContainerSize(); index++) slots.add(index);
        for (int index = 0; index < selectedSlot(); index++) slots.add(index);
        return slots;
    }

    default int optSlot(Arguments args, int index) {
        return args.count() > 0 && args.checkAny(0) != null
            ? new ExtendedArgumentsJava(args).checkSlot(inventory(), 0) : selectedSlot();
    }

    default StackOption stackInSlot(int slot) {
        return StackOption.apply(inventory().getItem(slot));
    }
}
