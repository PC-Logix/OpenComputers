package li.cil.oc.integration.multipart;

import li.cil.oc.OpenComputers;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.Optional;

/** Kept free of CB Multipart API types so OpenComputers can load without CB Multipart. */
public final class ModCBMultipart implements ModProxy {
    public static final ModCBMultipart INSTANCE = new ModCBMultipart();

    private ModCBMultipart() {
    }

    @Override
    public Mod getMod() {
        return Mods.CBMultipart();
    }

    @Override
    public void preInitialize() {
        final IEventBus bus = OpenComputers.proxy().modBus();
        MultipartIntegration.register(bus);
        if (FMLEnvironment.dist == Dist.CLIENT) registerClient(bus);
    }

    public static Optional<Integer> cableColor(BlockGetter world, BlockPos pos) {
        return MultipartColorLookup.cableColor(world, pos);
    }

    public static boolean isCable(BlockGetter world, BlockPos pos) {
        return Mods.CBMultipart().isModAvailable() && MultipartColorLookup.cablePart(world, pos).isPresent();
    }

    public static boolean canConnectFromSide(BlockGetter world, BlockPos pos, Direction side) {
        return !Mods.CBMultipart().isModAvailable() || MultipartColorLookup.canConnectFromSide(world, pos, side);
    }

    public static boolean canAudioConnectFromSide(BlockGetter world, BlockPos pos, Direction side) {
        return !Mods.CBMultipart().isModAvailable() || MultipartColorLookup.canAudioConnectFromSide(world, pos, side);
    }

    public static boolean isAudioCable(BlockGetter world, BlockPos pos) {
        return Mods.CBMultipart().isModAvailable() && MultipartColorLookup.isAudioCable(world, pos);
    }

    private static void registerClient(IEventBus bus) {
        try {
            final Class<?> clientIntegration = Class.forName("li.cil.oc.integration.multipart.MultipartClientIntegration$");
            final Object instance = clientIntegration.getField("MODULE$").get(null);
            clientIntegration.getMethod("register", IEventBus.class).invoke(instance, bus);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Failed to register CB Multipart client integration", exception);
        }
    }
}
