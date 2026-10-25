package li.cil.oc.common;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

public abstract class PacketBuilderBase<T extends OutputStream> extends PacketBuilder {
    protected final T stream;

    protected PacketBuilderBase(T stream) {
        super(new BufferedOutputStream(stream));
        this.stream = stream;
    }
}
