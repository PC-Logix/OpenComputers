package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.datacomponents.OCComponents;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import org.lwjgl.openal.AL10;
import scala.Option;
import scala.Option$;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Set$;
import scala.jdk.javaapi.CollectionConverters;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioCard extends AbstractManagedEnvironment implements DeviceInfo {
    // Handles are process wide because the client indexes audio packets by handle alone.
    private static final AtomicInteger NEXT_NETWORK_HANDLE = new AtomicInteger(1);

    private static int allocateHandle() {
        return NEXT_NETWORK_HANDLE.getAndIncrement();
    }

    /** Sends a completed mono8 stream for block devices such as tape drives. */
    public static Option<Integer> playPcm(EnvironmentHost host, byte[] pcm, int sampleRate, int packetSize) {
        if (!Settings.get().audioCardEnablePcm() || pcm.length == 0) return Option$.MODULE$.empty();
        int handle = allocateHandle();
        // Computronics used 1024 encoded bytes, decoded to one 8192 byte OpenAL buffer.
        int chunkSize = Math.max(1, packetSize > 0 ? packetSize * 8 : Settings.get().audioCardChunkSize());
        PacketSender.sendAudioStart(host, handle, 0, Math.max(1, Math.min(96000, sampleRate)),
            1, AL10.AL_FORMAT_MONO8, false);
        for (int offset = 0; offset < pcm.length; offset += chunkSize) {
            PacketSender.sendAudioChunk(host, handle,
                Arrays.copyOfRange(pcm, offset, Math.min(pcm.length, offset + chunkSize)));
        }
        PacketSender.sendAudioPlay(host, handle);
        return Option$.MODULE$.apply(handle);
    }

    /** Sends encoded DFPWM packets to the client's decoder. */
    public static Option<Integer> playDfpwm(EnvironmentHost host, byte[] encoded, int sampleRate, float volume) {
        if (!Settings.get().audioCardEnablePcm() || encoded.length == 0) return Option$.MODULE$.empty();
        int handle = allocateHandle();
        PacketSender.sendTapeAudioStart(host, handle, Math.max(1, Math.min(96000, sampleRate)),
            Math.max(0f, Math.min(1f, volume)));
        for (int offset = 0; offset < encoded.length; offset += 1024) {
            PacketSender.sendAudioChunk(host, handle,
                Arrays.copyOfRange(encoded, offset, Math.min(encoded.length, offset + 1024)));
        }
        PacketSender.sendAudioPlay(host, handle);
        return Option$.MODULE$.apply(handle);
    }

    private final EnvironmentHost host;
    private final Map<String, Set<Integer>> owners = new HashMap<>();
    private final Map<Integer, AudioCardSession> sessions = new HashMap<>();
    private final SynthChannel[] synthChannels = new SynthChannel[8];
    private static final String[] SYNTH_MODES = {"square", "sine", "triangle", "sawtooth", "noise"};
    private double totalSynthVolume = 1.0;

    public AudioCard(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Neighbors).withComponent("audio").withConnector().create());
        for (int i = 0; i < synthChannels.length; i++) synthChannels[i] = new SynthChannel();
    }

    private static final class SynthChannel {
        int mode;
        int frequency = 440;
        double volume = 1.0;
        int fmChannel = -1;
        double fmIntensity;
        int amChannel = -1;
        int attack;
        int decay;
        double sustain = 1.0;
        int release;
    }

    private SynthChannel synthChannel(int index) {
        if (index < 1 || index > synthChannels.length) throw new IllegalArgumentException("channel must be in [1, 8]");
        return synthChannels[index - 1];
    }

    private void playSynth(int channel, int duration, int delay) {
        SynthChannel current = synthChannel(channel);
        int fm = current.fmChannel >= 0 ? synthChannels[current.fmChannel].frequency : 0;
        int am = current.amChannel >= 0 ? synthChannels[current.amChannel].frequency : 0;
        var level = host.getEnvironmentLevel();
        if (level != null && !level.isClientSide()) {
            PacketSender.sendComputronicsTone(level, host.xPosition(), host.yPosition(), host.zPosition(),
                current.mode, clamp(current.frequency, 20, 2000), clamp(duration, 50, 5000),
                clamp(delay, 0, 16000), current.volume * totalSynthVolume,
                fm, current.fmIntensity, am, current.attack, current.decay, current.sustain, current.release);
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private int chunkSize() {
        return Math.max(1, Settings.get().audioCardChunkSize());
    }

    private int bufferLimit() {
        return Math.max(chunkSize(), Settings.get().audioCardBufferLimit());
    }

    private AudioCardSession session(int handle) {
        AudioCardSession value = sessions.get(handle);
        if (value == null) throw new IllegalArgumentException("invalid handle");
        return value;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Multimedia,
            DeviceAttribute.Description, "Audio Streaming Interface",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.ViridiaComputronics(),
            DeviceAttribute.Product, "WaveBlaster Zero");
    }

    @Callback(direct = true, doc = "function([channel:number, sampleRate:number, mode:string]):userdata -- open an audio buffer handle.\n"
        + "  - channel: output channel index (default: 0)\n"
        + "  - sampleRate: samples per second (default: server config value)\n"
        + "  - mode: PCM format string (default: \"mono8\")\n"
        + "      \"mono8\"    -- mono,   8-bit unsigned  (DFPWM output)\n"
        + "      \"mono16\"   -- mono,   16-bit signed little-endian\n"
        + "      \"stereo8\"  -- stereo, 8-bit unsigned\n"
        + "      \"stereo16\" -- stereo, 16-bit signed little-endian (WAV stereo)\n")
    public synchronized Object[] open(Context context, Arguments args) throws IOException {
        if (!Settings.get().audioCardEnablePcm()) return ResultWrapper.result(null, "PCM streaming is disabled by the server");
        Set<Integer> owned = owners.get(context.node().address());
        if (owned != null && owned.size() >= Settings.get().maxHandles()) throw new IOException("too many open handles");
        int channel = args.optInteger(0, 0);
        int sampleRate = args.optInteger(1, Settings.get().audioCardSampleRate());
        String mode = args.optString(2, "mono8");
        int handle = allocateHandle();
        sessions.put(handle, new AudioCardSession(handle, channel, sampleRate, mode));
        owners.computeIfAbsent(context.node().address(), ignored -> new HashSet<>()).add(handle);
        return ResultWrapper.result(new AudioHandleValue(node().address(), handle));
    }

    @Callback(direct = true, doc = "function():table -- available synthesizer wave modes.")
    public Object[] modes(Context context, Arguments args) {
        Map<Object, Object> modes = new HashMap<>();
        for (int i = 0; i < SYNTH_MODES.length; i++) {
            modes.put(i + 1, SYNTH_MODES[i]);
            modes.put(SYNTH_MODES[i], i + 1);
        }
        return ResultWrapper.result(modes);
    }

    @Callback(direct = true, doc = "function():number -- number of synthesizer channels.")
    public Object[] channel_count(Context context, Arguments args) {
        return ResultWrapper.result(synthChannels.length);
    }

    @Callback(direct = true, doc = "function(volume:number):boolean -- set overall synthesizer volume.")
    public Object[] setTotalVolume(Context context, Arguments args) {
        totalSynthVolume = Math.max(0, Math.min(1, args.checkDouble(0)));
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number, wave:number):boolean -- set a synthesizer wave.")
    public Object[] setWave(Context context, Arguments args) {
        SynthChannel current = synthChannel(args.checkInteger(0));
        int wave = args.checkInteger(1) - 1;
        if (wave < 0 || wave >= SYNTH_MODES.length) throw new IllegalArgumentException("unknown wave mode");
        current.mode = wave;
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number, initial:number, mask:number):boolean -- configure legacy LFSR noise.")
    public Object[] setLFSR(Context context, Arguments args) {
        synthChannel(args.checkInteger(0)).mode = 4;
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number, frequency:number):boolean -- set a synthesizer frequency.")
    public Object[] setFrequency(Context context, Arguments args) {
        synthChannel(args.checkInteger(0)).frequency = clamp(args.checkInteger(1), 20, 2000);
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number, volume:number):boolean -- set a synthesizer channel volume.")
    public Object[] setVolume(Context context, Arguments args) {
        synthChannel(args.checkInteger(0)).volume = Math.max(0, Math.min(1, args.checkDouble(1)));
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number, attack:number, decay:number, sustain:number, release:number):boolean -- set ADSR in milliseconds.")
    public Object[] setADSR(Context context, Arguments args) {
        SynthChannel current = synthChannel(args.checkInteger(0));
        current.attack = clamp(args.checkInteger(1), 0, 5000);
        current.decay = clamp(args.checkInteger(2), 0, 5000);
        current.sustain = Math.max(0, Math.min(1, args.checkDouble(3)));
        current.release = clamp(args.checkInteger(4), 0, 5000);
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number):boolean -- reset ADSR envelope.")
    public Object[] resetEnvelope(Context context, Arguments args) {
        SynthChannel current = synthChannel(args.checkInteger(0));
        current.attack = 0;
        current.decay = 0;
        current.sustain = 1;
        current.release = 0;
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number, modulator:number, intensity:number):boolean -- set frequency modulation.")
    public Object[] setFM(Context context, Arguments args) {
        SynthChannel current = synthChannel(args.checkInteger(0));
        current.fmChannel = args.checkInteger(1) - 1;
        synthChannel(current.fmChannel + 1);
        current.fmIntensity = args.checkDouble(2);
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number):boolean -- disable frequency modulation.")
    public Object[] resetFM(Context context, Arguments args) {
        synthChannel(args.checkInteger(0)).fmChannel = -1;
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number, modulator:number):boolean -- set amplitude modulation.")
    public Object[] setAM(Context context, Arguments args) {
        SynthChannel current = synthChannel(args.checkInteger(0));
        current.amChannel = args.checkInteger(1) - 1;
        synthChannel(current.amChannel + 1);
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number):boolean -- disable amplitude modulation.")
    public Object[] resetAM(Context context, Arguments args) {
        synthChannel(args.checkInteger(0)).amChannel = -1;
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(channel:number, duration:number[, delay:number]):boolean -- play one synthesized channel; durations are milliseconds.")
    public Object[] playSynthesized(Context context, Arguments args) {
        playSynth(args.checkInteger(0), args.checkInteger(1), args.optInteger(2, 0));
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(table):boolean -- play up to eight frequency-duration beep pairs.")
    public Object[] beep(Context context, Arguments args) {
        Map<?, ?> entries = args.checkTable(0);
        if (entries.size() > 8) return ResultWrapper.result(false, "table must not contain more than 8 frequencies");
        int index = 0;
        for (Map.Entry<?, ?> entry : entries.entrySet()) {
            Number frequency = (Number) entry.getKey();
            Number duration = (Number) entry.getValue();
            SynthChannel current = synthChannels[index];
            current.frequency = clamp(frequency.intValue(), 20, 2000);
            current.mode = 0;
            current.volume = 1;
            playSynth(index + 1, (int) (duration.doubleValue() * 1000), 0);
            index++;
        }
        return ResultWrapper.result(true);
    }

    @Callback(direct = true, doc = "function(handle:userdata, pcm:string):boolean -- append raw PCM bytes to the handle buffer.")
    public synchronized Object[] send(Context context, Arguments args) throws IOException {
        int handle = checkHandle(args, 0);
        byte[] data = args.checkByteArray(1);
        checkOwner(context.node().address(), handle);
        AudioCardSession current = session(handle);
        if (current.isPlayingNow()) return ResultWrapper.result(null, "already playing");
        if (current.closed) return ResultWrapper.result(null, "handle closed");
        if (current.size() + data.length > bufferLimit()) return ResultWrapper.result(null, "buffer full");
        current.append(data);
        return ResultWrapper.result(true);
    }

    @Callback(direct = true, doc = "function(handle:userdata):boolean -- flush buffer to clients and start playback.")
    public synchronized Object[] play(Context context, Arguments args) throws IOException {
        int handle = checkHandle(args, 0);
        checkOwner(context.node().address(), handle);
        AudioCardSession current = session(handle);
        if (current.closed) return ResultWrapper.result(null, "handle closed");
        if (current.size() == 0) return ResultWrapper.result(null, "buffer empty");
        current.startPlayback();
        PacketSender.sendAudioStart(host, handle, current.channel, current.sampleRate,
            current.channels, current.format, current.loop);
        byte[] pcm = current.pcm();
        for (int offset = 0; offset < pcm.length; offset += chunkSize()) {
            PacketSender.sendAudioChunk(host, handle,
                Arrays.copyOfRange(pcm, offset, Math.min(pcm.length, offset + chunkSize())));
        }
        PacketSender.sendAudioPlay(host, handle);
        return ResultWrapper.result(true);
    }

    @Callback(direct = true, doc = "function(handle:userdata):boolean -- pause playback if active.")
    public synchronized Object[] pause(Context context, Arguments args) throws IOException {
        int handle = checkHandle(args, 0);
        checkOwner(context.node().address(), handle);
        AudioCardSession current = session(handle);
        if (current.closed) return ResultWrapper.result(null, "handle closed");
        current.pausePlayback();
        PacketSender.sendAudioPause(host, handle);
        return ResultWrapper.result(true);
    }

    @Callback(direct = true, doc = "function(handle:userdata):boolean -- resume playback if paused.")
    public synchronized Object[] resume(Context context, Arguments args) throws IOException {
        int handle = checkHandle(args, 0);
        checkOwner(context.node().address(), handle);
        AudioCardSession current = session(handle);
        if (current.closed) return ResultWrapper.result(null, "handle closed");
        current.resumePlayback();
        PacketSender.sendAudioResume(host, handle);
        return ResultWrapper.result(true);
    }

    @Callback(direct = true, doc = "function(handle:userdata):boolean -- stop playback.")
    public synchronized Object[] stop(Context context, Arguments args) throws IOException {
        int handle = checkHandle(args, 0);
        checkOwner(context.node().address(), handle);
        AudioCardSession current = session(handle);
        if (current.closed) return ResultWrapper.result(null, "handle closed");
        current.stopPlayback();
        PacketSender.sendAudioStop(host, handle);
        return ResultWrapper.result(true);
    }

    @Callback(direct = true, doc = "function(handle:userdata, loop:boolean):boolean -- set loop mode.")
    public synchronized Object[] setLoop(Context context, Arguments args) throws IOException {
        int handle = checkHandle(args, 0);
        boolean loop = args.checkBoolean(1);
        checkOwner(context.node().address(), handle);
        AudioCardSession current = session(handle);
        if (current.closed) return ResultWrapper.result(null, "handle closed");
        current.loop = loop;
        PacketSender.sendAudioSetLoop(host, handle, loop);
        return ResultWrapper.result(true);
    }

    @Callback(direct = true, doc = "function(handle:userdata):boolean -- close and dispose handle.")
    public synchronized Object[] close(Context context, Arguments args) throws IOException {
        closeHandle(context.node().address(), checkHandle(args, 0));
        return ResultWrapper.result(true);
    }

    @Callback(direct = true, doc = "function(handle:userdata):number -- get current buffer size.")
    public synchronized Object[] size(Context context, Arguments args) throws IOException {
        int handle = checkHandle(args, 0);
        checkOwner(context.node().address(), handle);
        return ResultWrapper.result(session(handle).size());
    }

    @Callback(direct = true, doc = "function(handle:userdata):boolean -- check if audio is currently playing.")
    public synchronized Object[] isPlaying(Context context, Arguments args) throws IOException {
        int handle = checkHandle(args, 0);
        checkOwner(context.node().address(), handle);
        return ResultWrapper.result(session(handle).isPlayingNow());
    }

    public int checkHandle(Arguments args, int index) throws IOException {
        if (args.isInteger(index)) return args.checkInteger(index);
        if (args.isTable(index)) {
            Object handle = args.checkTable(index).get("handle");
            if (handle instanceof Number number) return number.intValue();
            throw new IOException("bad file descriptor");
        }
        Object value = args.checkAny(index);
        if (value instanceof AudioHandleValue handle) return handle.handle;
        throw new IOException("bad file descriptor");
    }

    public void closeHandle(String owner, int handle) throws IOException {
        AudioCardSession current = sessions.get(handle);
        Set<Integer> owned = owners.get(owner);
        if (current == null || owned == null || !owned.remove(handle)) throw new IOException("bad file descriptor");
        PacketSender.sendAudioClose(host, handle);
        current.closed = true;
        sessions.remove(handle);
    }

    private void checkOwner(String owner, int handle) throws IOException {
        if (!owners.containsKey(owner) || !owners.get(owner).contains(handle)) throw new IOException("bad file descriptor");
    }

    @Override
    public synchronized void onMessage(Message message) {
        super.onMessage(message);
        if (message.name().equals("computer.stopped") || message.name().equals("computer.started")) {
            Set<Integer> owned = owners.get(message.source().address());
            if (owned != null) {
                for (int handle : owned) {
                    PacketSender.sendAudioClose(host, handle);
                    AudioCardSession current = sessions.get(handle);
                    if (current != null) current.closed = true;
                    sessions.remove(handle);
                }
                owned.clear();
            }
        }
    }

    @Override
    public synchronized void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == node()) {
            for (int handle : sessions.keySet()) PacketSender.sendAudioClose(host, handle);
            sessions.clear();
            owners.clear();
        }
        else {
            Set<Integer> owned = owners.get(node.address());
            if (owned != null) {
                for (int handle : owned) {
                    PacketSender.sendAudioClose(host, handle);
                    AudioCardSession current = sessions.get(handle);
                    if (current != null) current.closed = true;
                    sessions.remove(handle);
                }
                owners.remove(node.address());
            }
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        scala.collection.immutable.Map<String, scala.collection.immutable.Set<Integer>> saved =
            holder.get(OCComponents.HANDLES.get());
        if (saved != null) {
            Map<String, scala.collection.immutable.Set<Integer>> restored = CollectionConverters.asJava(saved);
            for (Map.Entry<String, scala.collection.immutable.Set<Integer>> entry : restored.entrySet()) {
                Set<Integer> owned = new HashSet<>();
                for (Object handle : CollectionConverters.asJava(entry.getValue())) {
                    owned.add(((Number) handle).intValue());
                }
                owners.put(entry.getKey(), owned);
            }
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        Map<String, scala.collection.immutable.Set<Integer>> saved = new HashMap<>();
        for (Map.Entry<String, Set<Integer>> entry : owners.entrySet()) {
            Set<Integer> handles = new HashSet<>(entry.getValue());
            saved.put(entry.getKey(), Set$.MODULE$.from(CollectionConverters.asScala(handles)));
        }
        holder.set(OCComponents.HANDLES.get(), Map$.MODULE$.from(CollectionConverters.asScala(saved)));
    }
}
