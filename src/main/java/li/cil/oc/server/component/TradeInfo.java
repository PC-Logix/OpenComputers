package li.cil.oc.server.component;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.blockentity.RobotProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

public class TradeInfo {
    private static final String HOST_IS_ENTITY_TAG = "hostIsEntity";
    private static final String MERCHANT_UUID_MOST_TAG = "merchantUUIDMost";
    private static final String MERCHANT_UUID_LEAST_TAG = "merchantUUIDLeast";
    private static final String DIMENSION_ID_TAG = "dimensionID";
    private static final String HOST_UUID_MOST = "hostUUIDMost";
    private static final String HOST_UUID_LEAST = "hostUUIDLeast";
    private static final String HOST_X_TAG = "hostX";
    private static final String HOST_Y_TAG = "hostY";
    private static final String HOST_Z_TAG = "hostZ";
    private static final String RECIPE_ID = "recipeID";
    private static final String MERCHANT_ID = "merchantID";

    private EnvironmentHost host;
    private java.lang.ref.WeakReference<Merchant> merchant;
    private int recipeID;
    private int merchantID;

    public TradeInfo() {
        this(null, null, -1, -1);
    }

    public TradeInfo(EnvironmentHost host, Merchant merchant, int recipeID, int merchantID) {
        this.host = host;
        this.merchant = new java.lang.ref.WeakReference<>(merchant);
        this.recipeID = recipeID;
        this.merchantID = merchantID;
    }

    public EnvironmentHost host() { return host; }
    public Merchant merchant() { return merchant.get(); }
    public int recipeID() { return recipeID; }
    public int merchantID() { return merchantID; }

    public MerchantOffer recipe() {
        Merchant current = merchant.get();
        return current == null ? null : current.getOffers().get(recipeID);
    }

    public Container inventory() {
        return host instanceof Agent agent ? agent.mainInventory() : null;
    }

    public void loadData(CompoundTag nbt) {
        boolean isEntity = nbt.getBoolean(HOST_IS_ENTITY_TAG);
        // Drones are resolved by UUID; robots are resolved from their saved block position.
        host = isEntity ? loadHostEntity(nbt) : loadHostTileEntity(nbt);
        Entity loadedMerchant = loadEntity(nbt,
                new UUID(nbt.getLong(MERCHANT_UUID_MOST_TAG), nbt.getLong(MERCHANT_UUID_LEAST_TAG)));
        merchant = new java.lang.ref.WeakReference<>(loadedMerchant instanceof Merchant found ? found : null);
        recipeID = nbt.getInt(RECIPE_ID);
        merchantID = nbt.contains(MERCHANT_ID) ? nbt.getInt(MERCHANT_ID) : -1;
    }

    public void saveData(CompoundTag nbt) {
        if (host instanceof Entity entity) {
            nbt.putBoolean(HOST_IS_ENTITY_TAG, true);
            nbt.putString(DIMENSION_ID_TAG, entity.level().dimension().location().toString());
            nbt.putLong(HOST_UUID_LEAST, entity.getUUID().getLeastSignificantBits());
            nbt.putLong(HOST_UUID_MOST, entity.getUUID().getMostSignificantBits());
        }
        else if (host instanceof BlockEntity tileEntity) {
            nbt.putBoolean(HOST_IS_ENTITY_TAG, false);
            nbt.putString(DIMENSION_ID_TAG, tileEntity.getLevel().dimension().location().toString());
            nbt.putInt(HOST_X_TAG, tileEntity.getBlockPos().getX());
            nbt.putInt(HOST_Y_TAG, tileEntity.getBlockPos().getY());
            nbt.putInt(HOST_Z_TAG, tileEntity.getBlockPos().getZ());
        }

        Merchant currentMerchant = merchant.get();
        if (currentMerchant instanceof Entity entity) {
            nbt.putLong(MERCHANT_UUID_LEAST_TAG, entity.getUUID().getLeastSignificantBits());
            nbt.putLong(MERCHANT_UUID_MOST_TAG, entity.getUUID().getMostSignificantBits());
        }
        nbt.putInt(RECIPE_ID, recipeID);
        nbt.putInt(MERCHANT_ID, merchantID);
    }

    private Entity loadEntity(CompoundTag nbt, UUID uuid) {
        ResourceLocation dimension = ResourceLocation.tryParse(nbt.getString(DIMENSION_ID_TAG));
        ResourceKey<net.minecraft.world.level.Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimension);
        ServerLevel world = ServerLifecycleHooks.getCurrentServer().getLevel(dimensionKey);
        return world.getEntity(uuid);
    }

    private EnvironmentHost loadHostEntity(CompoundTag nbt) {
        Entity entity = loadEntity(nbt, new UUID(nbt.getLong(HOST_UUID_MOST), nbt.getLong(HOST_UUID_LEAST)));
        return entity instanceof Agent && entity instanceof EnvironmentHost environmentHost ? environmentHost : null;
    }

    private EnvironmentHost loadHostTileEntity(CompoundTag nbt) {
        ResourceLocation dimension = ResourceLocation.tryParse(nbt.getString(DIMENSION_ID_TAG));
        ResourceKey<net.minecraft.world.level.Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimension);
        ServerLevel world = ServerLifecycleHooks.getCurrentServer().getLevel(dimensionKey);
        BlockEntity blockEntity = world.getBlockEntity(new BlockPos(
                nbt.getInt(HOST_X_TAG), nbt.getInt(HOST_Y_TAG), nbt.getInt(HOST_Z_TAG)));
        if (blockEntity instanceof RobotProxy robotProxy) return robotProxy.robot();
        if (blockEntity instanceof Agent && blockEntity instanceof EnvironmentHost environmentHost) return environmentHost;
        return null;
    }
}
