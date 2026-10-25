package li.cil.oc.server.fs;

public interface Volatile extends VirtualFileSystem {
    @Override
    default void close() {
        VirtualFileSystem.super.close();
        root().children().clear();
    }
}
