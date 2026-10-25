package li.cil.oc.common.blockentity;

import li.cil.oc.api.machine.*;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.blockentity.traits.BlockInventory;
import li.cil.oc.common.blockentity.traits.Tickable;
import li.cil.oc.common.item.Tape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Server-owned cassette state and encoded DFPWM playback. */
public final class TapeDrive extends EnvironmentBlockEntity implements BlockInventory, Tickable, MenuProvider {
    private static final String STOPPED = "stopped", PLAYING = "playing", REWINDING = "rewinding", FORWARDING = "forwarding";
    private static final int SAMPLE_RATE = 48000;
    private static final double BYTES_PER_TICK = SAMPLE_RATE / 8.0 / 20.0;
    private final Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network).withComponent("tape_drive").create();
    private int cursor;
    private String state = STOPPED;
    private double speed = 1, volume = 1;
    private List<AudioRouter.Session> activeAudio = List.of();
    public TapeDrive(BlockPos pos, BlockState state) { super(BlockEntityTypes.TAPE_DRIVE.get(), pos, state); }
    @Override public Node node() { return node; }
    @Override public int getContainerSize() { return 1; }
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new li.cil.oc.common.menu.TapeDrive(id, inventory, this); }
    public int guiState() { return switch (state) { case PLAYING -> 1; case REWINDING -> 2; case FORWARDING -> 3; default -> 0; }; }
    private Optional<ItemStack> tape() { ItemStack stack = getItem(0); return !stack.isEmpty() && stack.getItem() instanceof Tape ? Optional.of(stack) : Optional.empty(); }
    private static int capacity(ItemStack stack) { return ((Tape) stack.getItem()).capacity(); }
    private static byte[] data(ItemStack stack) { return TapeData.bytes(stack); }
    private static void persist(ItemStack stack, byte[] bytes) { TapeData.save(stack, bytes, TapeData.tag(stack).getString(TapeData.Label)); }
    public void interact(Player player, ItemStack held) {
        if (player.isCrouching()) {
            if (tape().isPresent()) { stopTape(); dropSlot(0, 1, Optional.empty()); }
            if (getItem(0).isEmpty() && held.getItem() instanceof Tape) setItem(0, held.split(1));
        } else playTape();
    }
    public void onRedstone(boolean powered) {
        if (powered && tape().isPresent() && !state.equals(PLAYING)) playTape(); else if (!powered) stopTape();
        setChanged();
    }
    @Callback(direct=true) public Object[] isReady(Context context, Arguments args) { return result(tape().isPresent()); }
    @Callback(direct=true) public Object[] getSize(Context context, Arguments args) { return result(tape().map(TapeDrive::capacity).orElse(0)); }
    @Callback(direct=true) public Object[] getPosition(Context context, Arguments args) { return result(cursor); }
    @Callback public Object[] seek(Context context, Arguments args) {
        var tape = tape(); if (tape.isEmpty()) return result(null, "no tape inserted");
        int old = cursor; cursor = Math.min(capacity(tape.get()), Math.max(0, cursor + args.checkInteger(0))); setChanged(); return result(cursor - old);
    }
    @Callback public Object[] read(Context context, Arguments args) {
        var tape = tape(); if (tape.isEmpty()) return result(null, "no tape inserted");
        byte[] bytes = data(tape.get());
        if (args.count() == 0) { int value = cursor < bytes.length ? bytes[cursor] & 255 : 0; cursor = Math.min(capacity(tape.get()), cursor + 1); return result(value); }
        int count = Math.min(256, Math.max(0, args.checkInteger(0))), end = Math.min(capacity(tape.get()), cursor + count);
        byte[] output = new byte[end - cursor];
        for (int i = 0; i < output.length; i++) output[i] = cursor + i < bytes.length ? bytes[cursor + i] : 0;
        cursor = end; return result(output);
    }
    @Callback public Object[] write(Context context, Arguments args) {
        var tape = tape(); if (tape.isEmpty()) return result(null, "no tape inserted");
        ItemStack stack = tape.get(); byte[] input = args.isInteger(0) ? new byte[]{(byte) args.checkInteger(0)} : args.checkByteArray(0);
        byte[] bytes = data(stack); int end = cursor + input.length;
        if (end > capacity(stack)) return result(null, "tape is full");
        byte[] next = Arrays.copyOf(bytes, Math.max(bytes.length, end)); System.arraycopy(input, 0, next, cursor, input.length); cursor = end;
        persist(stack, next); setChanged(); return result(input.length);
    }
    @Callback(direct=true) public Object[] getLabel(Context context, Arguments args) { return result(tape().map(stack -> TapeData.tag(stack).getString(TapeData.Label)).orElse(null)); }
    @Callback public Object[] setLabel(Context context, Arguments args) {
        var tape = tape(); if (tape.isEmpty()) return result(null, "no tape inserted");
        String label = args.checkString(0); TapeData.save(tape.get(), data(tape.get()), label.substring(0, Math.min(64, label.length()))); setChanged(); return result(true);
    }
    @Callback public Object[] play(Context context, Arguments args) { return result(playTape()); }
    @Callback public Object[] stop(Context context, Arguments args) { stopTape(); return result(true); }
    @Callback public Object[] setSpeed(Context context, Arguments args) { speed = Math.min(2, Math.max(0.25, args.checkDouble(0))); return result(speed); }
    @Callback public Object[] setVolume(Context context, Arguments args) { volume = Math.min(1, Math.max(0, args.checkDouble(0))); return result(volume); }
    @Callback(direct=true) public Object[] getState(Context context, Arguments args) { return result(state); }
    @Callback(direct=true) public Object[] isEnd(Context context, Arguments args) { return result(tape().map(stack -> cursor + 1024 >= capacity(stack)).orElse(true)); }
    @Override public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        cursor = nbt.getInt("tapeCursor"); String saved = nbt.getString("tapeState"); state = Set.of(STOPPED, PLAYING, REWINDING, FORWARDING).contains(saved) ? saved : STOPPED;
        double savedSpeed = nbt.getDouble("tapeSpeed"), savedVolume = nbt.getDouble("tapeVolume");
        speed = savedSpeed >= 0.25 && savedSpeed <= 2 ? savedSpeed : 1; volume = savedVolume >= 0 && savedVolume <= 1 ? savedVolume : 1;
        tape().ifPresent(stack -> { var tag = TapeData.tag(stack); if (!tag.contains(TapeData.Storage) && tag.contains(TapeData.Bytes)) TapeData.save(stack, tag.getByteArray(TapeData.Bytes), tag.getString(TapeData.Label)); });
        tape().ifPresent(stack -> cursor = Math.min(capacity(stack), Math.max(0, cursor)));
    }
    @Override public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider); nbt.putInt("tapeCursor", cursor); nbt.putString("tapeState", state); nbt.putDouble("tapeSpeed", speed); nbt.putDouble("tapeVolume", volume);
    }
    @Override public void updateEntity() {
        super.updateEntity();
        if (getLevel() == null || getLevel().isClientSide() || state.equals(STOPPED)) return;
        var tape = tape();
        if (tape.isEmpty()) { state = STOPPED; return; }
        int old = cursor, step = Math.max(1, (int) (BYTES_PER_TICK * speed)); ItemStack stack = tape.get();
        if (state.equals(PLAYING) || state.equals(FORWARDING)) cursor = Math.min(data(stack).length, cursor + step);
        else if (state.equals(REWINDING)) cursor = Math.max(0, cursor - step);
        if (((state.equals(PLAYING) || state.equals(FORWARDING)) && cursor >= data(stack).length) || (state.equals(REWINDING) && cursor <= 0)) { state = STOPPED; activeAudio = List.of(); }
        if (cursor != old) setChanged();
    }
    private boolean playTape() {
        var tape = tape(); if (tape.isEmpty()) return false;
        byte[] bytes = data(tape.get()); if (cursor >= bytes.length) return false;
        byte[] encoded = Arrays.copyOfRange(bytes, cursor, bytes.length);
        OpenComputersLog.playback(cursor, bytes, encoded, (int) (SAMPLE_RATE * speed));
        activeAudio = AudioRouter.playDfpwm(this, encoded, (int) (SAMPLE_RATE * speed), (float) volume);
        if (!activeAudio.isEmpty()) { state = PLAYING; setChanged(); } return !activeAudio.isEmpty();
    }
    private void stopTape() { AudioRouter.stop(activeAudio); activeAudio = List.of(); state = STOPPED; setChanged(); }
    public void handleControl(int action, Player player) {
        if (getLevel() == null || getLevel().isClientSide() || !stillValid(player)) return;
        switch (action) {
            case 0 -> { stopTape(); state = REWINDING; setChanged(); }
            case 1 -> { if (state.equals(PLAYING)) stopTape(); else playTape(); }
            case 2 -> stopTape();
            case 3 -> { stopTape(); state = FORWARDING; setChanged(); }
            default -> { }
        }
    }
    private static final class OpenComputersLog {
        static void playback(int cursor, byte[] bytes, byte[] encoded, int rate) {
            li.cil.oc.OpenComputers.log.info("Tape playback source: cursor={}, tapeLength={}, tapeSha256={}, encodedLength={}, encodedSha256={}, sampleRate={}", cursor, bytes.length, TapeData.sha256(bytes), encoded.length, TapeData.sha256(encoded), rate);
        }
    }
}
