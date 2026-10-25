package li.cil.oc.server.fs;

import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.io.IOException;
import java.util.Optional;

/** Read-only integration base exposing only Java types to filesystem implementors. */
public abstract class JavaReadOnlyStreamFileSystem extends JavaInputStreamFileSystem {
    protected abstract Optional<InputChannel> openInputChannelJava(String path) throws IOException;

    @Override public final Option<InputChannel> openInputChannel(String path) {
        try {
            return OptionConverters.toScala(openInputChannelJava(path));
        } catch (IOException error) {
            throw unchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T { throw (T) error; }
}
