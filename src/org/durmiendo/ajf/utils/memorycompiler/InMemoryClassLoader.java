package org.durmiendo.ajf.utils.memorycompiler;

import arc.struct.ObjectMap;

public class InMemoryClassLoader extends ClassLoader {
    public ObjectMap<String, Class<?>> classes = new ObjectMap<>();

    public InMemoryClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void acceptNewClass(String name, byte[] bytes) {
        classes.put(name, defineClass(name, bytes, 0, bytes.length));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (classes.containsKey(name)) {
            return classes.get(name);
        } else {
            throw new ClassNotFoundException();
        }
    }
}
