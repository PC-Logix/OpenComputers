package li.cil.oc.util;

public final class MovingAverage {
    private final int[] data;
    private int head;
    private int cachedAverage;
    private boolean dirty = true;

    public MovingAverage(int size) {
        data = new int[size];
    }

    public int size() {
        return data.length;
    }

    public int get() {
        if (dirty) {
            int sum = 0;
            for (int value : data) sum += value;
            cachedAverage = sum / data.length;
            dirty = false;
        }
        return cachedAverage;
    }

    public MovingAverage add(int value) {
        data[head] = value;
        head = (head + 1) % data.length;
        dirty = true;
        return this;
    }
}
