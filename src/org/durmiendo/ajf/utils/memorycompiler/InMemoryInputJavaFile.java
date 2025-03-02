package org.durmiendo.ajf.utils.memorycompiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Виртуальный файл, из которого можно прочитать значения. */
public class InMemoryInputJavaFile extends InMemoryJavaFileObject {
    public final String sourceCode;

    public InMemoryInputJavaFile(String name, String sourceCode) {
        super(name, Kind.SOURCE);
        this.sourceCode = sourceCode;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return sourceCode;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(sourceCode.getBytes());
    }
}
