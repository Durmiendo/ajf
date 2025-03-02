package org.durmiendo.ajf.utils.memorycompiler;

import arc.struct.ObjectMap;

import javax.tools.*;
import java.io.IOException;
import java.util.Set;

public class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private ObjectMap<String, InMemoryOutputJavaFile> compiledClasses;
    private ClassLoader loader;

    public InMemoryFileManager(StandardJavaFileManager standardManager) {
        super(standardManager);
        this.compiledClasses = new ObjectMap<>();
        this.loader = new InMemoryClassLoader(this.getClass().getClassLoader(), this);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        if (packageName.equals("jcfg.customclasses"))
            return compiledClasses.values().toSeq().map(obj -> obj);

        return super.list(location, packageName, kinds, recurse);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location,
                                               String className, JavaFileObject.Kind kind, FileObject sibling) {
        InMemoryOutputJavaFile classAsBytes = new InMemoryOutputJavaFile(className, kind);
        compiledClasses.put(className, classAsBytes);
        return classAsBytes;
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return loader;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof InMemoryJavaFileObject fileObject)
            return fileObject.inferBinaryName();
        return super.inferBinaryName(location, file);
    }

    public ObjectMap<String, InMemoryOutputJavaFile> getBytesMap() {
        return compiledClasses;
    }
}
