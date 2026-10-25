package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.io.IOException;
import java.util.Optional;

/** Writable integration base whose channel and handle hooks use Java Optional. */
public abstract class JavaWritableStreamFileSystem extends JavaOutputStreamFileSystem {
    protected abstract Optional<InputChannel> openInputChannelJava(String path) throws IOException;
    protected abstract Optional<OutputHandle> openOutputHandleJava(int id, String path, Mode mode) throws IOException;

    @Override public final Option<InputChannel> openInputChannel(String path) {
        try {
            return OptionConverters.toScala(openInputChannelJava(path));
        } catch (IOException error) {
            throw unchecked(error);
        }
    }
    @Override public final Option<OutputHandle> openOutputHandle(int id, String path, Mode mode) {
        try {
            return OptionConverters.toScala(openOutputHandleJava(id, path, mode));
        } catch (IOException error) {
            throw unchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T { throw (T) error; }
}
