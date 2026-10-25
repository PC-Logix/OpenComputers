package li.cil.oc.client.renderer;

import java.util.OptionalDouble;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc.OpenComputers;
import li.cil.oc.client.Textures;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderTypes extends RenderType {
    private static final RenderStateShard.ShaderStateShard POSITION_TEX_COLOR_SHADER =
            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);
    private static final RenderStateShard.TextureStateShard WHITE_TEXTURE =
            new RenderStateShard.TextureStateShard(ResourceLocation.withDefaultNamespace("textures/misc/white.png"), false, false);

    public static final RenderStateShard.TextureStateShard ROBOT_CHASSIS_TEXTURE = new RenderStateShard.TextureStateShard(Textures.Model.Robot, false, false);

    public static final RenderType ROBOT_CHASSIS = create(OpenComputers.ID() + ":robot_chassis",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.TRIANGLES, 1024, true, false, CompositeState.builder()
                    .setShaderState(RENDERTYPE_CUTOUT_SHADER)
                    .setTextureState(ROBOT_CHASSIS_TEXTURE)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(true));

    public static final RenderType ROBOT_LIGHT = create(OpenComputers.ID() + ":robot_light",
            DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, true, false, CompositeState.builder()
                    .setShaderState(POSITION_TEX_COLOR_SHADER)
                    .setTextureState(ROBOT_CHASSIS_TEXTURE)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .createCompositeState(true));

    private static RenderType createRobotFlag(String name, ResourceLocation texture) {
        return create(OpenComputers.ID() + ":robot_flag_" + name,
                DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, true, false, CompositeState.builder()
                        .setShaderState(RENDERTYPE_CUTOUT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setLightmapState(LIGHTMAP)
                        .setCullState(NO_CULL)
                        .createCompositeState(true));
    }

    private static final Map<ResourceLocation, RenderType> ROBOT_FLAGS = new ConcurrentHashMap<>();

    public static RenderType robotFlag(ResourceLocation flag) {
        return ROBOT_FLAGS.computeIfAbsent(flag, id -> createRobotFlag(
                id.getPath(),
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "model/robot_" + id.getPath())
        ));
    }

    public static final RenderType ROBOT_RAINBOW_FLAG = createRobotFlag("rainbow", Textures.Model.RobotRainbowFlag);

    public static final RenderType ROBOT_TRANS_FLAG = createRobotFlag("trans", Textures.Model.RobotTransFlag);

    public static final RenderType HOLOGRAM = create(OpenComputers.ID() + ":hologram",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            // 48*48*48 voxels * 6 faces * 4 verts * ~8 bytes = ~25 MB worst case; 1<<22 is a safe upper bound.
            1 << 22,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true));


    private static RenderType createUpgrade(String name, ResourceLocation texture) {
        return create(OpenComputers.ID() + ":upgrade_" + name,
                DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2048, true, false, CompositeState.builder()
                        .setShaderState(RENDERTYPE_CUTOUT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setLightmapState(LIGHTMAP)
                        .createCompositeState(true));
    }

    public static final RenderType UPGRADE_CRAFTING = createUpgrade("crafting", Textures.Model.UpgradeCrafting);

    public static final RenderType UPGRADE_GENERATOR = createUpgrade("generator", Textures.Model.UpgradeGenerator);

    public static final RenderType UPGRADE_INVENTORY = createUpgrade("inventory", Textures.Model.UpgradeInventory);

    public static final RenderType MFU_LINES = create(OpenComputers.ID() + ":mfu_lines",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.LINES, 1024, false, false, CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0)))
                    .createCompositeState(false));

    public static final RenderType MFU_QUADS = create(OpenComputers.ID() + ":mfu_quads",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    public static final RenderType BLOCK_OVERLAY = create(OpenComputers.ID() + ":overlay_block",
            DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1024, false, false, CompositeState.builder()
                    .setShaderState(POSITION_TEX_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .createCompositeState(false));

    public static final RenderType BLOCK_OVERLAY_COLOR = create(OpenComputers.ID() + ":overlay_block_color",
            DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1024, false, false, CompositeState.builder()
                    .setShaderState(POSITION_TEX_COLOR_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .createCompositeState(false));

    public static final RenderType FONT_QUAD = create(OpenComputers.ID() + ":font_quad",
            DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1024, false, false, CompositeState.builder()
                    .setShaderState(POSITION_TEX_COLOR_SHADER)
                    .setTextureState(WHITE_TEXTURE)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setTransparencyState(NO_TRANSPARENCY)
                    // NO_CULL required: block rendering works because ScreenRenderer.transform()
                    // applies mirrorScale(1,-1,1) which flips Y and reverses winding to CCW (front-face).
                    // GUI rendering has no Y-flip, so quads are CW (back-face) and get culled without this.
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private static class CustomTextureState extends RenderStateShard.TexturingStateShard {
        public CustomTextureState(int id) {
            super("custom_tex_" + id, () -> {
                RenderSystem.setShaderTexture(0, id);
            }, () -> {});
        }
    }

    private static class LinearTexturingState extends RenderStateShard.TexturingStateShard {
        public LinearTexturingState(boolean linear) {
            super(linear ? "lin_font_texturing" : "near_font_texturing", () -> {
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, linear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
            }, () -> {});
        }
    }

    private static final LinearTexturingState NEAR = new LinearTexturingState(false);
    private static final LinearTexturingState LINEAR = new LinearTexturingState(true);

    public static RenderType createFontTex(String name, ResourceLocation texture, boolean linear) {
        return create(OpenComputers.ID() + ":font_stat_" + name,
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1024, false, false, CompositeState.builder()
                        .setShaderState(POSITION_TEX_COLOR_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTexturingState(linear ? LINEAR : NEAR)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        // NO_CULL required: see FONT_QUAD comment above.
                        .setCullState(NO_CULL)
                        .createCompositeState(false));
    }

    public static RenderType createFontTex(int id) {
        return create(OpenComputers.ID() + ":font_dyn_" + id,
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1024, false, false, CompositeState.builder()
                        .setShaderState(POSITION_TEX_COLOR_SHADER)
                        .setTexturingState(new CustomTextureState(id))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        // NO_CULL required: see FONT_QUAD comment above.
                        .setCullState(NO_CULL)
                        .createCompositeState(false));
    }

    public static RenderType createTexturedQuad(String name, ResourceLocation texture, VertexFormat format, boolean additive) {
        RenderStateShard.ShaderStateShard shader = format == DefaultVertexFormat.POSITION_TEX ? POSITION_TEX_SHADER : POSITION_TEX_COLOR_SHADER;

        return create(OpenComputers.ID() + ":tex_quad_" + name,
                format, VertexFormat.Mode.QUADS, 1024, false, false, CompositeState.builder()
                        .setShaderState(shader)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(additive ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                        .createCompositeState(false));
    }

    private RenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufSize, boolean delegate, boolean sorting, Runnable setup, Runnable clear) {
        super(name, format, mode, bufSize, delegate, sorting, setup, clear);
        throw new Error();
    }
}
