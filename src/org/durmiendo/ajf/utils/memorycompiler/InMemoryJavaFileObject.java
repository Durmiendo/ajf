package org.durmiendo.ajf.utils.memorycompiler;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/** Абстрактный виртуальный файл. */
public abstract class InMemoryJavaFileObject extends SimpleJavaFileObject {
    public String name;

    public InMemoryJavaFileObject(String name, Kind kind) {
        super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
        this.name = name;
    }

    public String inferBinaryName() {
        return name;
    }
}
