package org.durmiendo.ajf.utils.codeconstructor;

import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;

import java.lang.reflect.Modifier;

public class MethodSpec {
    public @Nullable String source;

    public String name;
    public int[] modifiers;

    public Seq<String> argTypes = new Seq<>();
    public Seq<String> argNames = new Seq<>();

    public String code = "";

    public String returnType = "void";

    public MethodSpec(String name, int... modifiers) {
        this.name = name;
        this.modifiers = modifiers;
    }

    public MethodSpec() {
    }

    public MethodSpec setSource(String source) {
        this.source = source;
        return this;
    }

    public MethodSpec addArg(Class<?> type, String name) {
        return addArg(type.getCanonicalName(), name);
    }

    public MethodSpec addArg(String type, String name) {
        argNames.add(name);
        argTypes.add(type);
        return this;
    }

    public MethodSpec returns(Class<?> type) {
        return returns(type.getCanonicalName());
    }

    public MethodSpec returns(String type) {
        returnType = type;
        return this;
    }

    public MethodSpec addCode(String code) {
        this.code += code;
        return this;
    }

    public String getModifiersString() {
        String out = "";

        for (int i = 0; i < modifiers.length; i++) {
            out = (i != 0 ? ", " : "") + Modifier.toString(modifiers[i]);
        }

        return out;
    }

    public String getArgsString() {
        String out = "(";

        for (int i = 0; i < argTypes.size; i++) {
            out += (i != 0 ? ", " : "") + argTypes.get(i) + " " + argNames.get(i);
        }

        out += ")";

        return out;
    }

    public String toString() {
        if (source == null) {
            String out = "";
            out += Strings.format("@ @ @ @ { @ }", getModifiersString(), returnType, name, getArgsString(), code);
            return out;
        } else
            return source;
    }
}
