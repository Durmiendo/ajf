package org.durmiendo.ajf.utils.clazz;

import arc.struct.ObjectMap;

public class ClassFinder {
    @SuppressWarnings("unchecked")
    public static <T> Class<T> forName(String className, ObjectMap<String, Class<?>> imports) throws ClassNotFoundException {
        if (imports != null && imports.containsKey(className))
            return (Class<T>) imports.get(className);

        return switch (className) {
            case "float" -> (Class<T>) float.class;
            case "double" -> (Class<T>) double.class;
            case "byte" -> (Class<T>) byte.class;
            case "int" -> (Class<T>) int.class;
            case "short" -> (Class<T>) short.class;
            case "long" -> (Class<T>) long.class;
            case "char" -> (Class<T>) char.class;
            default -> (Class<T>) Class.forName(className);
        };
    }
}
