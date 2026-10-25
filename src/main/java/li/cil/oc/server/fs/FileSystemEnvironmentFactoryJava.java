package li.cil.oc.server.fs;

import li.cil.oc.SettingsAccessJava;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;

/** Creates managed filesystem environments with the legacy speed and energy-cost rules. */
public final class FileSystemEnvironmentFactoryJava {
    private FileSystemEnvironmentFactoryJava() { }

    public static li.cil.oc.server.component.FileSystem create(
            li.cil.oc.api.fs.FileSystem fileSystem, Label label, EnvironmentHost host,
            String accessSound, int speed) {
        return create(fileSystem, label, host, accessSound, speed,
                SettingsAccessJava.get().hddReadCost(), SettingsAccessJava.get().hddWriteCost());
    }

    public static li.cil.oc.server.component.FileSystem create(
            li.cil.oc.api.fs.FileSystem fileSystem, Label label, EnvironmentHost host,
            String accessSound, int speed, double readEnergyCost, double writeEnergyCost) {
        if (fileSystem == null) return null;
        return new li.cil.oc.server.component.FileSystem(fileSystem, label,
                java.util.Optional.ofNullable(host), java.util.Optional.ofNullable(accessSound),
                Math.max(0, Math.min(5, speed - 1)),
                Math.max(0, readEnergyCost), Math.max(0, writeEnergyCost));
    }
}
