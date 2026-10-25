package li.cil.oc.client.renderer.font;

import li.cil.oc.util.TextBuffer;
import scala.Tuple2;

public interface TextBufferRenderData {
    boolean dirty();

    void dirty_$eq(boolean value);

    TextBuffer data();

    Tuple2<Object, Object> viewport();
}
