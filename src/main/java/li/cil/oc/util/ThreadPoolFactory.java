package li.cil.oc.util;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.server.fs.Buffered;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadPoolFactory {
    public static final ThreadPoolFactory INSTANCE = new ThreadPoolFactory();
    private static final int priority = threadPriority();
    private static final List<SafeThreadPool> safePools = new ArrayList<>();

    private ThreadPoolFactory() {}

    public static int priority() { return priority; }

    public static List<SafeThreadPool> safePools() { return safePools; }

    private static int threadPriority() {
        int custom = Settings.get() == null ? -1 : Settings.get().threadPriority();
        if (custom < 1) return Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2;
        return Math.max(Thread.MIN_PRIORITY, Math.min(Thread.MAX_PRIORITY, custom));
    }

    @SubscribeEvent
    public void serverStart(ServerAboutToStartEvent event) {
        SaveHandler.initialize();
        Buffered.fileSaveHandler();
        for (SafeThreadPool pool : safePools) pool.newThreadPool();

        if (Settings.get().internetAccessConfigured()) {
            if (Settings.get().internetFilteringRulesInvalid()) {
                OpenComputers.log().warn("####################################################");
                OpenComputers.log().warn("#                                                  #");
                OpenComputers.log().warn("#  Could not parse Internet Card filtering rules!  #");
                OpenComputers.log().warn("#  Review the server log and adjust the filtering  #");
                OpenComputers.log().warn("#  list to ensure it is appropriately configured.  #");
                OpenComputers.log().warn("#   (config/OpenComputers.cfg => filteringRules)   #");
                OpenComputers.log().warn("# Internet access has been automatically disabled. #");
                OpenComputers.log().warn("#                                                  #");
                OpenComputers.log().warn("####################################################");
            }
            else if (!Settings.get().internetFilteringRulesObserved() && event.getServer().isDedicatedServer()) {
                OpenComputers.log().warn("####################################################");
                OpenComputers.log().warn("#                                                  #");
                OpenComputers.log().warn("#    It appears that you're running a dedicated    #");
                OpenComputers.log().warn("#  server with OpenComputers installed! Make sure  #");
                OpenComputers.log().warn("#  to review the Internet Card address filtering   #");
                OpenComputers.log().warn("#  list to ensure it is appropriately configured.  #");
                OpenComputers.log().warn("#   (config/OpenComputers.cfg => filteringRules)   #");
                OpenComputers.log().warn("#                                                  #");
                OpenComputers.log().warn("####################################################");
            }
            else OpenComputers.log().info("Successfully applied {} Internet Card filtering rules.",
                Settings.get().internetFilteringRules().length);
        }
    }

    @SubscribeEvent
    public void serverStop(ServerStoppedEvent event) {
        for (SafeThreadPool pool : safePools) pool.waitForCompletion();
    }

    public static ScheduledExecutorService create(String name, int threads) {
        return Executors.newScheduledThreadPool(threads, new ThreadFactory() {
            private final String baseName = "OpenComputers-" + name + "-";
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, baseName + threadNumber.getAndIncrement());
                if (!thread.isDaemon()) thread.setDaemon(true);
                if (thread.getPriority() != priority) thread.setPriority(priority);
                return thread;
            }
        });
    }

    public static SafeThreadPool createSafePool(String name, int threads) {
        SafeThreadPool pool = new SafeThreadPool(name, threads);
        safePools.add(pool);
        return pool;
    }
}
