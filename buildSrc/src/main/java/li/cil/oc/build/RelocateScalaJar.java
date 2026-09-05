package li.cil.oc.build;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Relocates the private Scala runtime embedded in the OpenComputers jar. */
public final class RelocateScalaJar {
    private static final String SCALA_PACKAGE = "scala";
    private static final String RELOCATED_PACKAGE = "li.cil.oc.internal.scalalib";
    private static final String SCALA_PATH = "scala/";
    private static final String RELOCATED_PATH = "li/cil/oc/internal/scalalib/";

    private RelocateScalaJar() {
    }

    public static void relocate(final Path jar) throws IOException {
        final Path temporaryJar = jar.resolveSibling(jar.getFileName() + ".relocating");

        try (InputStream input = Files.newInputStream(jar);
             ZipInputStream zipInput = new ZipInputStream(input);
             OutputStream output = Files.newOutputStream(temporaryJar);
             ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                final String outputName = relocatePath(entry.getName());
                final ZipEntry outputEntry = new ZipEntry(outputName);
                outputEntry.setTime(entry.getTime());
                if (entry.getComment() != null) {
                    outputEntry.setComment(entry.getComment());
                }

                zipOutput.putNextEntry(outputEntry);
                if (!entry.isDirectory()) {
                    final byte[] contents = zipInput.readAllBytes();
                    zipOutput.write(isClassFile(entry.getName()) ? relocateClass(contents) : contents);
                }
                zipOutput.closeEntry();
            }
        } catch (final IOException | RuntimeException exception) {
            Files.deleteIfExists(temporaryJar);
            throw exception;
        }

        Files.move(temporaryJar, jar, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String relocatePath(final String path) {
        if (path.equals(SCALA_PACKAGE)) {
            return RELOCATED_PATH.substring(0, RELOCATED_PATH.length() - 1);
        }
        if (path.startsWith(SCALA_PATH)) {
            return RELOCATED_PATH + path.substring(SCALA_PATH.length());
        }
        return path;
    }

    private static boolean isClassFile(final String path) {
        return path.endsWith(".class");
    }

    private static byte[] relocateClass(final byte[] contents) {
        final ClassWriter writer = new ClassWriter(0);
        final ClassRemapper remapper = new ClassRemapper(writer, new Remapper() {
            @Override
            public String map(final String internalName) {
                return relocateInternalName(internalName);
            }

            @Override
            public Object mapValue(final Object value) {
                if (value instanceof String string) {
                    return relocateClassNameString(string);
                }
                return super.mapValue(value);
            }
        });
        new ClassReader(contents).accept(remapper, 0);
        return writer.toByteArray();
    }

    private static String relocateInternalName(final String name) {
        if (name.equals(SCALA_PACKAGE)) {
            return RELOCATED_PACKAGE.replace('.', '/');
        }
        if (name.startsWith(SCALA_PATH)) {
            return RELOCATED_PATH + name.substring(SCALA_PATH.length());
        }
        return name;
    }

    private static String relocateClassNameString(final String value) {
        final String scalaPackage = SCALA_PACKAGE + ".";
        if (value.startsWith(scalaPackage)) {
            return RELOCATED_PACKAGE + value.substring(SCALA_PACKAGE.length());
        }
        return value;
    }
}
