package li.cil.oc.common.blockentity.traits;

import li.cil.oc.common.datacomponents.OCComponentsJava;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolderJava;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.collection.immutable.Set;
import scala.jdk.javaapi.CollectionConverters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/** Client/server computer data-component persistence outside trait dispatch. */
public final class ComputerComponentDataJava {
    private ComputerComponentDataJava() { }

    public static void saveForServer(Computer computer, MutableDataComponentHolder holder) {
        if (computer.machine() != null) computer.machine().saveData(holder);
    }

    public static void loadForClient(Computer computer, DataComponentHolder holder) {
        computer.hasErrored_$eq(holder.has(OCComponentsJava.IS_ERRORED.get()));

        ExtendedDataComponentHolder data = new ExtendedDataComponentHolder(holder);
        computer.setRunning(data.getComponentOptional(OCComponentsJava.IS_RUNNING.get()).orElse(false));

        data.getComponentOptional(OCComponentsJava.USERS.get())
            .map(CollectionConverters::asJava)
            .ifPresentOrElse(computer::setUsersJava, () -> computer.setUsersJava(List.of()));
    }

    public static void saveForClient(Computer computer, MutableDataComponentHolder holder) {
        ExtendedMutableDataComponentHolderJava data = new ExtendedMutableDataComponentHolderJava(holder);
        boolean hasMachine = computer.machine() != null;
        data.setUnitComponent(OCComponentsJava.IS_ERRORED, hasMachine && computer.machine().lastError() != null);
        data.setComponent(OCComponentsJava.IS_RUNNING, computer.isRunning());
        if (hasMachine) {
            Set<String> users = CollectionConverters.asScala(new HashSet<>(Arrays.asList(computer.machine().users()))).toSet();
            data.setComponent(OCComponentsJava.USERS, users);
        }
    }
}
