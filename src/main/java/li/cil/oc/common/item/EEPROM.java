package li.cil.oc.common.item;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;

public final class EEPROM extends SimpleItemBase {
    private String unlocalizedName;

    public EEPROM(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public Component getName(ItemStack stack) {
        String label = stack.get(OCComponentsJava.LABEL.get());
        return label != null ? Component.literal(label) : super.getName(stack);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }
}
