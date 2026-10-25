package li.cil.oc.common.blockentity.traits;

/** Power-capable host; lifecycle dispatch belongs to the shared block-entity controller. */
public interface PowerAcceptor extends li.cil.oc.common.blockentity.traits.power.Common {
    boolean isServer();

    /** Preserves the host's original ordering relative to environment lifecycle callbacks. */
    default boolean powerLifecycleAfterEnvironment() { return false; }
    default boolean powerLifecycleAfterHub() { return false; }
}
