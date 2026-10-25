package li.cil.oc.common.block;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class ChameliumBlock extends SimpleBlock {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final DyeColor DEFAULT_COLOR = DyeColor.BLACK;
    public static final MapCodec<ChameliumBlock> CODEC = BlockBehaviour.simpleCodec(ChameliumBlock::new);

    public ChameliumBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(COLOR, DEFAULT_COLOR));
    }

    @Override
    public MapCodec<ChameliumBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Deprecated
    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        final ItemStack stack = new ItemStack(this);
        stack.set(OCComponentsJava.CHAMELIUM_COLOR.get(), state.getValue(COLOR));
        return stack;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(COLOR,
            context.getItemInHand().getOrDefault(OCComponentsJava.CHAMELIUM_COLOR.get(), DEFAULT_COLOR));
    }
}
