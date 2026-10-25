package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Option;

/** Scoreboard userdata exposed by the debug card. The binary name is stable for saved values. */
public class DebugCard$ScoreboardValue extends li.cil.oc.api.prefab.AbstractValue {
    private static final String DIMENSION_TAG = "dimension";

    private Option<DebugCardAccessContext> ctx;
    private Scoreboard scoreboard;
    private ResourceLocation dimension;

    public DebugCard$ScoreboardValue(Option<Level> world, Option<DebugCardAccessContext> ctx) {
        this.ctx = ctx;
        this.scoreboard = world.fold(() -> null, Level::getScoreboard);
        this.dimension = world.fold(() -> Level.OVERWORLD, level -> level.dimension()).location();
    }

    public DebugCard$ScoreboardValue() {
        this(Option.empty(), Option.empty());
    }

    public Option<DebugCardAccessContext> ctx() { return ctx; }
    public void ctx_$eq(Option<DebugCardAccessContext> value) { ctx = value; }
    public Scoreboard scoreboard() { return scoreboard; }
    public void scoreboard_$eq(Scoreboard value) { scoreboard = value; }
    public ResourceLocation dimension() { return dimension; }
    public void dimension_$eq(ResourceLocation value) { dimension = value; }

    private void checkAccess() {
        DebugCardSupport.checkAccess(ctx);
    }

    @Callback(doc = "function(team:string) - Add a team to the scoreboard")
    public Object[] addTeam(Context context, Arguments args) {
        checkAccess();
        scoreboard.addPlayerTeam(args.checkString(0));
        return null;
    }

    @Callback(doc = "function(teamName: string) - Remove a team from the scoreboard")
    public Object[] removeTeam(Context context, Arguments args) {
        checkAccess();
        scoreboard.removePlayerTeam(scoreboard.getPlayersTeam(args.checkString(0)));
        return null;
    }

    @Callback(doc = "function(player:string, team:string):boolean - Add a player to a team")
    public Object[] addPlayerToTeam(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(scoreboard.addPlayerToTeam(args.checkString(0), scoreboard.getPlayersTeam(args.checkString(1))));
    }

    @Callback(doc = "function(player:string):boolean - Remove a player from their team")
    public Object[] removePlayerFromTeams(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(scoreboard.removePlayerFromTeam(args.checkString(0)));
    }

    @Callback(doc = "function(player:string, team:string):boolean - Remove a player from a specific team")
    public Object[] removePlayerFromTeam(Context context, Arguments args) {
        checkAccess();
        String player = args.checkString(0);
        scoreboard.removePlayerFromTeam(player, scoreboard.getPlayersTeam(args.checkString(1)));
        return null;
    }

    @Callback(doc = "function(objectiveName:string, objectiveCriteria:string) - Create a new objective for the scoreboard")
    public Object[] addObjective(Context context, Arguments args) {
        checkAccess();
        String objectiveName = args.checkString(0);
        ObjectiveCriteria criteria = ObjectiveCriteria.byName(args.checkString(1))
                .orElseThrow(() -> new IllegalArgumentException("invalid criterion"));
        scoreboard.addObjective(objectiveName, criteria, Component.literal(objectiveName),
                ObjectiveCriteria.RenderType.INTEGER, false, new StyledFormat(Style.EMPTY));
        return null;
    }

    @Callback(doc = "function(objectiveName:string) - Remove an objective from the scoreboard")
    public Object[] removeObjective(Context context, Arguments args) {
        checkAccess();
        scoreboard.removeObjective(scoreboard.getObjective(args.checkString(0)));
        return null;
    }

    @Callback(doc = "function(playerName:string, objectiveName:string, score:int) - Sets the score of a player for a certain objective")
    public Object[] setPlayerScore(Context context, Arguments args) {
        checkAccess();
        String name = args.checkString(0);
        var score = scoreboard.getOrCreatePlayerScore(() -> name, scoreboard.getObjective(args.checkString(1)));
        score.set(args.checkInteger(2));
        return null;
    }

    @Callback(doc = "function(playerName:string, objectiveName:string):int - Gets the score of a player for a certain objective")
    public Object[] getPlayerScore(Context context, Arguments args) {
        checkAccess();
        String name = args.checkString(0);
        var score = scoreboard.getOrCreatePlayerScore(() -> name, scoreboard.getObjective(args.checkString(1)));
        return ResultWrapper.result(score.get());
    }

    @Callback(doc = "function(playerName:string, objectiveName:string, score:int) - Increases the score of a player for a certain objective")
    public Object[] increasePlayerScore(Context context, Arguments args) {
        checkAccess();
        String name = args.checkString(0);
        var score = scoreboard.getOrCreatePlayerScore(() -> name, scoreboard.getObjective(args.checkString(1)));
        score.add(args.checkInteger(2));
        return null;
    }

    @Callback(doc = "function(playerName:string, objectiveName:string, score:int) - Decrease the score of a player for a certain objective")
    public Object[] decreasePlayerScore(Context context, Arguments args) {
        checkAccess();
        String name = args.checkString(0);
        var score = scoreboard.getOrCreatePlayerScore(() -> name, scoreboard.getObjective(args.checkString(1)));
        score.add(-args.checkInteger(2));
        return null;
    }

    @Override
    public void loadData(DataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadData(holder, nbt, provider);
        ctx = Option.apply(holder.get(OCComponentsJava.DEBUG_CARD_ACCESS_CONTEXT.get()));
        dimension = ResourceLocation.tryParse(nbt.getString(DIMENSION_TAG));
        ResourceKey<Level> dimensionKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        scoreboard = server.getLevel(dimensionKey).getScoreboard();
    }

    @Override
    public void saveData(MutableDataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveData(holder, nbt, provider);
        nbt.putString(DIMENSION_TAG, dimension.toString());
    }
}
