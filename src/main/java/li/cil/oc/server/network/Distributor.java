package li.cil.oc.server.network;

public interface Distributor {
    double globalBuffer();

    void globalBuffer_$eq(double value);

    double globalBufferSize();

    void globalBufferSize_$eq(double value);

    void addConnector(Connector connector);

    void removeConnector(Connector connector);

    double changeBuffer(double delta);
}
