package li.cil.oc.client.renderer.font;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.util.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

public class FontParserHex implements IGlyphProvider {
    private static final byte[] OPAQUE = {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
    private static final byte[] TRANSPARENT = {0, 0, 0, 0};

    private final Int2ObjectMap<byte[]> glyphs = new Int2ObjectOpenHashMap<>();

    private static int hex2int(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'F') {
            return c - ('A' - 10);
        } else if (c >= 'a' && c <= 'f') {
            return c - ('a' - 10);
        } else {
            throw new RuntimeException("invalid char: " + c);
        }
    }

    @Override
    public void initialize() {
        glyphs.clear();

        OpenComputers.log().info("Loading Unicode glyphs...");
        long time = System.currentTimeMillis();
        int glyphCount = 0;

        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), "font.hex");
        List<Resource> optRes = Minecraft.getInstance().getResourceManager().getResourceStack(loc);
        if (optRes.isEmpty()) return;
        for (Resource resource : optRes) {
            try (InputStream font = resource.open()) {
                final BufferedReader input = new BufferedReader(new InputStreamReader(font));
                String line;
                while ((line = input.readLine()) != null) {
                    final String info = line.substring(0, line.indexOf(':'));
                    final int charCode = Integer.parseInt(info, 16);
                    if (charCode < 0 || charCode >= FontUtils.codepoint_limit()) {
                        OpenComputers.log().warn(String.format("Unicode font contained unexpected glyph: U+%04X, ignoring", charCode));
                        continue; // Out of bounds.
                    }
                    final int expectedWidth = FontUtils.wcwidth(charCode);
                    if (expectedWidth < 1) continue; // Skip control characters.
                    // Two chars representing one byte represent one row of eight pixels.
                    int glyphStrOfs = info.length() + 1;
                    final byte[] glyph = new byte[(line.length() - glyphStrOfs) >> 1];
                    final int glyphWidth = glyph.length / getGlyphHeight();
                    if (expectedWidth == glyphWidth) {
                        for (int i = 0; i < glyph.length; i++, glyphStrOfs += 2) {
                            glyph[i] = (byte) ((hex2int(line.charAt(glyphStrOfs)) << 4) | (hex2int(line.charAt(glyphStrOfs + 1))));
                        }
                        if (!glyphs.containsKey(charCode)) {
                            glyphCount++;
                        }
                        glyphs.put(charCode, glyph);
                    } else if (Settings.get().logHexFontErrors()) {
                        OpenComputers.log().warn(String.format("Size of glyph for code point U+%04X (%s) in font (%d) does not match expected width (%d), ignoring.", charCode, (char) charCode, glyphWidth, expectedWidth));
                    }
                }
            } catch (IOException ex) {
                OpenComputers.log().warn("Error parsing font.", ex);
            }
        }

        OpenComputers.log().info("Loaded " + glyphCount + " glyphs in " + (System.currentTimeMillis() - time) + " milliseconds.");
    }

    @Override
    public ByteBuffer getGlyph(int charCode) {
        if (!glyphs.containsKey(charCode))
            return null;
        final byte[] glyph = glyphs.get(charCode);
        if (glyph == null || glyph.length == 0)
            return null;
        final ByteBuffer buffer = BufferUtils.createByteBuffer(glyph.length * getGlyphWidth() * 4);
        for (byte aGlyph : glyph) {
            int c = ((int) aGlyph) & 0xFF;
            // Grab all bits by grabbing the leftmost one then shifting.
            for (int j = 0; j < 8; j++) {
                final boolean isBitSet = (c & 0x80) > 0;
                if (isBitSet) buffer.put(OPAQUE);
                else buffer.put(TRANSPARENT);
                c <<= 1;
            }
        }
        ((Buffer) buffer).rewind();
        return buffer;
    }

    @Override
    public int getGlyphWidth() {
        return 8;
    }

    @Override
    public int getGlyphHeight() {
        return 16;
    }
}
