package org.durmiendo.ajf.utils.memorycompiler;

import arc.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Виртуальный файл в который можно записать данные. */
public class InMemoryOutputJavaFile extends InMemoryJavaFileObject {
    InMemoryFileManager manager;

    public final ByteArrayOutputStream bos =
            new ByteArrayOutputStream(){
                @Override
                public void close() throws IOException {
                    super.close();
                    Log.info(name);
                    manager.getInMemoryClassLoader().acceptNewClass(name, getBytes());
                }
            };

    public InMemoryOutputJavaFile(String name, Kind kind, InMemoryFileManager manager) {
        super(name, kind);
        this.manager = manager;
    }

    public byte[] getBytes() {
        return bos.toByteArray();
    }

    @Override
    public OutputStream openOutputStream() {
        return bos;
    }
}
