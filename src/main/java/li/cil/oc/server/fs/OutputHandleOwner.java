package li.cil.oc.server.fs;

/** Owner contract used by output handles to remove themselves when closed. */
public interface OutputHandleOwner {
    void removeOutputHandle(int handle);
}
