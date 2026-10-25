package li.cil.oc.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.server.network.DebugNetwork.DebugNode;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;
import scala.Tuple3;

/** Managed environment and callbacks exposed by the debug card. */
public final class DebugCard extends AbstractManagedEnvironment implements DebugNode {
    private final EnvironmentHost host;
    private final ComponentConnector connector;
    private Option<Node> remoteNode = Option$.MODULE$.empty();
    private Option<Tuple3<Integer, Integer, Integer>> remoteNodePosition = Option$.MODULE$.empty();
    private Option<DebugCardAccessContext> access = Option$.MODULE$.empty();

    public DebugCard(EnvironmentHost host) {
        this.host = host;
        this.connector = Network.newNode(this, Visibility.Neighbors)
            .withComponent("debug")
            .withConnector()
            .create();
    }

    @Override
    public ComponentConnector node() {
        return connector;
    }

    public Option<String> player() {
        return access.isDefined() ? Option$.MODULE$.apply(access.get().player()) : Option$.MODULE$.empty();
    }

    EnvironmentHost environmentHostForJava() { return host; }
    Option<DebugCardAccessContext> accessForJava() { return access; }
    void setAccessForJava(Option<DebugCardAccessContext> value) { access = value; }
    Option<Node> remoteNodeForJava() { return remoteNode; }
    void setRemoteNodeForJava(Option<Node> value) { remoteNode = value; }
    Option<Tuple3<Integer, Integer, Integer>> remoteNodePositionForJava() { return remoteNodePosition; }
    void setRemoteNodePositionForJava(int x, int y, int z) { remoteNodePosition = Option$.MODULE$.apply(new Tuple3<>(x, y, z)); }
    void clearRemoteNodePositionForJava() { remoteNodePosition = Option$.MODULE$.empty(); }

    @Callback(doc = "function(value:number):number -- Changes the component network's energy buffer by the specified delta.")
    public Object[] changeBuffer(Context context, Arguments args) {
        DebugCardSupport.checkAccess(access);
        return ResultWrapper.result(node().changeBuffer(args.checkDouble(0)));
    }

    @Callback(doc = "function():number -- Get the container's X position in the world.")
    public Object[] getX(Context context, Arguments args) {
        DebugCardSupport.checkAccess(access);
        return ResultWrapper.result(host.xPosition());
    }

    @Callback(doc = "function():number -- Get the container's Y position in the world.")
    public Object[] getY(Context context, Arguments args) {
        DebugCardSupport.checkAccess(access);
        return ResultWrapper.result(host.yPosition());
    }

    @Callback(doc = "function():number -- Get the container's Z position in the world.")
    public Object[] getZ(Context context, Arguments args) {
        DebugCardSupport.checkAccess(access);
        return ResultWrapper.result(host.zPosition());
    }

    @Deprecated
    @Callback(doc = "function([id:number]):userdata -- Get the world object for the specified dimension ID, or the container's.")
    public Object[] getWorld(Context context, Arguments args) {
        return DebugCardQueryCallbacks.getWorld(host, access, args);
    }

    @Deprecated
    @Callback(doc = "function():table -- Get a list of all world IDs, loaded and unloaded.")
    public Object[] getWorlds(Context context, Arguments args) {
        return DebugCardQueryCallbacks.getWorlds(access);
    }

    @Callback(doc = "function(name:string):userdata -- Get the entity of a player.")
    public Object[] getPlayer(Context context, Arguments args) {
        return DebugCardQueryCallbacks.getPlayer(access, args);
    }

    @Callback(doc = "function():table -- Get a list of currently logged-in players.")
    public Object[] getPlayers(Context context, Arguments args) {
        return DebugCardQueryCallbacks.getPlayers(access);
    }

    @Callback(doc = "function():userdata -- Get the scoreboard object for the world")
    public Object[] getScoreboard(Context context, Arguments args) {
        return DebugCardQueryCallbacks.getScoreboard(host, access);
    }

    @Deprecated
    @Callback(doc = "function(x: number, y: number, z: number[, worldId: number]):boolean, string, table -- returns contents at the location in world by id (default host world)")
    public Object[] scanContentsAt(Context context, Arguments args) {
        return DebugCardQueryCallbacks.scanContentsAt(host, access, args);
    }

    @Callback(doc = "function(name:string):boolean -- Get whether a mod or API is loaded.")
    public Object[] isModLoaded(Context context, Arguments args) {
        return DebugCardQueryCallbacks.isModLoaded(access, args);
    }

    @Callback(doc = "function(command:string):number, string -- Runs a command as the bound player or configured fake player.")
    public Object[] runCommand(Context context, Arguments args) {
        return DebugCardQueryCallbacks.runCommand(host, player(), access, args);
    }

    @Callback(doc = "function(x:number, y:number, z:number):boolean -- Add a component block at the specified coordinates to the computer network.")
    public Object[] connectToBlock(Context context, Arguments args) {
        return DebugCardNetworkCallbacks.connectToBlock(this, host, access, args);
    }

    @Callback(doc = "function():userdata -- Test method for user-data and general value conversion.")
    public Object[] test(Context context, Arguments args) {
        return DebugCardSupport.test(host, access);
    }

    @Callback(doc = "function(player:string, text:string) -- Sends text to the specified player's clipboard if possible.")
    public Object[] sendToClipboard(Context context, Arguments args) {
        return DebugCardCommunicationCallbacks.sendToClipboard(access, args);
    }

    @Callback(doc = "function(address:string, data...) -- Sends data to the debug card with the specified address.")
    public Object[] sendToDebugCard(Context context, Arguments args) {
        return DebugCardCommunicationCallbacks.sendToDebugCard(this, access, args);
    }

    @Override
    public void receivePacket(Packet packet) {
        DebugCardNetworkCallbacks.receivePacket(this, packet);
    }

    @Override
    public String address() {
        return DebugCardNetworkCallbacks.address(this);
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        DebugCardNetworkCallbacks.onConnect(this, node);
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        DebugCardNetworkCallbacks.onDisconnect(this, node);
    }

    @Override
    public void loadData(DataComponentHolder holder) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(holder);
        DebugCardDataCallbacks.load(this, holder);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        DebugCardDataCallbacks.save(this, holder);
    }
}
