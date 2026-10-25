package li.cil.oc.integration;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.integration.appeng.ModAppliedEnergistics2;
import li.cil.oc.integration.computercraft.ModComputerCraft;
import li.cil.oc.integration.create.ModCreate;
import li.cil.oc.integration.enderstorage.ModEnderStorage;
import li.cil.oc.integration.immersiveengineering.ModImmersiveEngineering;
import li.cil.oc.integration.mekanism.ModMekanism;
import li.cil.oc.integration.minecraft.ModMinecraft;
import li.cil.oc.integration.multipart.ModCBMultipart;
import li.cil.oc.integration.neoforge.ModNeoForge;
import li.cil.oc.integration.opencomputers.ModOpenComputers;
import li.cil.oc.integration.projectred.ModProjectRed;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.MavenVersionAdapter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import scala.Option;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.ArrayBuffer$;
import scala.jdk.javaapi.CollectionConverters;
import scala.jdk.javaapi.OptionConverters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Mods {
    private static boolean preInited;
    private static boolean inited;
    private static final List<ModBase> KNOWN_MODS = new ArrayList<>();

    private static final SimpleMod CREATE = new SimpleMod(IDs.Create);
    private static final SimpleMod SABLE = new SimpleMod(IDs.Sable, "[2.0,3)");
    private static final SimpleMod COMPUTER_CRAFT = new SimpleMod(IDs.ComputerCraft);
    private static final SimpleMod NEO_FORGE = new SimpleMod(IDs.Forge);
    private static final SimpleMod JUST_ENOUGH_ITEMS = new SimpleMod(IDs.JustEnoughItems);
    private static final SimpleMod APPLIED_ENERGISTICS_2 = new SimpleMod(IDs.AppliedEnergistics2);
    private static final SimpleMod MEKANISM = new SimpleMod(IDs.Mekanism);
    private static final SimpleMod MINECRAFT = new SimpleMod(IDs.Minecraft);
    private static final SimpleMod OPEN_COMPUTERS = new SimpleMod(IDs.OpenComputers);
    private static final SimpleMod ENDER_STORAGE = new SimpleMod(IDs.EnderStorage);
    private static final SimpleMod CB_MULTIPART = new SimpleMod(IDs.CBMultipart);
    private static final SimpleMod PROJECT_RED_TRANSMISSION = new SimpleMod(IDs.ProjectRedTransmission);
    private static final SimpleMod IMMERSIVE_ENGINEERING = new SimpleMod(IDs.ImmersiveEngineering);

    // Keep the original order: OpenComputers initializes after the optional integrations.
    private static final ModProxy[] PROXIES = {
        ModCreate.INSTANCE,
        ModNeoForge.INSTANCE,
        ModMekanism.INSTANCE,
        ModMinecraft.INSTANCE,
        ModComputerCraft.INSTANCE,
        ModAppliedEnergistics2.INSTANCE,
        ModEnderStorage.INSTANCE,
        ModCBMultipart.INSTANCE,
        ModProjectRed.INSTANCE,
        ModImmersiveEngineering.INSTANCE,
        ModOpenComputers.INSTANCE
    };

    public static List<ModBase> allJava() {
        return List.copyOf(KNOWN_MODS);
    }

    /** Scala compatibility view; Java callers should use {@link #allJava()}. */
    @Deprecated
    public static ArrayBuffer<ModBase> All() {
        return ArrayBuffer$.MODULE$.from(CollectionConverters.asScala(KNOWN_MODS));
    }

    public static SimpleMod Create() { return CREATE; }
    public static SimpleMod Sable() { return SABLE; }
    public static SimpleMod ComputerCraft() { return COMPUTER_CRAFT; }
    public static SimpleMod NeoForge() { return NEO_FORGE; }
    public static SimpleMod JustEnoughItems() { return JUST_ENOUGH_ITEMS; }
    public static SimpleMod AppliedEnergistics2() { return APPLIED_ENERGISTICS_2; }
    public static SimpleMod Mekanism() { return MEKANISM; }
    public static SimpleMod Minecraft() { return MINECRAFT; }
    public static SimpleMod OpenComputers() { return OPEN_COMPUTERS; }
    public static SimpleMod EnderStorage() { return ENDER_STORAGE; }
    public static SimpleMod CBMultipart() { return CB_MULTIPART; }
    public static SimpleMod ProjectRedTransmission() { return PROJECT_RED_TRANSMISSION; }
    public static SimpleMod ImmersiveEngineering() { return IMMERSIVE_ENGINEERING; }
    public static ModProxy[] Proxies() { return PROXIES; }

    public static void preInit() {
        if (preInited) return;
        preInited = true;
        for (ModProxy proxy : PROXIES) tryPreInit(proxy);
    }

    private static void tryPreInit(ModProxy proxy) {
        Set<ModProxy> handlers = new HashSet<>();
        Mod mod = proxy.getMod();
        boolean blacklisted = Settings.get().modBlacklist().contains(mod.id());
        boolean alwaysEnabled = mod == null || mod == MINECRAFT;
        if (!blacklisted && (alwaysEnabled || mod.isModAvailable()) && handlers.add(proxy)) {
            OpenComputers.log().debug("Pre-initializing mod integration for '{}'.", mod.id());
            try {
                proxy.preInitialize();
            } catch (Throwable exception) {
                OpenComputers.log().warn("Error pre-initializing integration for '{}'", mod.id(), exception);
            }
        }
    }

    public static void init() {
        if (inited) return;
        inited = true;
        for (ModProxy proxy : PROXIES) tryInit(proxy);
    }

    private static void tryInit(ModProxy proxy) {
        Set<ModProxy> handlers = new HashSet<>();
        Mod mod = proxy.getMod();
        boolean blacklisted = Settings.get().modBlacklist().contains(mod.id());
        boolean alwaysEnabled = mod == null || mod == MINECRAFT;
        if (!blacklisted && (alwaysEnabled || mod.isModAvailable()) && handlers.add(proxy)) {
            OpenComputers.log().debug("Initializing mod integration for '{}'.", mod.id());
            try {
                proxy.initialize();
            } catch (Throwable exception) {
                OpenComputers.log().warn("Error initializing integration for '{}'", mod.id(), exception);
            }
        }
    }

    public static final class IDs {
        public static final String Create = "create";
        public static final String Sable = "sable";
        public static final String ComputerCraft = "computercraft";
        public static final String Forge = "forge";
        public static final String JustEnoughItems = "jei";
        public static final String AppliedEnergistics2 = "ae2";
        public static final String Mekanism = "mekanism";
        public static final String Minecraft = "minecraft";
        public static final String OpenComputers = "opencomputers";
        public static final String EnderStorage = "enderstorage";
        public static final String CBMultipart = "cb_multipart";
        public static final String ProjectRedTransmission = "projectred_transmission";
        public static final String ImmersiveEngineering = "immersiveengineering";

        private IDs() {
        }
    }

    public interface ModBase extends Mod {
        default Optional<ModContainer> containerOptional() {
            return ModList.get().getModContainerById(id()).map(container -> (ModContainer) container);
        }

        default Optional<ArtifactVersion> versionOptional() {
            return containerOptional().map(container -> container.getModInfo().getVersion());
        }

        /** Scala compatibility accessor; Java callers should use {@link #containerOptional()}. */
        @Deprecated
        default Option<ModContainer> container() {
            return OptionConverters.toScala(containerOptional());
        }

        /** Scala compatibility accessor; Java callers should use {@link #versionOptional()}. */
        @Deprecated
        default Option<ArtifactVersion> version() {
            return OptionConverters.toScala(versionOptional());
        }
    }

    public static class SimpleMod implements ModBase {
        private final String id;
        private final String version;
        private volatile Boolean available;

        public SimpleMod(String id) {
            this(id, "");
        }

        public SimpleMod(String id, String version) {
            this.id = id;
            this.version = version;
            KNOWN_MODS.add(this);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public synchronized boolean isModAvailable() {
            if (available == null) {
                available = ModList.get().getModContainerById(id)
                    .map(container -> version.isEmpty() || MavenVersionAdapter.createFromVersionSpec(version)
                        .containsVersion(container.getModInfo().getVersion()))
                    .orElse(false);
            }
            return available;
        }
    }

    public static class ClassBasedMod implements ModBase {
        private final String id;
        private final String[] classNames;
        private volatile Boolean available;

        public ClassBasedMod(String id, String... classNames) {
            this.id = id;
            this.classNames = classNames;
            KNOWN_MODS.add(this);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public synchronized boolean isModAvailable() {
            if (available == null) {
                boolean loaded = ModList.get().isLoaded(id);
                if (loaded) {
                    for (String className : classNames) {
                        try {
                            Class.forName(className);
                        } catch (Throwable ignored) {
                            loaded = false;
                            break;
                        }
                    }
                }
                available = loaded;
            }
            return available;
        }
    }

    private Mods() {
    }
}
