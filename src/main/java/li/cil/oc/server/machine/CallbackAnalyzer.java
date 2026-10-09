package li.cil.oc.server.machine;

import li.cil.oc.api.driver.MethodWhitelist;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.FilteredEnvironment;
import li.cil.oc.api.network.ManagedPeripheral;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Discovers component and peripheral callbacks for the machine runtime. */
public final class CallbackAnalyzer {
    private final Logger log;

    public CallbackAnalyzer(Logger log) {
        this.log = log;
    }

    public Map<String, Callback> fromClass(Class<?> environment) {
        Map<String, Callback> callbacks = new LinkedHashMap<>();
        staticAnalyze(environment, name -> true, callbacks);
        return callbacks;
    }

    public Map<String, Callback> analyze(List<?> environments) {
        List<EnvironmentEntry> entries = new ArrayList<>();
        Set<String> whitelist = null;
        for (Object environment : environments) {
            if (environment instanceof MethodWhitelist listed) {
                String[] methods = listed.whitelistedMethods();
                Set<String> allowed = methods == null ? Set.of() : new HashSet<>(Arrays.asList(methods));
                if (whitelist == null) whitelist = new HashSet<>(allowed);
                else whitelist.retainAll(allowed);
            }
            int priority = environment instanceof NamedBlock named ? named.priority() : 0;
            entries.add(new EnvironmentEntry(environment, priority));
        }

        Set<String> allowed = whitelist;
        Map<String, Callback> callbacks = new LinkedHashMap<>();
        entries.sort((left, right) -> Integer.compare(-left.priority, -right.priority));
        for (EnvironmentEntry entry : entries) {
            Object environment = entry.environment;
            Predicate<String> filter = name -> !callbacks.containsKey(name)
                && (allowed == null || allowed.isEmpty() || allowed.contains(name))
                && (!(environment instanceof FilteredEnvironment filtered) || filtered.isCallbackEnabled(name));
            if (environment instanceof ManagedPeripheral peripheral) {
                for (String name : peripheral.methods()) {
                    if (filter.test(name)) callbacks.put(name, new PeripheralCallback(name));
                }
            }
            staticAnalyze(environment.getClass(), filter, callbacks);
        }
        return callbacks;
    }

    private void staticAnalyze(Class<?> seed, Predicate<String> shouldAdd, Map<String, Callback> callbacks) {
        for (Class<?> type = seed; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(li.cil.oc.api.machine.Callback.class)) continue;
                String target = method.getDeclaringClass().getName() + "." + method.getName();
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length != 2 || parameters[0] != Context.class || parameters[1] != Arguments.class) {
                    log.error("Invalid use of Callback annotation on {}: invalid argument types or count.", target);
                } else if (method.getReturnType() != Object[].class) {
                    log.error("Invalid use of Callback annotation on {}: invalid return type.", target);
                } else if (!Modifier.isPublic(method.getModifiers())) {
                    log.error("Invalid use of Callback annotation on {}: method must be public.", target);
                } else {
                    li.cil.oc.api.machine.Callback annotation = method.getAnnotation(li.cil.oc.api.machine.Callback.class);
                    String name = annotation.value() != null && !annotation.value().trim().isEmpty()
                        ? annotation.value() : method.getName();
                    if (shouldAdd.test(name)) callbacks.put(name, new ComponentCallback(method, annotation));
                }
            }
        }
    }

    private record EnvironmentEntry(Object environment, int priority) {
    }

    public abstract static class Callback {
        public final li.cil.oc.api.machine.Callback annotation;

        protected Callback(li.cil.oc.api.machine.Callback annotation) {
            this.annotation = annotation;
        }

        public abstract Object[] apply(Object instance, Context context, Arguments args) throws Exception;
    }

    public static final class ComponentCallback extends Callback {
        public final Method method;
        private final CallbackCall callWrapper;

        public ComponentCallback(Method method, li.cil.oc.api.machine.Callback annotation) {
            super(annotation);
            this.method = method;
            this.callWrapper = CallbackWrapper.createCallbackWrapper(method);
        }

        @Override
        public Object[] apply(Object instance, Context context, Arguments args) {
            return callWrapper.call(instance, context, args);
        }
    }

    public static final class PeripheralCallback extends Callback {
        private final String name;

        public PeripheralCallback(String name) {
            super(new PeripheralAnnotation(name));
            this.name = name;
        }

        @Override
        public Object[] apply(Object instance, Context context, Arguments args) throws Exception {
            if (instance instanceof ManagedPeripheral peripheral) return peripheral.invoke(name, context, args);
            throw new NoSuchMethodException();
        }
    }
}
