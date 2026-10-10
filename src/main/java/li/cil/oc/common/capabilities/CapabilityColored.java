package li.cil.oc.common.capabilities;

import li.cil.oc.api.internal.Colored;

public final class CapabilityColored {
    private CapabilityColored() {
    }

    public static class DefaultImpl implements Colored {
        private int color;

        public int color() {
            return color;
        }

        public void color_$eq(int value) {
            color = value;
        }

        @Override
        public int getColor() {
            return color;
        }

        @Override
        public void setColor(int value) {
            color = value;
        }

        @Override
        public boolean controlsConnectivity() {
            return false;
        }
    }
}
