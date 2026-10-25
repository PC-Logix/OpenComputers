package li.cil.oc.server.machine.luac;

import com.google.common.base.Strings;
import com.google.common.io.PatternFilenameFilter;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.SystemUtils;
import scala.Option;
import scala.Option$;
import scala.Some;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.Random;
import java.util.regex.Pattern;

/** Java implementation of shared native Lua loading and state initialization. */
public abstract class LuaStateFactoryBase {
    private static final PatternFilenameFilter STALE_LIBRARY_FILTER = new PatternFilenameFilter(
        "^" + Pattern.quote("OpenComputersMod-") + ".*\\.(dll|so|dylib)$"
    );
    private static boolean cleanedOldLibraries;

    private final String libraryName;
    private boolean haveNativeLibrary;
    private String currentLib = "";

    protected LuaStateFactoryBase() {
        libraryName = createLibraryName();
        init();
        if (!haveNativeLibrary) {
            OpenComputers.log().warn("Unsupported platform, you won't be able to host games with persistent computers.");
        }
    }

    protected abstract String version();
    protected abstract LuaState create(Integer maxMemory);
    protected abstract void openLibs(LuaState state);

    public final boolean isAvailable() { return haveNativeLibrary; }

    private String createLibraryName() {
        String extension = SystemUtils.IS_OS_MAC ? ".dylib" : SystemUtils.IS_OS_WINDOWS ? ".dll" : ".so";
        String platformName;
        String forcedPlatform = Settings.get().forceNativeLibPlatform();
        if (!Strings.isNullOrEmpty(forcedPlatform)) {
            platformName = forcedPlatform;
        }
        else {
            String systemName;
            if (SystemUtils.IS_OS_FREE_BSD) systemName = "freebsd";
            else if (SystemUtils.IS_OS_NET_BSD) systemName = "netbsd";
            else if (SystemUtils.IS_OS_OPEN_BSD) systemName = "openbsd";
            else if (SystemUtils.IS_OS_SOLARIS) systemName = "solaris";
            else if (SystemUtils.IS_OS_LINUX) systemName = "linux";
            else if (SystemUtils.IS_OS_MAC) systemName = "darwin";
            else if (SystemUtils.IS_OS_WINDOWS) systemName = "windows";
            else systemName = "unknown";

            String archName;
            if (ArchitectureFlags.IS_OS_ARM64) archName = "aarch64";
            else if (ArchitectureFlags.IS_OS_ARM) archName = "arm";
            else if (ArchitectureFlags.IS_OS_X64) archName = "x86_64";
            else if (ArchitectureFlags.IS_OS_X86) archName = "x86";
            else archName = "unknown";
            platformName = systemName + "-" + archName;
        }
        return "libjnlua" + version() + "-" + platformName + extension;
    }

    private void init() {
        if (libraryName == null) return;

        if (SystemUtils.IS_OS_WINDOWS && !Settings.get().alwaysTryNative()) {
            if (SystemUtils.IS_OS_WINDOWS_XP) {
                OpenComputers.log().warn("Sorry, but Windows XP isn't supported. I'm afraid you'll have to use a newer Windows. I very much recommend upgrading your Windows, anyway, since Microsoft has stopped supporting Windows XP in April 2014.");
                return;
            }
            if (SystemUtils.IS_OS_WINDOWS_2003) {
                OpenComputers.log().warn("Sorry, but Windows Server 2003 isn't supported. I'm afraid you'll have to use a newer Windows.");
                return;
            }
        }

        File tmpLibFile = null;
        String forcedPath = Settings.get().forceNativeLibPathFirst();
        if (!Strings.isNullOrEmpty(forcedPath)) {
            File libraryTest = new File(forcedPath, libraryName);
            if (libraryTest.canRead()) {
                tmpLibFile = libraryTest;
                currentLib = libraryTest.getAbsolutePath();
                OpenComputers.log().info("Found forced-path filesystem library " + currentLib + ".");
            }
            else {
                OpenComputers.log().warn("forceNativeLibPathFirst is set, but " + currentLib + " was not found there. Falling back to checking the built-in libraries.");
            }
        }

        if (currentLib.isEmpty()) {
            String libraryPath = "/assets/" + Settings.resourceDomain() + "/lib/" + libraryName;
            URL libraryUrl = findResource(libraryPath.substring(1));
            if (libraryUrl == null) {
                OpenComputers.log().warn("Native library with name '" + libraryPath + "' not found.");
                return;
            }

            String tmpLibName = "OpenComputersMod-" + OpenComputers.Version() + "-" + version() + "-" + libraryName;
            String tmpBasePath;
            if (Settings.get().nativeInTmpDir()) {
                String path = System.getProperty("java.io.tmpdir");
                if (path == null) tmpBasePath = "";
                else if (path.endsWith("/") || path.endsWith("\\")) tmpBasePath = path;
                else tmpBasePath = path + "/";
            }
            else {
                File path = new File("opencomputers/natives");
                path.mkdirs();
                tmpBasePath = path.getAbsolutePath() + File.separator;
            }
            tmpLibFile = new File(tmpBasePath + tmpLibName);

            if (!Settings.get().nativeInTmpDir()) {
                cleanupOldLibraries(new File(tmpBasePath));
            }

            if (tmpLibFile.exists()) {
                boolean matching = true;
                try (BufferedInputStream inCurrent = new BufferedInputStream(libraryUrl.openStream());
                     BufferedInputStream inExisting = new BufferedInputStream(new FileInputStream(tmpLibFile))) {
                    int inCurrentByte = 0;
                    int inExistingByte = 0;
                    while (matching) {
                        inCurrentByte = inCurrent.read();
                        inExistingByte = inExisting.read();
                        if (inCurrentByte != inExistingByte) matching = false;
                        if (inCurrentByte == -1 || inExistingByte == -1) break;
                    }
                }
                catch (Throwable ignored) {
                    matching = false;
                }
                if (!matching) {
                    try { tmpLibFile.delete(); }
                    catch (Throwable ignored) { }
                    if (tmpLibFile.exists()) {
                        OpenComputers.log().warn("Could not update native library '" + tmpLibFile.getName() + "'!");
                    }
                }
            }

            try (var in = Channels.newChannel(libraryUrl.openStream());
                 var out = new FileOutputStream(tmpLibFile).getChannel()) {
                out.transferFrom(in, 0, Long.MAX_VALUE);
                tmpLibFile.deleteOnExit();
                tmpLibFile.setReadable(true, false);
                tmpLibFile.setWritable(true, false);
                tmpLibFile.setExecutable(true, false);
            }
            catch (Throwable ignored) {
                // Native extraction is retried on the next initialization.
            }
            currentLib = tmpLibFile.getAbsolutePath();
        }

        try {
            synchronized (LuaStateFactory.class) {
                System.load(currentLib);
                create(null).close();
            }
            OpenComputers.log().info("Found a compatible native library: '" + tmpLibFile.getName() + "'.");
            haveNativeLibrary = true;
        }
        catch (Throwable error) {
            if (Settings.get().logFullLibLoadErrors()) {
                OpenComputers.log().warn("Could not load native library '" + tmpLibFile.getName() + "'.", error);
            }
            else {
                OpenComputers.log().trace("Could not load native library '" + tmpLibFile.getName() + "'.");
            }
            tmpLibFile.delete();
        }
    }

    private static synchronized void cleanupOldLibraries(File nativeDir) {
        if (cleanedOldLibraries) return;
        cleanedOldLibraries = true;
        cleanLibrariesIn(nativeDir);
        cleanLibrariesIn(new File("."));
    }

    private static void cleanLibrariesIn(File dir) {
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles(STALE_LIBRARY_FILTER);
        if (files == null) return;
        for (File file : files) {
            if (!file.isFile()) continue;
            try {
                if (file.delete()) {
                    OpenComputers.log().debug("Deleted stale native library '" + file.getAbsolutePath() + "'.");
                }
                else if (file.exists()) {
                    OpenComputers.log().debug("Could not delete stale native library '" + file.getAbsolutePath() + "'. It may still be in use.");
                }
            }
            catch (Throwable error) {
                OpenComputers.log().debug("Could not delete stale native library '" + file.getAbsolutePath() + "'.", error);
            }
        }
    }

    private static URL findResource(String path) {
        ClassLoader[] loaders = {
            Thread.currentThread().getContextClassLoader(),
            LuaStateFactoryBase.class.getClassLoader(),
            ClassLoader.getSystemClassLoader(),
            LuaState.class.getClassLoader()
        };
        for (ClassLoader loader : loaders) {
            if (loader != null) {
                URL resource = loader.getResource(path);
                if (resource != null) return resource;
            }
        }
        return null;
    }

    public final Option<LuaState> createState() {
        if (!haveNativeLibrary) return Option$.MODULE$.empty();

        try {
            LuaState state;
            synchronized (LuaStateFactory.class) {
                System.load(currentLib);
                state = Settings.get().limitMemory() ? create(Integer.MAX_VALUE) : create(null);
            }
            try {
                openLibs(state);
                if (!Settings.get().disableLocaleChanging()) {
                    state.openLib(LuaState.Library.OS);
                    state.getField(-1, "setlocale");
                    state.pushString("C");
                    state.call(1, 0);
                    state.pop(1);
                }

                state.newTable();
                state.setGlobal("os");
                state.pushNil();
                state.setGlobal("unpack");
                state.pushNil();
                state.setGlobal("loadstring");
                state.getGlobal("math");
                state.pushNil();
                state.setField(-2, "log10");
                state.pop(1);
                state.getGlobal("table");
                state.pushNil();
                state.setField(-2, "maxn");
                state.pop(1);
                state.pushNil();
                state.setGlobal("dofile");
                state.pushNil();
                state.setGlobal("loadfile");
                state.getGlobal("math");

                Random random = new Random();
                ExtendedLuaState.extendLuaState(state).pushScalaFunction(lua -> {
                    double value = random.nextDouble();
                    int top = lua.getTop();
                    if (top == 0) lua.pushNumber(value);
                    else if (top == 1) {
                        double upper = lua.checkNumber(1);
                        lua.checkArg(1, 1 <= upper, "interval is empty");
                        lua.pushNumber(Math.floor(value * upper) + 1);
                    }
                    else if (top == 2) {
                        double lower = lua.checkNumber(1);
                        double upper = lua.checkNumber(2);
                        lua.checkArg(2, lower <= upper, "interval is empty");
                        lua.pushNumber(Math.floor(value * (upper - lower + 1)) + lower);
                    }
                    else throw new IllegalArgumentException("wrong number of arguments");
                    return Integer.valueOf(1);
                });
                state.setField(-2, "random");

                ExtendedLuaState.extendLuaState(state).pushScalaFunction(lua -> {
                    random.setSeed(lua.checkInteger(1));
                    return Integer.valueOf(0);
                });
                state.setField(-2, "randomseed");
                state.pop(1);
                return new Some<>(state);
            }
            catch (Throwable error) {
                OpenComputers.log().warn("Failed creating Lua state.", error);
                state.close();
            }
        }
        catch (UnsatisfiedLinkError error) {
            OpenComputers.log().error("Failed loading the native libraries.");
        }
        catch (Throwable error) {
            OpenComputers.log().warn("Failed creating Lua state.", error);
        }
        return Option$.MODULE$.empty();
    }

    private static final class ArchitectureFlags {
        private static final String OS_ARCH = readOsArch();
        private static final boolean IS_OS_ARM = matches("arm");
        private static final boolean IS_OS_ARM64 = matches("aarch64");
        private static final boolean IS_OS_X86 = matches("x86") || matches("i386");
        private static final boolean IS_OS_X64 = matches("x86_64") || matches("amd64");

        private static String readOsArch() {
            try { return System.getProperty("os.arch"); }
            catch (SecurityException ignored) { return null; }
        }

        private static boolean matches(String prefix) {
            return OS_ARCH != null && OS_ARCH.startsWith(prefix);
        }
    }
}
