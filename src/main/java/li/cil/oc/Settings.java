package li.cil.oc;

import com.typesafe.config.Config;
import li.cil.oc.api.internal.TextBuffer.ColorDepth;
import li.cil.oc.server.component.DebugCardAccessContext;
import scala.Option;
import scala.Tuple2;
import scala.jdk.javaapi.OptionConverters;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Configuration loader and values shared by Java code and the retained audio implementations. */
public final class Settings extends SettingsValuesJava {
    private final float soundVolume;
    private final int beepSampleRate;
    private final int beepAmplitude;
    private final float beepRadius;
    private final DebugCardAccess debugCardAccess;
    private final double maxNetworkClientSoundPacketDistance;
    private final int audioCardChunkSize;
    private final boolean audioCardEnablePcm;
    private final int audioCardBufferLimit;
    private final int audioCardSampleRate;
    private final int audioCardFormat;

    public Settings(Config config) {
        super(config);
        soundVolume = Math.min(2, Math.max(0, (float) config.getDouble("client.soundVolume")));
        beepSampleRate = config.getInt("client.beepSampleRate");
        beepAmplitude = Math.min(Byte.MAX_VALUE, Math.max(0, config.getInt("client.beepVolume")));
        beepRadius = Math.min(32, Math.max(1, (float) config.getDouble("client.beepRadius")));
        debugCardAccess = debugSettings.debugCardAccess();
        maxNetworkClientSoundPacketDistance = Math.max(0, config.getDouble("misc.maxNetworkClientSoundPacketDistance"));
        audioCardChunkSize = Math.max(0, config.getInt("audio.chunkSize"));
        audioCardEnablePcm = config.getBoolean("audio.enablePcm");
        audioCardBufferLimit = Math.max(0, config.getInt("audio.bufferLimit"));
        audioCardSampleRate = Math.max(0, Math.min(48000, config.getInt("audio.sampleRate")));
        audioCardFormat = config.getInt("audio.format");
    }
    public float soundVolume() { return soundVolume; }
    public int beepSampleRate() { return beepSampleRate; }
    public int beepAmplitude() { return beepAmplitude; }
    public float beepRadius() { return beepRadius; }
    public DebugCardAccess debugCardAccess() { return debugCardAccess; }
    public double maxNetworkClientSoundPacketDistance() { return maxNetworkClientSoundPacketDistance; }
    public int audioCardChunkSize() { return audioCardChunkSize; }
    public boolean audioCardEnablePcm() { return audioCardEnablePcm; }
    public int audioCardBufferLimit() { return audioCardBufferLimit; }
    public int audioCardSampleRate() { return audioCardSampleRate; }
    public int audioCardFormat() { return audioCardFormat; }

    private static volatile Settings current;
    private static final ColorDepth[] SCREEN_DEPTHS = {ColorDepth.OneBit, ColorDepth.FourBit, ColorDepth.EightBit, ColorDepth.SixteenBit};
    @SuppressWarnings("unchecked")
    private static final Tuple2<Object, Object>[] SCREEN_RESOLUTIONS = new Tuple2[]{new Tuple2<>(50, 16), new Tuple2<>(80, 25), new Tuple2<>(160, 50), new Tuple2<>(190, 60)};
    private static final int[] DEVICE_COMPLEXITY = {12, 24, 32, 48, 9001};
    private static boolean rTreeDebugRenderer;
    private static int blockRenderId = -1;
    public static String resourceDomain() { return OpenComputers.ID; }
    public static String namespace() { return "oc:"; }
    public static String savePath() { return "opencomputers/"; }
    public static String scriptPath() { return "/assets/" + resourceDomain() + "/lua/"; }
    public static Tuple2<Object, Object>[] screenResolutionsByTier() { return SCREEN_RESOLUTIONS; }
    public static ColorDepth[] screenDepthsByTier() { return SCREEN_DEPTHS; }
    public static int[] deviceComplexityByTier() { return DEVICE_COMPLEXITY; }
    public static boolean rTreeDebugRenderer() { return rTreeDebugRenderer; }
    public static void rTreeDebugRenderer_$eq(boolean value) { rTreeDebugRenderer = value; }
    public static int blockRenderId() { return blockRenderId; }
    public static void blockRenderId_$eq(int value) { blockRenderId = value; }
    public static int basicScreenPixels() { return ((Number) SCREEN_RESOLUTIONS[0]._1()).intValue() * ((Number) SCREEN_RESOLUTIONS[0]._2()).intValue(); }
    public static Settings get() { return current; }
    public static void load(File file) {
        Config defaults;
        try { defaults = SettingsFileJava.loadDefaults(); }
        catch (IOException error) { throw propagate(error); }
        Config config;
        try {
            config = SettingsFileJava.parseFile(file, defaults);
            current = new Settings(config.getConfig("opencomputers"));
        } catch (Throwable error) {
            if (file.exists()) throw new RuntimeException("Error parsing configuration file. To restore defaults, delete '" + file.getName() + "' and restart the game.", error);
            current = new Settings(defaults.getConfig("opencomputers"));
            config = defaults;
        }
        SettingsAccessJava.install(current);
        SettingsConfigValidation.validateRemovedListOptions(config);
        SettingsFileJava.saveConfig(file, config);
    }
    public static List<Integer> getIntList(Config config, String path) { return getIntList(config, path, Optional.empty()); }
    public static List<Integer> getIntList(Config config, String path, Optional<List<Integer>> fallback) {
        return config.hasPath(path) ? config.getIntList(path) : fallback.orElseGet(LinkedList::new);
    }
    public static List<Integer> getIntList(Config config, String path, Option<List<Integer>> fallback) { return getIntList(config, path, OptionConverters.toJava(fallback)); }
    public static Option<List<Integer>> getIntList$default$3() { return Option.empty(); }

    public sealed interface DebugCardAccess {
        Optional<String> checkAccessJava(Optional<DebugCardAccessContext> context);
        default Option<String> checkAccess(Option<DebugCardAccessContext> context) { return OptionConverters.toScala(checkAccessJava(OptionConverters.toJava(context))); }
        enum Forbidden implements DebugCardAccess {
            INSTANCE;
            @Override public Optional<String> checkAccessJava(Optional<DebugCardAccessContext> context) { return Optional.of("debug card is disabled"); }
        }
        enum Allowed implements DebugCardAccess {
            INSTANCE;
            @Override public Optional<String> checkAccessJava(Optional<DebugCardAccessContext> context) { return Optional.empty(); }
        }
        final class Whitelist implements DebugCardAccess {
            private final File noncesFile;
            private final DebugCardAccessWhitelistJava accessState;
            public Whitelist(File noncesFile) {
                this.noncesFile = noncesFile;
                try { accessState = new DebugCardAccessWhitelistJava(noncesFile); accessState.load(); }
                catch (IOException | java.security.NoSuchAlgorithmException error) { throw propagate(error); }
            }
            public File noncesFile() { return noncesFile; }
            public void save() { try { accessState.save(); } catch (IOException error) { throw propagate(error); } }
            public void load() { try { accessState.load(); } catch (IOException error) { throw propagate(error); } }
            public Optional<String> nonceOptional(String player) { return accessState.nonce(player); }
            public Option<String> nonce(String player) { return OptionConverters.toScala(nonceOptional(player)); }
            public boolean isWhitelisted(String player) { return accessState.isWhitelisted(player); }
            public Set<String> whitelist() { return accessState.whitelist(); }
            public void add(String player) { try { accessState.add(player); } catch (IOException error) { throw propagate(error); } }
            public void remove(String player) { try { accessState.remove(player); } catch (IOException error) { throw propagate(error); } }
            public void invalidate(String player) { try { accessState.invalidate(player); } catch (IOException error) { throw propagate(error); } }
            @Override public Optional<String> checkAccessJava(Optional<DebugCardAccessContext> context) { return accessState.checkAccess(context); }
            @Override public boolean equals(Object other) { return other instanceof Whitelist whitelist && noncesFile.equals(whitelist.noncesFile); }
            @Override public int hashCode() { return noncesFile.hashCode(); }
            @Override public String toString() { return "Whitelist(" + noncesFile + ")"; }
        }
    }
    private static RuntimeException propagate(Throwable error) { Settings.<RuntimeException>throwUnchecked(error); return new IllegalStateException(error); }
    @SuppressWarnings("unchecked") private static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }
}
