package li.cil.oc.server.network;

import li.cil.oc.api.network.Visibility;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class NetworkBuildersTest {
    @Test
    public void buildersCreateBasicAndConnectorNodesWithTheirConfiguredState() {
        var node = new NetworkBuilders.NodeBuilder(null, Visibility.Network, () -> true).create();
        assertNotNull(node);
        assertTrue(node instanceof NodeVarargPart);
        assertEquals(Visibility.Network, ((Node) node).reachability());

        Connector connector = (Connector) new NetworkBuilders.NodeBuilder(null, Visibility.Network, () -> true)
                .withConnector(16).create();
        assertEquals(16, connector.localBufferSize(), 0);
        connector.localBufferSize_$eq(8);
        assertEquals(8, connector.localBufferSize(), 0);

        assertNotNull(new NetworkBuilders.NodeBuilder(null, Visibility.Network, () -> true)
                .withComponent("test", Visibility.Neighbors));
        assertNotNull(new NetworkBuilders.NodeBuilder(null, Visibility.Network, () -> true)
                .withConnector(32).withComponent("combined", Visibility.Neighbors));
    }
    @Test
    public void buildersReturnNullOnTheClientSide() {
        NetworkBuilders.NodeBuilder builder = new NetworkBuilders.NodeBuilder(null, Visibility.Network, () -> false);
        assertNull(builder.create());
        assertNull(builder.withComponent("test").create());
        assertNull(builder.withConnector().create());
        assertNull(builder.withConnector().withComponent("test").create());
    }
}