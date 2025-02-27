package org.durmiendo.ajf.utils.codeconstructor;

import arc.util.Strings;
import mindustry.type.Item;

public class ClassConstructorSpec extends MethodSpec {
    public ClassConstructorSpec(String className, int... modifiers) {
        super(className, modifiers);
    }

    public String toString() {
        String out = "";

        out += Strings.format("@ @ @ { @ }", getModifiersString(), name, getArgsString(), code);

        return out;
    }
}
