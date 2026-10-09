package li.cil.oc.server.machine;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class CallbackWrapper {
    private static final String OBJECT_NAME = Type.getInternalName(Object.class);
    private static final String CALL_DESCRIPTOR;
    private static final String[] CALL_INTERFACE = {Type.getInternalName(CallbackCall.class)};
    private static final Map<Method, CallbackCall> CACHE = new HashMap<>();
    private static final GeneratedClassLoader LOADER = new GeneratedClassLoader();

    static {
        try {
            CALL_DESCRIPTOR = Type.getMethodDescriptor(CallbackCall.class.getMethod(
                "call", Object.class, Context.class, Arguments.class));
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private CallbackWrapper() {
    }

    public static synchronized CallbackCall createCallbackWrapper(Method method) {
        return CACHE.computeIfAbsent(method, CallbackWrapper::createWrapper);
    }

    private static CallbackCall createWrapper(Method method) {
        String className = "generated.li.cil.oc.CallWrapper_" +
            method.getDeclaringClass().getName().replace('.', '_') + "_" + method.getName();
        if (!LOADER.containsClass(className)) {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                className.replace('.', '/'), null, OBJECT_NAME, CALL_INTERFACE);
            emitConstructor(writer);
            emitCallbackCall(method, writer);
            writer.visitEnd();
            LOADER.addClass(className, writer.toByteArray());
        }
        try {
            return (CallbackCall) LOADER.findClass(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not create callback wrapper for " + method, exception);
        }
    }

    private static void emitConstructor(ClassWriter writer) {
        MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        visitor.visitCode();
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, OBJECT_NAME, "<init>", "()V", false);
        visitor.visitInsn(Opcodes.RETURN);
        visitor.visitMaxs(1, 1);
        visitor.visitEnd();
    }

    private static void emitCallbackCall(Method method, ClassWriter writer) {
        String className = Type.getInternalName(method.getDeclaringClass());
        MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, "call", CALL_DESCRIPTOR, null, null);
        visitor.visitCode();
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitTypeInsn(Opcodes.CHECKCAST, className);
        visitor.visitVarInsn(Opcodes.ALOAD, 2);
        visitor.visitVarInsn(Opcodes.ALOAD, 3);
        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, method.getName(),
            Type.getMethodDescriptor(method), false);
        visitor.visitInsn(Opcodes.ARETURN);
        visitor.visitMaxs(3, 3);
        visitor.visitEnd();
    }

    private static final class GeneratedClassLoader extends ClassLoader {
        private final Map<String, Class<?>> classes = new HashMap<>();

        private GeneratedClassLoader() {
            super(CallbackWrapper.class.getClassLoader());
        }

        private boolean containsClass(String name) {
            return classes.containsKey(name);
        }

        private void addClass(String name, byte[] bytes) {
            classes.put(name, defineClass(name, bytes, 0, bytes.length));
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Class<?> result = classes.get(name);
            return result != null ? result : super.findClass(name);
        }
    }
}
