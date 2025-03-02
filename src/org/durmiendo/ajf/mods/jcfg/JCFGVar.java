package org.durmiendo.ajf.mods.jcfg;

import arc.util.Strings;

public class JCFGVar {
    public Object value;

    public JCFGVar(Object value) {
        this.value = value;
    }

    public Object get(Class<?> targetType) {
        try {
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt((String) value);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat((String) value);
            } else if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte((String) value);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong((String) value);
            } else if (targetType == char.class || targetType == Character.class) {
                return (char) Short.parseShort((String) value);
            } else if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort((String) value);
            }
        } catch (NumberFormatException exception) {
            throw new RuntimeException(new ClassCastException(Strings.format("Number @ cannot be parsed as @ or casted to @", value, targetType.getSimpleName(), targetType.getCanonicalName())));
        }
        return value;
    }
}
