package org.durmiendo.ajf.mods.jcfg;

import arc.struct.ObjectMap;

import java.lang.reflect.Field;

/**
 * Предоставляет доступ к переменным виртуальных классов.
 */
public class GenericJCFGUtils implements JCFGUtils {
    public ObjectMap<String, JCFGVar> globalObjectsList = new ObjectMap<>();

    @Override
    public String addGlobalObject(String name, JCFGVar var) {
        globalObjectsList.put(name, var);
        return name;
    }

    @SuppressWarnings("unused")
    @Override
    public <T> T getGlobalObject(String name, Class<?> targetType) {
        return (T) globalObjectsList.get(name).get(targetType);
    }

    @SuppressWarnings("unused")
    @Override
    public Class<?> getFieldType(Object object, String fieldName) {
        try {
            return searchFieldRecurse(object.getClass(), fieldName).getType();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JCFGVar wrapObject(Object object) {
        return new JCFGVar(object);
    }

    public Field searchFieldRecurse(Class<?> type, String fieldName) throws NoSuchFieldException {
        try {
            return type.getDeclaredField(fieldName);
        } catch (Exception e) {
            var sup = type.getSuperclass();
            if (sup != null)
                return searchFieldRecurse(sup, fieldName);
            throw e;
        }
    }
}
