package li.cil.oc.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.Tier;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.common.datacomponents.OCComponents$;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;

import java.util.ArrayList;
import java.util.List;

public class QuadGraphicsCard extends AbstractManagedEnvironment {
    private static final int HEAD_COUNT = 4;
    private static final double VRAM_SCREENS_PER_HEAD = 3.0;
    private final GraphicsCard[] graphicsCards = new GraphicsCard[HEAD_COUNT];

    public QuadGraphicsCard() {
        for (int head = 0; head < HEAD_COUNT; head++) {
            graphicsCards[head] = new GraphicsCard(Tier.Two, Option.apply(VRAM_SCREENS_PER_HEAD), Visibility.Network);
        }
        setNode(Network.newNode(this, Visibility.Network).create());
    }

    @Override
    public void onConnect(Node connectedNode) {
        if (connectedNode == node()) {
            for (GraphicsCard graphicsCard : graphicsCards) node().connect(graphicsCard.node());
        }
    }

    @Override
    public void onDisconnect(Node disconnectedNode) {
        if (disconnectedNode == node()) {
            for (GraphicsCard graphicsCard : graphicsCards) graphicsCard.node().remove();
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        scala.collection.immutable.List<Option<CompoundStorage>> saved = holder.get(OCComponents$.MODULE$.COMPONENT_NODES().get());
        if (saved == null) return;

        scala.collection.Iterator<Option<CompoundStorage>> entries = saved.iterator();
        for (GraphicsCard graphicsCard : graphicsCards) {
            if (!entries.hasNext()) break;
            Option<CompoundStorage> storage = entries.next();
            if (storage.isDefined()) graphicsCard.loadData(storage.get());
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        List<Option<CompoundStorage>> saved = new ArrayList<>(HEAD_COUNT);
        for (GraphicsCard graphicsCard : graphicsCards) {
            CompoundStorage storage = new CompoundStorage(DataComponentMap.EMPTY);
            graphicsCard.saveData(storage);
            saved.add(Option.apply(storage));
        }
        holder.set(OCComponents$.MODULE$.COMPONENT_NODES().get(), scala.jdk.javaapi.CollectionConverters.asScala(saved).toList());
    }
}
