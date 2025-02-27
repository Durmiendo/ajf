package org.durmiendo.ajf.utils.codeconstructor;

import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;

import java.lang.reflect.Modifier;

public class ClassSpec {
    public String packagee, name;
    public int[] modifiers;

    public Seq<String> imports = new Seq<>();

    public @Nullable String superClass;

    public Seq<MethodSpec> methods = new Seq<>();

    public ClassSpec(String packagee, String name, int... modifiers) {
        this.packagee = packagee;
        this.name = name;
        this.modifiers = modifiers;
    }

    public ClassSpec extend(Class<?> clazz) {
        return extend(clazz.getCanonicalName());
    }

    public ClassSpec extend(String className) {
        superClass = className;
        return this;
    }

    public ClassSpec addImport(Class<?> clazz) {
        return addImport(clazz.getCanonicalName());
    }

    public ClassSpec addImport(String clazz) {
        imports.add(clazz);
        return this;
    }

    public ClassSpec addMethod(MethodSpec methodSpec) {
        methods.add(methodSpec);
        return this;
    }

    public String getImportsString() {
        String out = "";

        for (String anImport : imports)
            out += Strings.format("import @;\n", anImport);

        return out;
    }

    public String getModifiersString() {
        String out = "";

        for (int i = 0; i < modifiers.length; i++) {
            out = (i != 0 ? ", " : "") + Modifier.toString(modifiers[i]);
        }

        return out;
    }

    public String getExtendsString() {
        return superClass != null ? Strings.format(" extends @", superClass) : "";
    }

    public String toString() {
        String out = "";

        out += Strings.format("package @;\n", packagee);
        out += getImportsString();
        out += Strings.format("@ class @", getModifiersString(), name);
        out += getExtendsString();
        out += "{";
        for (MethodSpec method : methods) {
            out += method.toString();
        }
        out += "}";

        return out;
    }
}
