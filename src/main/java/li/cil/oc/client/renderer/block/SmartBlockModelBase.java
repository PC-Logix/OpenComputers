package li.cil.oc.client.renderer.block;

import li.cil.oc.client.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;
import scala.Option;

import java.util.Collections;

public abstract class SmartBlockModelBase implements BakedModel {
    protected final int White = 0xFFFFFF;
    protected final ItemTransforms DefaultBlockCameraTransforms = createDefaultTransforms();
    protected final Vec3[][] UnitCube = {
        {new Vec3(0, 0, 1), new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 1)},
        {new Vec3(0, 1, 0), new Vec3(0, 1, 1), new Vec3(1, 1, 1), new Vec3(1, 1, 0)},
        {new Vec3(1, 1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 0), new Vec3(0, 1, 0)},
        {new Vec3(0, 1, 1), new Vec3(0, 0, 1), new Vec3(1, 0, 1), new Vec3(1, 1, 1)},
        {new Vec3(0, 1, 0), new Vec3(0, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 1, 1)},
        {new Vec3(1, 1, 1), new Vec3(1, 0, 1), new Vec3(1, 0, 0), new Vec3(1, 1, 0)}
    };
    private final Vec3[][] planes = {
        {new Vec3(1, 0, 0), new Vec3(0, 0, -1)},
        {new Vec3(1, 0, 0), new Vec3(0, 0, 1)},
        {new Vec3(-1, 0, 0), new Vec3(0, -1, 0)},
        {new Vec3(1, 0, 0), new Vec3(0, -1, 0)},
        {new Vec3(0, 0, 1), new Vec3(0, -1, 0)},
        {new Vec3(0, 0, -1), new Vec3(0, -1, 0)}
    };

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public java.util.List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return Collections.emptyList();
    }

    @Override
    public java.util.List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData extraData, RenderType renderType) {
        return getQuads(state, side, random);
    }

    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }

    @Deprecated
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return Textures.getSprite(Textures.Block.GenericTop);
    }

    @Deprecated
    @Override
    public ItemTransforms getTransforms() {
        return DefaultBlockCameraTransforms;
    }

    @Deprecated
    protected final ItemTransforms DefaultBlockCameraTransforms() {
        return DefaultBlockCameraTransforms;
    }

    protected BakedModel missingModel() {
        return Minecraft.getInstance().getModelManager().getMissingModel();
    }

    protected Vec3[][] makeBox(Vec3 from, Vec3 to) {
        double minX = Math.min(from.x, to.x), minY = Math.min(from.y, to.y), minZ = Math.min(from.z, to.z);
        double maxX = Math.max(from.x, to.x), maxY = Math.max(from.y, to.y), maxZ = Math.max(from.z, to.z);
        Vec3[][] result = new Vec3[UnitCube.length][];
        for (int face = 0; face < UnitCube.length; face++) {
            result[face] = new Vec3[UnitCube[face].length];
            for (int vertex = 0; vertex < UnitCube[face].length; vertex++) {
                Vec3 point = UnitCube[face][vertex];
                result[face][vertex] = new Vec3(
                    Math.max(minX, Math.min(maxX, point.x)),
                    Math.max(minY, Math.min(maxY, point.y)),
                    Math.max(minZ, Math.min(maxZ, point.z)));
            }
        }
        return result;
    }

    protected BakedQuad[] bakeQuads(Vec3[][] box, TextureAtlasSprite[] textures, Option<Integer> color) {
        return bakeQuads(box, textures, color.isDefined() ? color.get() : White);
    }

    protected BakedQuad[] bakeQuads(Vec3[][] box, TextureAtlasSprite[] textures, int colorRGB) {
        Direction[] directions = Direction.values();
        BakedQuad[] quads = new BakedQuad[directions.length];
        for (Direction side : directions) {
            int index = side.get3DDataValue();
            int[] data = quadData(box[index], side, textures[index], colorRGB, 0);
            quads[index] = new BakedQuad(data, -1, side, textures[index], true);
        }
        return quads;
    }

    protected BakedQuad bakeQuad(Direction side, TextureAtlasSprite texture, Option<Integer> color, int rotation) {
        int colorRGB = color.isDefined() ? color.get() : White;
        return bakeQuad(side, texture, colorRGB, rotation);
    }

    protected BakedQuad bakeQuad(Direction side, TextureAtlasSprite texture, int colorRGB, int rotation) {
        int index = side.get3DDataValue();
        int[] data = quadData(UnitCube[index], side, texture, colorRGB, rotation);
        return new BakedQuad(data, -1, side, texture, true);
    }

    protected int[] quadData(Vec3[] vertices, Direction facing, TextureAtlasSprite texture, int colorRGB, int rotation) {
        Vec3 uAxis = planes[facing.get3DDataValue()][0];
        Vec3 vAxis = planes[facing.get3DDataValue()][1];
        int rot = (rotation + 4) % 4;
        int[] result = new int[vertices.length * 8];
        for (int i = 0; i < vertices.length; i++) {
            Vec3 vertex = vertices[i];
            float u = (float) vertex.dot(uAxis);
            float v = (float) vertex.dot(vAxis);
            if (uAxis.x + uAxis.y + uAxis.z < 0) u = 1 + u;
            if (vAxis.x + vAxis.y + vAxis.z < 0) v = 1 + v;
            for (int turn = 0; turn < rot; turn++) {
                float tmp = u;
                u = v;
                v = (-(tmp - 0.5f)) + 0.5f;
            }
            int[] raw = rawData(vertex.x, vertex.y, vertex.z, facing, texture, texture.getU(u), texture.getV(v), colorRGB);
            System.arraycopy(raw, 0, result, i * raw.length, raw.length);
        }
        return result;
    }

    protected int[] rawData(double x, double y, double z, Direction face, TextureAtlasSprite texture, float u, float v, int colorRGB) {
        return rawData(x, y, z, face, texture, u, v, colorRGB, 0);
    }

    protected int[] rawData(double x, double y, double z, Direction face, TextureAtlasSprite texture, float u, float v, int colorRGB, int light) {
        int vx = (face.getStepX() * 127) & 0xFF;
        int vy = (face.getStepY() * 127) & 0xFF;
        int vz = (face.getStepZ() * 127) & 0xFF;
        int red = (colorRGB >> 16) & 0xFF;
        int green = (colorRGB >> 8) & 0xFF;
        int blue = colorRGB & 0xFF;
        return new int[] {
            Float.floatToRawIntBits((float) x),
            Float.floatToRawIntBits((float) y),
            Float.floatToRawIntBits((float) z),
            0xFF000000 | blue << 16 | green << 8 | red,
            Float.floatToRawIntBits(u),
            Float.floatToRawIntBits(v),
            light,
            vx | (vy << 0x08) | (vz << 0x10)
        };
    }

    private static ItemTransforms createDefaultTransforms() {
        ItemTransform gui = new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f), new Vector3f());
        ItemTransform ground = new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 3, 0), new Vector3f(0.25f, 0.25f, 0.25f), new Vector3f());
        ItemTransform fixed = new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f());
        ItemTransform thirdPersonRight = new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f, 0), new Vector3f(0.375f, 0.375f, 0.375f), new Vector3f());
        ItemTransform firstPersonRight = new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.40f, 0.40f, 0.40f), new Vector3f());
        ItemTransform firstPersonLeft = new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.40f, 0.40f, 0.40f), new Vector3f());
        gui.translation.mul(0.0625f);
        ground.translation.mul(0.0625f);
        fixed.translation.mul(0.0625f);
        thirdPersonRight.translation.mul(0.0625f);
        firstPersonRight.translation.mul(0.0625f);
        firstPersonLeft.translation.mul(0.0625f);
        return new ItemTransforms(ItemTransform.NO_TRANSFORM, thirdPersonRight, firstPersonLeft, firstPersonRight,
            ItemTransform.NO_TRANSFORM, gui, ground, fixed);
    }
}
