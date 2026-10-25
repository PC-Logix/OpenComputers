package li.cil.oc.client.renderer.block;

import li.cil.oc.client.Textures;
import li.cil.oc.common.Tier;
import li.cil.oc.common.blockentity.Screen;
import li.cil.oc.util.Color;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.List;

public final class ScreenModel extends SmartBlockModelBase {
    public static final ScreenModel INSTANCE = new ScreenModel();
    public static final ModelProperty<Screen> SCREEN_PROPERTY = new ModelProperty<>();
    private static final ItemOverrides ITEM_OVERRIDE = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            return new ItemModel(stack);
        }
    };

    private ScreenModel() {}

    @Override
    public ItemOverrides getOverrides() {
        return ITEM_OVERRIDE;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, net.minecraft.client.renderer.RenderType renderType) {
        Direction safeSide = side != null ? side : Direction.SOUTH;
        Screen screen = data.get(SCREEN_PROPERTY);
        if (screen == null) return super.getQuads(state, safeSide, random);

        Direction facing = screen.toLocal(safeSide);
        var localPosition = screen.localPositionJava();
        int x = localPosition.x();
        int y = localPosition.y();
        int px = xy2part(x, screen.width() - 1);
        int py = xy2part(y, screen.height() - 1);
        if ((safeSide == Direction.DOWN || screen.facing() == Direction.DOWN) && safeSide != screen.facing()) {
            px = 2 - px;
            py = 2 - py;
        }
        int rotation = safeSide == Direction.UP ? screen.yaw().get2DDataValue()
            : safeSide == Direction.DOWN ? -screen.yaw().get2DDataValue() : 0;
        int pitch = screen.pitch() == Direction.NORTH ? 0 : 1;

        var textures = Textures.Block.Screen.INSTANCE;
        net.minecraft.resources.ResourceLocation texture;
        if (screen.width() == 1 && screen.height() == 1) {
            texture = facing == Direction.SOUTH ? textures.SingleFront[pitch] : textures.Single[safeSide.get3DDataValue()];
        } else if (screen.width() == 1) {
            texture = facing == Direction.SOUTH ? textures.VerticalFront[pitch][py]
                : textures.Vertical[pitch][py][facing.get3DDataValue()];
        } else if (screen.height() == 1) {
            texture = facing == Direction.SOUTH ? textures.HorizontalFront[pitch][px]
                : textures.Horizontal[pitch][px][facing.get3DDataValue()];
        } else {
            texture = facing == Direction.SOUTH ? textures.MultiFront[pitch][py][px]
                : textures.Multi[pitch][py][px][facing.get3DDataValue()];
        }
        return List.of(bakeQuad(safeSide, Textures.getSprite(texture), screen.getColor(), rotation));
    }

    private static int xy2part(int value, int high) {
        return value == 0 ? 2 : value == high ? 0 : 1;
    }

    public static final class ItemModel extends SmartBlockModelBase {
        private final int color;

        public ItemModel(ItemStack stack) {
            Block block = Block.byItem(stack.getItem());
            color = block instanceof li.cil.oc.common.block.Screen screen ? Color.byTier(screen.tier()) : Color.byTier(Tier.One);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            Direction safeSide = side != null ? side : Direction.SOUTH;
            var screenTextures = Textures.Block.Screen.INSTANCE;
            net.minecraft.resources.ResourceLocation texture = side == Direction.NORTH || side == null
                ? screenTextures.SingleFront[0]
                : screenTextures.Single[side.get3DDataValue()];
            return List.of(bakeQuad(safeSide, Textures.getSprite(texture), color, 0));
        }
    }
}
