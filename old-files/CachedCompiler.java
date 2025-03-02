/*
 * Copyright 2014 Higher Frequency Trading
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.durmiendo.ajf.utils.memorycompiler;

import arc.util.Log;
import arc.util.Nullable;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static org.durmiendo.ajf.utils.memorycompiler.CompilerUtils.*;

@SuppressWarnings("StaticNonFinalField")
public class CachedCompiler implements Closeable {
    private static final PrintWriter DEFAULT_WRITER = new PrintWriter(System.err);
    private static final List<String> DEFAULT_OPTIONS = Arrays.asList("-g", "-nowarn");

    private final Map<ClassLoader, Map<String, Class<?>>> loadedClassesMap = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<ClassLoader, MemoryJavaFileManager> fileManagerMap = Collections.synchronizedMap(new WeakHashMap<>());
    public Function<StandardJavaFileManager, MemoryJavaFileManager> fileManagerOverride;

    private final File sourceDir;
    private final File classDir;
    private final List<String> options;

    private final ConcurrentMap<String, JavaFileObject> javaFileObjects = new ConcurrentHashMap<>();

    public CachedCompiler(File sourceDir, File classDir) {
        this(sourceDir, classDir, DEFAULT_OPTIONS);
    }

    public CachedCompiler(File sourceDir, File classDir, List<String> options) {
        this.sourceDir = sourceDir;
        this.classDir = classDir;
        this.options = options;
    }

    public void close() {
        try {
            for (MemoryJavaFileManager fileManager : fileManagerMap.values()) {
                fileManager.close();
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public Class<?> loadFromJava(String className, String javaCode) throws ClassNotFoundException {
        return loadFromJava(getClass().getClassLoader(), className, javaCode, DEFAULT_WRITER);
    }

    public Class<?> loadFromJava(ClassLoader classLoader,
                              String className,
                              String javaCode) throws ClassNotFoundException {
        return loadFromJava(classLoader, className, javaCode, DEFAULT_WRITER);
    }

    
    Map<String, byte[]> compileFromJava(String className, String javaCode, MemoryJavaFileManager fileManager) {
        return compileFromJava(className, javaCode, DEFAULT_WRITER, fileManager);
    }

    
    Map<String, byte[]> compileFromJava(String className,
                                        String javaCode,
                                        final PrintWriter writer,
                                        MemoryJavaFileManager fileManager) {
        Iterable<? extends JavaFileObject> compilationUnits;
        if (sourceDir != null) {
            String filename = className.replaceAll("\\.", '\\' + File.separator) + ".java";
            File file = new File(sourceDir, filename);
            writeText(file, javaCode);
            if (s_standardJavaFileManager == null)
                s_standardJavaFileManager = s_compiler.getStandardFileManager(null, null, null);
            compilationUnits = s_standardJavaFileManager.getJavaFileObjects(file);

        } else {
            javaFileObjects.put(className, new StringJavaFileObject(className, javaCode));
            compilationUnits = new ArrayList<>(javaFileObjects.values()); // To prevent CME from compiler code
        }
        // reuse the same file manager to allow caching of jar files
        boolean ok = s_compiler.getTask(writer, fileManager, Log::info, options, null, compilationUnits).call();

        Log.info(ok);
        if (!ok) {
            // compilation error, so we want to exclude this file from future compilation passes
            if (sourceDir == null)
                javaFileObjects.remove(className);

            // nothing to return due to compiler error
            return Collections.emptyMap();
        } else {
            Map<String, byte[]> result = fileManager.getAllBuffers();

            return result;
        }
    }

    public Class<?> loadFromJava(ClassLoader classLoader,
                              String className,
                              String javaCode,
                              @Nullable PrintWriter writer) throws ClassNotFoundException {
        Class<?> clazz = null;
        Map<String, Class<?>> loadedClasses;
        synchronized (loadedClassesMap) {
            loadedClasses = loadedClassesMap.get(classLoader);
            if (loadedClasses == null)
                loadedClassesMap.put(classLoader, loadedClasses = new LinkedHashMap<>());
            else
                clazz = loadedClasses.get(className);
        }
        PrintWriter printWriter = (writer == null ? DEFAULT_WRITER : writer);
        if (clazz != null)
            return clazz;

        MemoryJavaFileManager fileManager = fileManagerMap.get(classLoader);
        if (fileManager == null) {
            StandardJavaFileManager standardJavaFileManager = s_compiler.getStandardFileManager(null, null, null);
            fileManager = getFileManager(standardJavaFileManager);
            fileManagerMap.put(classLoader, fileManager);
        }
        final Map<String, byte[]> compiled = compileFromJava(className, javaCode, printWriter, fileManager);
        for (Map.Entry<String, byte[]> entry : compiled.entrySet()) {
            String className2 = entry.getKey();
            synchronized (loadedClassesMap) {
                if (loadedClasses.containsKey(className2))
                    continue;
            }
            byte[] bytes = entry.getValue();
            if (classDir != null) {
                String filename = className2.replaceAll("\\.", '\\' + File.separator) + ".class";
                boolean changed = writeBytes(new File(classDir, filename), bytes);
            }

            synchronized (className2.intern()) { // To prevent duplicate class definition error
                synchronized (loadedClassesMap) {
                    if (loadedClasses.containsKey(className2))
                        continue;
                }

                Class<?> clazz2 = CompilerUtils.defineClass(classLoader, className2, bytes);
                synchronized (loadedClassesMap) {
                    loadedClasses.put(className2, clazz2);
                }
            }
        }
        synchronized (loadedClassesMap) {
            loadedClasses.put(className, clazz = classLoader.loadClass(className));
        }
        return clazz;
    }

    private MemoryJavaFileManager getFileManager(StandardJavaFileManager fm) {
        return fileManagerOverride != null
                ? fileManagerOverride.apply(fm)
                : new MemoryJavaFileManager(fm);
    }
}
