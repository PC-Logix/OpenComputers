package li.cil.oc.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

// Keep RenderSystem's cached state current while applying the matching GL state directly.
@OnlyIn(Dist.CLIENT)
public final class RenderState {
    private RenderState() {
    }

    public static String getErrorString(int errorCode) {
        return switch (errorCode) {
            case GL11.GL_NO_ERROR -> "No error";
            case GL11.GL_INVALID_ENUM -> "Enum argument out of range";
            case GL11.GL_INVALID_VALUE -> "Numeric argument out of range";
            case GL11.GL_INVALID_OPERATION -> "Operation illegal in current state";
            case GL11.GL_STACK_OVERFLOW -> "Command would cause a stack overflow";
            case GL11.GL_STACK_UNDERFLOW -> "Command would cause a stack underflow";
            case GL11.GL_OUT_OF_MEMORY -> "Not enough memory left to execute command";
            default -> String.format("Unknown [0x%X]", errorCode);
        };
    }

    public static void checkError(String where) {
        if (Settings.get().logOpenGLErrors()) {
            int error = GL11.glGetError();
            if (error != 0) {
                OpenComputers.log().warn("GL ERROR @ " + where + ": " + getErrorString(error));
            }
        }
    }

    public static void makeItBlend() {
        RenderSystem.enableBlend();
        GL11.glEnable(GL11.GL_BLEND);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void disableBlend() {
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ZERO);
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void setBlendAlpha(float alpha) {
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    }

    public static void bindTexture(int id) {
        RenderSystem.bindTexture(id);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
    }

    public static void mirrorScale(PoseStack stack, float sx, float sy, float sz) {
        stack.last().pose().mul(new Matrix4f().scaling(sx, sy, sz));
        if (sx != sy || sx != sz || sx <= 0) {
            float isx = 1f / sx;
            float isy = 1f / sy;
            float isz = 1f / sz;
            float invScale = isx * isy * isz;
            // Vanilla's inverse cube root fails for negative values.
            float normScale = Mth.fastInvCubeRoot(Mth.abs(invScale));
            if (invScale < 0) normScale = -normScale;
            stack.last().normal().mul(new Matrix3f().scaling(isx * normScale, isy * normScale, isz * normScale));
        }
    }
}
