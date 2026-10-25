package li.cil.oc.common.item;

import li.cil.oc.Settings;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;

public final class TexturePicker extends SimpleItemBase {
    private String unlocalizedName;

    public TexturePicker(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (level.isClientSide()) {
            var pos = context.getClickedPos();
            var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(level.getBlockState(pos));
            var blockEntity = level.getBlockEntity(pos);
            var particle = model.getParticleIcon(blockEntity == null ? ModelData.EMPTY : blockEntity.getModelData());
            if (context.getPlayer() != null) {
                var message = Component.literal("§aOpenComputers§f: ")
                    .append(Component.translatable(Settings.namespace() + "gui.Chat.TextureName", particle.contents().name()));
                context.getPlayer().sendSystemMessage(message);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
