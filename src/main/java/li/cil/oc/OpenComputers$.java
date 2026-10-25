package li.cil.oc;

import li.cil.oc.common.Proxy;
import org.apache.logging.log4j.Logger;

/** Compatibility accessors for Java sources that previously used the Scala companion object. */
public final class OpenComputers$ {
    public static final OpenComputers$ MODULE$ = new OpenComputers$();

    private OpenComputers$() {
    }

    public String ID() {
        return OpenComputers.ID;
    }

    public String Name() {
        return OpenComputers.Name;
    }

    public String McVersion() {
        return OpenComputers.McVersion;
    }

    public String Version() {
        return OpenComputers.Version;
    }

    public Logger log() {
        return OpenComputers.log;
    }

    public Proxy proxy() {
        return OpenComputers.proxy;
    }
}
