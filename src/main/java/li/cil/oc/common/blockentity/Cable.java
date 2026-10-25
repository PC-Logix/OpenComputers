package li.cil.oc.common.blockentity;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.util.Color;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class Cable extends ColoredEnvironmentBlockEntity {
    private final li.cil.oc.common.blockentity.traits.ColoredJava.State colorState =
        new li.cil.oc.common.blockentity.traits.ColoredJava.State();
    @Override public li.cil.oc.common.blockentity.traits.ColoredJava.State colorState() { return colorState; }
    private static final int DEFAULT_COLOR = Color.rgbValues(DyeColor.LIGHT_GRAY);
    private final Node node = Network.newNode(this, Visibility.None).create();

    public Cable(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.CABLE.get(), pos, state);
        setColor(DEFAULT_COLOR);
    }

    @Override
    public Node node() {
        return node;
    }

    public ItemStack createItemStack() {
        ItemStack stack = Items.get(Constants.BlockName$.MODULE$.Cable()).createItemStack(1);
        if (getColor() != DEFAULT_COLOR) {
            ItemColorizer.setColor(stack, getColor());
        }
        return stack;
    }

    public void fromItemStack(ItemStack stack) {
        if (ItemColorizer.hasColor(stack)) {
            setColor(ItemColorizer.getColor(stack));
        }
    }

    @Override
    public boolean controlsConnectivity() {
        return true;
    }

    @Override
    public boolean consumesDye() {
        return true;
    }

    @Override
    public void onColorChanged() {
        super.onColorChanged();
        if (getLevel() != null && isServer()) {
            Network.joinOrCreateNetwork(this);
        }
    }
}
