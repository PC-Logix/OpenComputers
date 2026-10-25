package li.cil.oc.server.network;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Visibility;
import org.junit.Test;
import scala.Option;

import static org.junit.Assert.*;

public class ConnectorEnergyTest {
    @Test
    public void standaloneBufferClampsAndReportsRemainder() {
        TestConnector connector = new TestConnector();
        connector.localBufferSize = 10;
        connector.localBuffer = 3;

        assertEquals(0, ConnectorEnergy.changeBuffer(connector, 5, false), 0);
        assertEquals(8, connector.localBuffer, 0);
        assertEquals(3, ConnectorEnergy.changeBuffer(connector, 5, false), 0);
        assertEquals(10, connector.localBuffer, 0);
        assertEquals(-10, ConnectorEnergy.changeBuffer(connector, -20, false), 0);
        assertEquals(0, connector.localBuffer, 0);
    }

    @Test
    public void atomicChangesRejectOutOfBoundsRequests() {
        TestConnector connector = new TestConnector();
        connector.localBufferSize = 10;
        connector.localBuffer = 5;

        assertFalse(ConnectorEnergy.tryChangeBuffer(connector, -6, false));
        assertFalse(ConnectorEnergy.tryChangeBuffer(connector, 6, false));
        assertEquals(5, connector.localBuffer, 0);
        assertTrue(ConnectorEnergy.tryChangeBuffer(connector, -5, false));
        assertEquals(0, connector.localBuffer, 0);
    }

    @Test
    public void ignoredPowerLeavesBufferAlone() {
        TestConnector connector = new TestConnector();
        connector.localBufferSize = 10;
        connector.localBuffer = 5;

        assertEquals(0, ConnectorEnergy.changeBuffer(connector, -3, true), 0);
        assertEquals(3, ConnectorEnergy.changeBuffer(connector, 3, true), 0);
        assertTrue(ConnectorEnergy.tryChangeBuffer(connector, -3, true));
        assertFalse(ConnectorEnergy.tryChangeBuffer(connector, 3, true));
        assertEquals(5, connector.localBuffer, 0);
    }

    private static final class TestConnector implements Connector {
        double localBufferSize;
        double localBuffer;
        Option<Distributor> distributor = Option.empty();
        String address;
        Network network;

        @Override public double localBufferSize() { return localBufferSize; }
        @Override public void localBufferSize_$eq(double value) { localBufferSize = value; }
        @Override public double localBuffer() { return localBuffer; }
        @Override public void localBuffer_$eq(double value) { localBuffer = value; }
        @Override public Option<Distributor> distributor() { return distributor; }
        @Override public void distributor_$eq(Option<Distributor> value) { distributor = value; }
        @Override public Environment host() { return null; }
        @Override public Visibility reachability() { return Visibility.Network; }
        @Override public String address() { return address; }
        @Override public void address_$eq(String value) { address = value; }
        @Override public Network network() { return network; }
        @Override public void network_$eq(Network value) { network = value; }
        @Override public void sendToAddress(String target, String name, Object... data) { }
        @Override public void sendToNeighbors(String name, Object... data) { }
        @Override public void sendToReachable(String name, Object... data) { }
        @Override public void sendToVisible(String name, Object... data) { }
    }
}
