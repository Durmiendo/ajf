package org.durmiendo.ajf.utils.memorycompiler;


import org.antlr.v4.runtime.misc.NotNull;
import sun.misc.Unsafe;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private final static Unsafe unsafe;
    private static final long OVERRIDE_OFFSET;

    static {
        long offset;
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
        try {
            Field f = AccessibleObject.class.getDeclaredField("override");
            offset = unsafe.objectFieldOffset(f);
        } catch (NoSuchFieldException e) {
            offset = 0;
        }
        OVERRIDE_OFFSET = offset;
    }

    private final StandardJavaFileManager fileManager;

    // synchronizing due to ConcurrentModificationException
    private final Map<String, CloseableByteArrayOutputStream> buffers = Collections.synchronizedMap(new LinkedHashMap<>());

    public MemoryJavaFileManager(StandardJavaFileManager fileManager) {
        super(fileManager);
        this.fileManager = fileManager;
    }

    // Apparently, this method might not be thread-safe.
    // See https://github.com/OpenHFT/Java-Runtime-Compiler/issues/85
    public synchronized Iterable<Set<Location>> listLocationsForModules(final Location location) {
        return invokeNamedMethodIfAvailable(location, "listLocationsForModules");
    }

    // Apparently, this method might not be thread-safe.
    // See https://github.com/OpenHFT/Java-Runtime-Compiler/issues/85
    public synchronized String inferModuleName(final Location location) {
        return invokeNamedMethodIfAvailable(location, "inferModuleName");
    }

    public ClassLoader getClassLoader(Location location) {
        return fileManager.getClassLoader(location);
    }

    public synchronized Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        return fileManager.list(location, packageName, kinds, recurse);
    }

    public String inferBinaryName(Location location, JavaFileObject file) {
        return fileManager.inferBinaryName(location, file);
    }

    public boolean isSameFile(FileObject a, FileObject b) {
        return fileManager.isSameFile(a, b);
    }

    public synchronized boolean handleOption(String current, Iterator<String> remaining) {
        return fileManager.handleOption(current, remaining);
    }

    public boolean hasLocation(Location location) {
        return fileManager.hasLocation(location);
    }

    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {

        if (location == StandardLocation.CLASS_OUTPUT) {
            boolean success;
            final byte[] bytes;
            synchronized (buffers) {
                success = buffers.containsKey(className) && kind == JavaFileObject.Kind.CLASS;
                bytes = buffers.get(className).toByteArray();
            }
            if (success) {

                return new SimpleJavaFileObject(URI.create(className), kind) {
                    @NotNull
                    public InputStream openInputStream() {
                        return new ByteArrayInputStream(bytes);
                    }
                };
            }
        }
        return fileManager.getJavaFileForInput(location, className, kind);
    }

    @NotNull
    public JavaFileObject getJavaFileForOutput(Location location, final String className, JavaFileObject.Kind kind, FileObject sibling) {
        return new SimpleJavaFileObject(URI.create(className), kind) {
            @NotNull
            public OutputStream openOutputStream() {
                // CloseableByteArrayOutputStream.closed is used to filter partial results from getAllBuffers()
                CloseableByteArrayOutputStream baos = new CloseableByteArrayOutputStream();

                // Reads from getAllBuffers() should be repeatable:
                // let's ignore compile result in case compilation of this class was triggered before
                buffers.putIfAbsent(className, baos);

                return baos;
            }
        };
    }

    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
        return fileManager.getFileForInput(location, packageName, relativeName);
    }

    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
        return fileManager.getFileForOutput(location, packageName, relativeName, sibling);
    }

    public void flush() {
        // Do nothing
    }

    public void close() throws IOException {
        fileManager.close();
    }

    public int isSupportedOption(String option) {
        return fileManager.isSupportedOption(option);
    }

    public void clearBuffers() {
        buffers.clear();
    }

    @NotNull
    public Map<String, byte[]> getAllBuffers() {
        Map<String, byte[]> ret = new LinkedHashMap<>(buffers.size() * 2);
        Map<String, CloseableByteArrayOutputStream> compiledClasses = new LinkedHashMap<>(ret.size());

        synchronized (buffers) {
            compiledClasses.putAll(buffers);
        }

        for (Map.Entry<String, CloseableByteArrayOutputStream> e : compiledClasses.entrySet()) {
            try {
                // Await for compilation in case class is still being compiled in previous compiler run.
                e.getValue().closeFuture().get(30, TimeUnit.SECONDS);
            } catch (InterruptedException t) {
                Thread.currentThread().interrupt();

                break;
            } catch (ExecutionException | TimeoutException t) {
                continue;
            }

            final byte[] value = e.getValue().toByteArray();

            ret.put(e.getKey(), value);
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeNamedMethodIfAvailable(final Location location, final String name) {
        final Method[] methods = JavaFileManager.class.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(name) && method.getParameterTypes().length == 1 &&
                    method.getParameterTypes()[0] == Location.class) {
                try {
                    if (OVERRIDE_OFFSET == 0)
                        method.setAccessible(true);
                    else
                        unsafe.putBoolean(method, OVERRIDE_OFFSET, true);
                    return (T) method.invoke(fileManager, location);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new UnsupportedOperationException("Unable to invoke method " + name, e);
                }
            }
        }
        throw new UnsupportedOperationException("Unable to find method " + name);
    }
}
