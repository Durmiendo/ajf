package org.durmiendo.ajf.utils.memorycompiler;

import arc.struct.ObjectMap;

public class InMemoryClassLoader extends ClassLoader {
    private InMemoryFileManager manager;

    public InMemoryClassLoader(ClassLoader parent, InMemoryFileManager manager) {
        super(parent);
        this.manager = manager;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        ObjectMap<String, InMemoryOutputJavaFile> compiledClasses = manager.getBytesMap();

        if (compiledClasses.containsKey(name)) {
            byte[] bytes = compiledClasses.get(name).getBytes();
            return defineClass(name, bytes, 0, bytes.length);
        } else {
            throw new ClassNotFoundException();
        }
    }
}
