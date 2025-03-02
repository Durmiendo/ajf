package org.durmiendo.ajf.utils.memorycompiler;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/** Виртуальный файл в который можно записать данные. */
public class InMemoryOutputJavaFile extends InMemoryJavaFileObject {
    public final ByteArrayOutputStream bos =
            new ByteArrayOutputStream();

    public String name;

    public InMemoryOutputJavaFile(String name, Kind kind) {
        super(name, kind);
    }

    public byte[] getBytes() {
        return bos.toByteArray();
    }

    @Override
    public OutputStream openOutputStream() {
        return bos;
    }
}
