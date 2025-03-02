package org.durmiendo.ajf.mods.jcfg;

public interface JCFGUtils  {
    String addGlobalObject(String name, JCFGVar var);

    @SuppressWarnings("unused")
    <T> T getGlobalObject(String name, Class<?> targetType);

    @SuppressWarnings("unused")
    Class<?> getFieldType(Object object, String fieldName);

    JCFGVar wrapObject(Object object);
}
