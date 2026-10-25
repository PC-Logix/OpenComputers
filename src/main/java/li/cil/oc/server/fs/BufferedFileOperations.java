package li.cil.oc.server.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.function.Supplier;
import org.apache.commons.io.FileUtils;
import scala.Option;

/** File copying for buffered filesystem mirrors. */
public final class BufferedFileOperations {
    private BufferedFileOperations() { }

    public static boolean restore(OutputHandle output, File source) throws IOException {
        try (FileInputStream input = new FileInputStream(source)) {
            byte[] buffer = new byte[8 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) output.write(read == buffer.length ? buffer : java.util.Arrays.copyOf(buffer, read));
            }
        } catch (FileNotFoundException ignored) {
            // The source file vanished while the virtual filesystem was being restored.
        }
        output.close();
        return true;
    }

    public static boolean restore(File source, Supplier<Option<OutputHandle>> openOutput) throws IOException {
        Option<OutputHandle> opened = openOutput.get();
        return opened != null && opened.isDefined() && restore(opened.get(), source);
    }

    public static boolean store(File destination, long modified, InputChannel input) throws IOException {
        FileUtils.deleteQuietly(destination);
        destination.createNewFile();
        try (InputChannel source = input; FileChannel output = new FileOutputStream(destination).getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
            while (source.read(buffer) != -1) {
                buffer.flip();
                output.write(buffer);
                buffer.compact();
            }
            buffer.flip();
            while (buffer.hasRemaining()) output.write(buffer);
        }
        destination.setLastModified(modified);
        return true;
    }

    public static boolean store(File destination, long modified,
                                Supplier<Option<InputChannel>> openInput) throws IOException {
        Option<InputChannel> opened = openInput.get();
        return store(destination, modified, opened.get());
    }
}
