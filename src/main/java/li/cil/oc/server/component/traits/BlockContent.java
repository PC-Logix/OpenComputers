package li.cil.oc.server.component.traits;

/** Result of the robot's block-content check: whether movement is blocked and why. */
public record BlockContent(Object detected, String kind) { }
