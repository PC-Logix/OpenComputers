package li.cil.oc.client;

import li.cil.oc.OpenComputers$;
import li.cil.oc.client.audio.AudioSession;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.PacketType;
import li.cil.oc.util.Audio;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Client packet routing and session state, with event details in focused Java handlers. */
public final class ClientPacketHandlerJava {
    private static final Map<Integer, AudioSession> audioSessions = new HashMap<>();

    private ClientPacketHandlerJava() { }

    public static Optional<Level> worldOptional(Player player, ResourceLocation dimension) {
        Level world = player.level();
        return world.dimension().location().equals(dimension) ? Optional.of(world) : Optional.empty();
    }

    public static void update() {
        synchronized (audioSessions) {
            for (AudioSession session : audioSessions.values()) session.update();
            Iterator<Map.Entry<Integer, AudioSession>> iterator = audioSessions.entrySet().iterator();
            while (iterator.hasNext()) {
                AudioSession session = iterator.next().getValue();
                if (session.checkFinished() && !session.loop()) {
                    session.cleanup();
                    iterator.remove();
                }
            }
        }
        ClientProjectorFrameHandler.applyPendingFrames();
    }

    public static void stopAllAudio() {
        synchronized (audioSessions) {
            List<AudioSession> sessions = new ArrayList<>(audioSessions.values());
            audioSessions.clear();
            for (AudioSession session : sessions) {
                session.stop();
                session.cleanup();
            }
        }
    }

    public static void clearPendingProjectorFrames() {
        ClientProjectorFrameHandler.clearPendingFrames();
    }

    public static void dispatch(PacketParser packet) {
        try {
            dispatchChecked(packet);
        } catch (IOException error) {
            ClientPacketHandlerJava.<RuntimeException>throwUnchecked(error);
        }
    }

    private static void dispatchChecked(PacketParser p) throws IOException {
        switch (p.packetType) {
            case AdapterState -> ClientDeviceStatePacketHandlers.adapterState(p);
            case Analyze -> ClientClipboardPacketHandlers.analyzerAddress(p);
            case AudioStart -> onAudioStart(p);
            case AudioChunk -> onAudioChunk(p);
            case AudioPlay -> onAudioPlay(p);
            case AudioPause -> onAudioPause(p);
            case AudioResume -> onAudioResume(p);
            case AudioStop -> onAudioStop(p);
            case AudioClose -> onAudioClose(p);
            case AudioSetLoop -> onAudioSetLoop(p);
            case TapeAudioStart -> onTapeAudioStart(p);
            case ChargerState -> ClientDeviceStatePacketHandlers.chargerState(p);
            case ClientLog -> OpenComputers$.MODULE$.log().info(p.readUTF());
            case Clipboard -> ClientClipboardPacketHandlers.clipboard(p);
            case ColorChange -> ClientDeviceStatePacketHandlers.colorChange(p);
            case MachineItemStateResponse -> ClientMenuPacketHandlers.machineItemState(p);
            case ComputerState -> ClientDeviceStatePacketHandlers.computerState(p);
            case ComputerUserList -> ClientDeviceStatePacketHandlers.computerUserList(p);
            case ContainerUpdate -> ClientMenuPacketHandlers.containerUpdate(p);
            case DisassemblerActiveChange -> ClientDeviceStatePacketHandlers.disassemblerActive(p);
            case FileSystemActivity -> ClientActivityPacketHandlers.fileSystemActivity(p,
                    dimension -> worldOptional(p.player, dimension));
            case FloppyChange -> ClientDeviceStatePacketHandlers.floppyChange(p);
            case HologramArea -> ClientBlockStatePacketHandlers.hologramArea(p);
            case HologramClear -> ClientBlockStatePacketHandlers.hologramClear(p);
            case HologramColor -> ClientBlockStatePacketHandlers.hologramColor(p);
            case HologramPowerChange -> ClientBlockStatePacketHandlers.hologramPower(p);
            case HologramRotation -> ClientBlockStatePacketHandlers.hologramRotation(p);
            case HologramRotationSpeed -> ClientBlockStatePacketHandlers.hologramRotationSpeed(p);
            case HologramScale -> ClientBlockStatePacketHandlers.hologramScale(p);
            case HologramTranslation -> ClientBlockStatePacketHandlers.hologramTranslation(p);
            case HologramValues -> ClientBlockStatePacketHandlers.hologramValues(p);
            case ProjectorFrame -> ClientProjectorFrameHandler.receiveFrame(p);
            case ProjectorPowerChange -> ClientBlockStatePacketHandlers.projectorPower(p);
            case LootDisksReset -> ClientLootPacketHandlers.resetLootDisks();
            case LootDisk -> ClientLootPacketHandlers.lootDisk(p);
            case CyclingDisk -> ClientLootPacketHandlers.cyclingDisk(p);
            case LootEEPROMsReset -> ClientLootPacketHandlers.resetLootEeproms();
            case LootEEPROM -> ClientLootPacketHandlers.lootEeprom(p);
            case NanomachinesConfiguration -> ClientNanomachinePacketHandlers.configuration(p);
            case NanomachinesInputs -> ClientNanomachinePacketHandlers.inputs(p);
            case NanomachinesPower -> ClientNanomachinePacketHandlers.power(p);
            case NetSplitterState -> ClientDeviceStatePacketHandlers.netSplitterState(p);
            case NetworkActivity -> ClientActivityPacketHandlers.networkActivity(p,
                    dimension -> worldOptional(p.player, dimension));
            case ParticleEffect -> ClientVisualPacketHandlers.particleEffect(p,
                    worldOptional(p.player, ResourceLocation.tryParse(p.readUTF())));
            case PetVisibility -> ClientVisualPacketHandlers.petVisibility(p);
            case PowerState -> ClientDeviceStatePacketHandlers.powerState(p);
            case PrinterState -> ClientDeviceStatePacketHandlers.printerState(p);
            case RackInventory -> ClientRackPacketHandlers.inventory(p);
            case RackMountableData -> ClientRackPacketHandlers.mountableData(p);
            case RaidStateChange -> ClientDeviceStatePacketHandlers.raidStateChange(p);
            case RedstoneState -> ClientDeviceStatePacketHandlers.redstoneState(p);
            case RobotAnimateSwing -> ClientDeviceStatePacketHandlers.robotAnimateSwing(p);
            case RobotAnimateTurn -> ClientDeviceStatePacketHandlers.robotAnimateTurn(p);
            case RobotAssemblingState -> ClientDeviceStatePacketHandlers.robotAssemblingState(p);
            case RobotInventoryChange -> ClientDeviceStatePacketHandlers.robotInventoryChange(p);
            case RobotLightChange -> ClientDeviceStatePacketHandlers.robotLightChange(p);
            case RobotFlagChange -> ClientDeviceStatePacketHandlers.robotFlagChange(p);
            case RobotMove -> ClientDeviceStatePacketHandlers.robotMove(p);
            case RobotNameChange -> ClientDeviceStatePacketHandlers.robotNameChange(p);
            case RobotSelectedSlotChange -> ClientDeviceStatePacketHandlers.robotSelectedSlotChange(p);
            case RotatableState -> ClientDeviceStatePacketHandlers.rotatableState(p);
            case SwitchActivity -> ClientDeviceStatePacketHandlers.switchActivity(p);
            case TextBufferInit -> ClientTextBufferPacketHandlers.initialize(p);
            case TextBufferPowerChange -> ClientTextBufferPacketHandlers.powerChange(p);
            case TextBufferMulti -> ClientTextBufferPacketHandlers.multi(p);
            case ScreenTouchMode -> ClientBlockStatePacketHandlers.screenTouchMode(p);
            case SoundEffect -> onSoundEffect(p);
            case Sound -> onSound(p);
            case SoundPattern -> onSoundPattern(p);
            case ComputronicsTone -> onComputronicsTone(p);
            case TransposerActivity -> ClientDeviceStatePacketHandlers.transposerActivity(p);
            case WaypointLabel -> ClientDeviceStatePacketHandlers.waypointLabel(p);
            default -> { /* Invalid packet. */ }
        }
    }

    private static void onAudioStart(PacketParser p) throws IOException {
        int handle = p.readInt();
        int channel = p.readInt();
        int sampleRate = p.readInt();
        int channels = p.readInt();
        int format = p.readInt();
        boolean loop = p.readBoolean();
        Vec3 pos = new Vec3(p.readDouble(), p.readDouble(), p.readDouble());
        OpenComputers$.MODULE$.log().info("Audio stream start: handle={}, sampleRate={}, channels={}, format={}, loop={}",
                handle, sampleRate, channels, format, loop);
        AudioSession session = new AudioSession(handle, channel, sampleRate, channels, format, pos, false, 1.0f);
        session.setLoopMode(loop);
        synchronized (audioSessions) { audioSessions.put(handle, session); }
    }

    private static void onTapeAudioStart(PacketParser p) throws IOException {
        int handle = p.readInt();
        int sampleRate = p.readInt();
        float volume = p.readFloat();
        Vec3 pos = new Vec3(p.readDouble(), p.readDouble(), p.readDouble());
        OpenComputers$.MODULE$.log().info("Tape audio stream start: handle={}, sampleRate={}, volume={}", handle, sampleRate, volume);
        AudioSession session = new AudioSession(handle, 0, sampleRate, 1, org.lwjgl.openal.AL10.AL_FORMAT_MONO8,
                pos, true, volume);
        synchronized (audioSessions) {
            AudioSession old = audioSessions.remove(handle);
            if (old != null) old.cleanup();
            audioSessions.put(handle, session);
        }
    }

    private static void onComputronicsTone(PacketParser p) throws IOException {
        double x = p.readDouble(), y = p.readDouble(), z = p.readDouble();
        int mode = p.readUnsignedByte(), frequency = p.readShort();
        int duration = p.readUnsignedShort(), delay = p.readUnsignedShort();
        float volume = p.readFloat();
        int fmFrequency = p.readUnsignedShort();
        float fmIntensity = p.readFloat();
        int amFrequency = p.readUnsignedShort(), attack = p.readUnsignedShort(), decay = p.readUnsignedShort();
        float sustain = p.readFloat();
        int release = p.readUnsignedShort();
        Audio.playWave((float) x, (float) y, (float) z, mode, frequency, duration, delay, volume,
                fmFrequency, fmIntensity, amFrequency, attack, decay, sustain, release);
    }

    private static void onAudioChunk(PacketParser p) throws IOException {
        int handle = p.readInt();
        byte[] data = p.readByteArray();
        synchronized (audioSessions) {
            AudioSession session = audioSessions.get(handle);
            if (session != null) session.append(data);
        }
    }

    private static void onAudioPlay(PacketParser p) throws IOException { withAudioSession(p.readInt(), AudioSession::play); }
    private static void onAudioPause(PacketParser p) throws IOException { withAudioSession(p.readInt(), AudioSession::pause); }
    private static void onAudioResume(PacketParser p) throws IOException { withAudioSession(p.readInt(), AudioSession::resume); }

    private static void onAudioStop(PacketParser p) throws IOException {
        int handle = p.readInt();
        synchronized (audioSessions) {
            AudioSession session = audioSessions.remove(handle);
            if (session != null) { session.stop(); session.cleanup(); }
        }
    }

    private static void onAudioClose(PacketParser p) throws IOException {
        int handle = p.readInt();
        synchronized (audioSessions) {
            AudioSession session = audioSessions.remove(handle);
            if (session != null) session.cleanup();
        }
    }

    private static void onAudioSetLoop(PacketParser p) throws IOException {
        int handle = p.readInt();
        boolean loop = p.readBoolean();
        synchronized (audioSessions) {
            AudioSession session = audioSessions.get(handle);
            if (session != null) session.setLoopMode(loop);
        }
    }

    private static void withAudioSession(int handle, java.util.function.Consumer<AudioSession> action) {
        synchronized (audioSessions) {
            AudioSession session = audioSessions.get(handle);
            if (session != null) action.accept(session);
        }
    }

    private static void onSoundEffect(PacketParser p) throws IOException {
        Optional<Level> selectedWorld = worldOptional(p.player, ResourceLocation.tryParse(p.readUTF()));
        if (selectedWorld.isEmpty()) return;
        double x = p.readDouble(), y = p.readDouble(), z = p.readDouble();
        String sound = p.readUTF();
        SoundSource category = SoundSource.values()[p.readByte()];
        float range = p.readFloat();
        selectedWorld.get().playSound(p.player, x, y, z,
                SoundEvent.createVariableRangeEvent(ResourceLocation.tryParse(sound)), category, range / 15 + 0.5F, 1.0F);
    }

    private static void onSound(PacketParser p) throws IOException {
        if (worldOptional(p.player, ResourceLocation.tryParse(p.readUTF())).isEmpty()) return;
        int x = p.readInt(), y = p.readInt(), z = p.readInt();
        short frequency = p.readShort(), duration = p.readShort();
        Audio.play(x + 0.5f, y + 0.5f, z + 0.5f, frequency, duration);
    }

    private static void onSoundPattern(PacketParser p) throws IOException {
        if (worldOptional(p.player, ResourceLocation.tryParse(p.readUTF())).isEmpty()) return;
        int x = p.readInt(), y = p.readInt(), z = p.readInt();
        String pattern = p.readUTF();
        Audio.play(x + 0.5f, y + 0.5f, z + 0.5f, pattern,
                1000, 200);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable error) throws T {
        throw (T) error;
    }
}
