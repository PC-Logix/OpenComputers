package li.cil.oc.server.network;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.OpenComputers;
import com.google.common.base.Strings;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.component.DataComponentHolder;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolderJava;
import java.util.function.Consumer;
import java.util.Optional;

import java.util.Collections;

/** Network operations shared by the Scala node implementations. */
public final class NodeJava {
    private NodeJava() {
    }

    public static boolean canBeReachedFrom(Node self, li.cil.oc.api.network.Node other) {
        switch (self.reachability()) {
            case None:
                return false;
            case Neighbors:
                return isNeighborOf(self, other);
            case Network:
                return isInSameNetwork(self, other);
            default:
                throw new AssertionError(self.reachability());
        }
    }

    public static boolean isNeighborOf(Node self, li.cil.oc.api.network.Node other) {
        if (!isInSameNetwork(self, other)) return false;
        for (li.cil.oc.api.network.Node node : self.network().neighbors(self)) {
            if (node.equals(other)) return true;
        }
        return false;
    }

    public static java.lang.Iterable<li.cil.oc.api.network.Node> reachableNodes(Node self) {
        return self.network() == null ? Collections.emptyList() : self.network().nodes(self);
    }

    public static java.lang.Iterable<li.cil.oc.api.network.Node> neighbors(Node self) {
        return self.network() == null ? Collections.emptyList() : self.network().neighbors(self);
    }

    public static void connect(Node self, li.cil.oc.api.network.Node other) {
        if (self.network() != null) self.network().connect(self, other);
    }

    public static void disconnect(Node self, li.cil.oc.api.network.Node other) {
        if (self.network() != null && isInSameNetwork(self, other)) self.network().disconnect(self, other);
    }

    public static void remove(Node self) {
        if (self.network() != null) self.network().remove(self);
    }

    public static void onConnect(Node self, li.cil.oc.api.network.Node other) {
        try {
            self.host().onConnect(other);
        } catch (Throwable error) {
            OpenComputers.log().warn("A component of type '" + self.host().getClass().getName()
                    + "' threw an error while being connected to the component network.", error);
        }
    }

    public static void onDisconnect(Node self, li.cil.oc.api.network.Node other) {
        try {
            self.host().onDisconnect(other);
        } catch (Throwable error) {
            OpenComputers.log().warn("A component of type '" + self.host().getClass().getName()
                    + "' threw an error while being disconnected from the component network.", error);
        }
    }

    public static void loadAddress(Node self, String newAddress) {
        if (!Strings.isNullOrEmpty(newAddress) && !newAddress.equals(self.address())) {
            li.cil.oc.api.network.Network network = self.network();
            if (network instanceof NetworkWrapper wrapper) wrapper.network().remap(self, newAddress);
            else self.address_$eq(newAddress);
        }
    }

    public static void loadData(DataComponentHolder holder, Consumer<String> addressLoader) {
        Optional<String> address = new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.ADDRESS.get());
        address.ifPresent(addressLoader);
    }

    public static void saveData(Node self, net.neoforged.neoforge.common.MutableDataComponentHolder holder) {
        if (self.address() != null) {
            new ExtendedMutableDataComponentHolderJava(holder).setComponent(OCComponentsJava.ADDRESS.get(), self.address());
        }
    }

    private static boolean isInSameNetwork(Node self, li.cil.oc.api.network.Node other) {
        return self.network() != null && other != null && self.network().equals(other.network());
    }
}
