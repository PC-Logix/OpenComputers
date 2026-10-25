package li.cil.oc.client;

public final class ManualHistory {
    public final String path;
    public int offset;

    public ManualHistory(String path) {
        this(path, 0);
    }

    public ManualHistory(String path, int offset) {
        this.path = path;
        this.offset = offset;
    }

    public String path() {
        return path;
    }

    public int offset() {
        return offset;
    }

    public void offset_$eq(int offset) {
        this.offset = offset;
    }
}
