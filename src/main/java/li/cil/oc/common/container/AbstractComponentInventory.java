package li.cil.oc.common.container;

/** Java inventory host that supplies shared component state without compiler-generated accessors. */
public abstract class AbstractComponentInventory implements ComponentInventory {
    private final State componentState = new State();

    @Override public final State componentInventoryState() { return componentState; }
    public final boolean isInventorySizeReady() { return isSizeInventoryReady(); }
    public final void setInventorySizeReady(boolean value) { isSizeInventoryReady_$eq(value); }
}
