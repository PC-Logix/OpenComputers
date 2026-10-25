package li.cil.oc.common.item;

import li.cil.oc.OpenComputers;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class Present extends SimpleItemBase {
    public static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(
        Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "presents"));
    private String unlocalizedName;

    public Present(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {
            stack.shrink(1);
            if (!level.isClientSide()) {
                level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.MASTER, 0.2f, 1.0f);
                ServerLevel serverLevel = (ServerLevel) level;
                var lootTable = serverLevel.getServer().reloadableRegistries()
                    .getLootTable(LOOT_TABLE);
                var parameters = new LootParams.Builder(serverLevel).withLuck(player.getLuck())
                    .create(LootContextParamSets.EMPTY);
                lootTable.getRandomItems(parameters, item -> InventoryUtils.addToPlayerInventory(item, player, true));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
