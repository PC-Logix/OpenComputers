package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Function1;
import scala.Option;
import scala.runtime.BoxedUnit;

import java.util.function.Function;

/** Player userdata exposed by the debug card. The binary name is kept stable for saved values. */
public class DebugCard$PlayerValue extends li.cil.oc.api.prefab.AbstractValue {
    private static final String NAME_TAG = "name";

    private String name;
    private Option<DebugCardAccessContext> ctx;

    public DebugCard$PlayerValue(String name, Option<DebugCardAccessContext> ctx) {
        this.name = name;
        this.ctx = ctx;
    }

    public DebugCard$PlayerValue() {
        this("", Option.empty());
    }

    public String name() {
        return name;
    }

    public void name_$eq(String name) {
        this.name = name;
    }

    public Option<DebugCardAccessContext> ctx() {
        return ctx;
    }

    public void ctx_$eq(Option<DebugCardAccessContext> ctx) {
        this.ctx = ctx;
    }

    public Object[] withPlayer(Function1<ServerPlayer, Object[]> callback) {
        return withPlayerJava(callback::apply);
    }

    private Object[] withPlayerJava(Function<ServerPlayer, Object[]> callback) {
        checkAccess();
        Player player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(name);
        if (player instanceof ServerPlayer serverPlayer) return callback.apply(serverPlayer);
        return ResultWrapper.result(BoxedUnit.UNIT, "player is offline");
    }

    private void checkAccess() {
        DebugCardSupport.checkAccess(ctx);
    }

    @Callback(doc = "function():userdata -- Get the player's world object.")
    public Object[] getWorld(Context context, Arguments args) {
        return withPlayerJava(player -> ResultWrapper.result(DebugCardSupport.worldValue(player.level(), ctx)));
    }

    @Callback(doc = "function():string -- Get the player's game type.")
    public Object[] getGameType(Context context, Arguments args) {
        return withPlayerJava(player -> ResultWrapper.result(player.gameMode.getGameModeForPlayer().getName()));
    }

    @Callback(doc = "function(gametype:string) -- Set the player's game type (survival, creative, adventure).")
    public Object[] setGameType(Context context, Arguments args) {
        return withPlayerJava(player -> {
            player.gameMode.changeGameModeForPlayer(GameType.byName(args.checkString(0), GameType.SURVIVAL));
            return null;
        });
    }

    @Callback(doc = "function():number, number, number -- Get the player's position.")
    public Object[] getPosition(Context context, Arguments args) {
        return withPlayerJava(player -> ResultWrapper.result(player.getX(), player.getY(), player.getZ()));
    }

    @Callback(doc = "function(x:number, y:number, z:number) -- Set the player's position.")
    public Object[] setPosition(Context context, Arguments args) {
        return withPlayerJava(player -> {
            player.teleportTo(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2));
            return null;
        });
    }

    @Callback(doc = "function():number -- Get the player's health.")
    public Object[] getHealth(Context context, Arguments args) {
        return withPlayerJava(player -> ResultWrapper.result(player.getHealth()));
    }

    @Callback(doc = "function():number -- Get the player's max health.")
    public Object[] getMaxHealth(Context context, Arguments args) {
        return withPlayerJava(player -> ResultWrapper.result(player.getMaxHealth()));
    }

    @Callback(doc = "function(health:number) -- Set the player's health.")
    public Object[] setHealth(Context context, Arguments args) {
        return withPlayerJava(player -> {
            player.setHealth((float) args.checkDouble(0));
            return null;
        });
    }

    @Callback(doc = "function():number -- Get the player's level")
    public Object[] getLevel(Context context, Arguments args) {
        return withPlayerJava(player -> ResultWrapper.result(player.experienceLevel));
    }

    @Callback(doc = "function():number -- Get the player's total experience")
    public Object[] getExperienceTotal(Context context, Arguments args) {
        return withPlayerJava(player -> ResultWrapper.result(player.totalExperience));
    }

    @Callback(doc = "function(level:number) -- Add a level to the player's experience level")
    public Object[] addExperienceLevel(Context context, Arguments args) {
        return withPlayerJava(player -> {
            player.giveExperienceLevels(args.checkInteger(0));
            return null;
        });
    }

    @Callback(doc = "function(level:number) -- Remove a level from the player's experience level")
    public Object[] removeExperienceLevel(Context context, Arguments args) {
        return withPlayerJava(player -> {
            player.giveExperienceLevels(-args.checkInteger(0));
            return null;
        });
    }

    @Callback(doc = "function() -- Clear the players inventory")
    public Object[] clearInventory(Context context, Arguments args) {
        return withPlayerJava(player -> {
            player.getInventory().clearContent();
            return null;
        });
    }

    @Deprecated
    @Callback(doc = "function(id:string, amount:number, meta:number[, nbt:string]):number -- Adds the item stack to the players inventory")
    public Object[] insertItem(Context context, Arguments args) {
        return withPlayerJava(player -> {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(args.checkString(0)));
            if (item == null) throw new IllegalArgumentException("invalid item id");
            int amount = args.checkInteger(1);
            args.checkInteger(2); // meta
            String tagJson = args.checkString(3);
            CompoundTag tag = com.google.common.base.Strings.isNullOrEmpty(tagJson) ? new CompoundTag() : parseTag(tagJson);
            ItemStack stack = new ItemStack(item, amount);
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
            InventoryUtils.addToPlayerInventory(stack, player);
            return ResultWrapper.result(BoxedUnit.UNIT);
        });
    }

    @Override
    public void loadData(DataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadData(holder, nbt, provider);
        ctx = Option.apply(holder.get(OCComponentsJava.DEBUG_CARD_ACCESS_CONTEXT.get()));
        name = nbt.getString(NAME_TAG);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveData(holder, nbt, provider);
        nbt.putString(NAME_TAG, name);
    }

    private static CompoundTag parseTag(String text) {
        try {
            return TagParser.parseTag(text);
        } catch (CommandSyntaxException error) {
            throw sneaky(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }

}
