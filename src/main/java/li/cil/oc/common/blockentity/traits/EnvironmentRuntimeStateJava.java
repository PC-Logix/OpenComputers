package li.cil.oc.common.blockentity.traits;

/** Owns the deferred dirty-state bit used by ticking block-entity environments. */
public final class EnvironmentRuntimeStateJava {
    private boolean changeScheduled;

    public boolean isChangeScheduled() { return changeScheduled; }
    public void setChangeScheduled(boolean value) { changeScheduled = value; }
}
