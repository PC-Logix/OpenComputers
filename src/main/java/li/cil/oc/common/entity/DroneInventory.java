package li.cil.oc.common.entity;

/** Inventory facade used by the drone menu to reach its backing entity. */
public abstract class DroneInventory implements li.cil.oc.common.container.Inventory {
    private final Drone drone;

    protected DroneInventory(Drone drone) { this.drone = drone; }

    public Drone drone() { return drone; }
}
