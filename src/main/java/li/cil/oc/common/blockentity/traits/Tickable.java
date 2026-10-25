package li.cil.oc.common.blockentity.traits;

/** Shared tick callback for block entities integrated with the entity ticker. */
public interface Tickable {
    void updateEntity();

    default void tick() {
        updateEntity();
    }
}
