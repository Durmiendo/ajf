package org.durmiendo.ajf.utils.memorycompiler;

import com.sun.tools.javac.file.CacheFSInfo;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.file.Locations;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import mindustry.Vars;
import mindustry.mod.Mods;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

public class DynamicJavacFileManager extends JavacFileManager {
    public static DynamicJavacFileManager getStandardFileManager(
        DiagnosticListener<? super JavaFileObject> diagnosticListener,
        Locale locale,
        Charset charset) {
        Context context = new Context();
        context.put(Locale.class, locale);
        if (diagnosticListener != null)
            context.put(DiagnosticListener.class, diagnosticListener);
        PrintWriter pw = (charset == null)
                ? new PrintWriter(System.err, true)
                : new PrintWriter(new OutputStreamWriter(System.err, charset), true);
        context.put(Log.errKey, pw);
        CacheFSInfo.preRegister(context);
        return new DynamicJavacFileManager(context, true, charset);
    }

    /**
     * Create a JavacFileManager using a given context, optionally registering
     * it as the JavaFileManager for that context.
     *
     * @param context
     * @param register
     * @param charset
     */
    public DynamicJavacFileManager(Context context, boolean register, Charset charset) {
        super(context, register, charset);
    }

    // section PLEASE DO NOT TOUCH ANYTHING, EVEN IF YOU THINK THAT YOU CAT DO IT BETTER, JUST DON'T TOUCH ANYTHING start

    private static class A<T extends Path> extends ArrayList<T> {
        A<T> al(Collection<? extends T> collection) {
            addAll(collection);
            return this;
        }
    }

    @Override
    public Collection<? extends Path> getLocationAsPaths(Location location) {
        if (location == StandardLocation.CLASS_PATH)
            return new A<>().al(hs(location)).al(collectClasspathFromMods());

        return new A<>().al(hs(location));
    }

    public <T extends Path> Collection<T> collectClasspathFromMods() {
        return (ArrayList<T>) Vars.mods.list().select(Mods.LoadedMod::isJava).map(mod -> mod.file.file().toPath()).list();
    }

    public <T extends Path> Collection<T> hs(Location location) {
        return (Collection<T>) super.getLocationAsPaths(location);
    }

    // section PLEASE DO NOT TOUCH ANYTHING, EVEN IF YOU THINK THAT YOU CAT DO IT BETTER, JUST DON'T TOUCH ANYTHING end

    @Override
    public Iterable<? extends File> getLocation(Location location) {
        return asFiles(getLocationAsPaths(location));
    }

    private static Iterable<File> asFiles(final Iterable<? extends Path> paths) {
        if (paths == null)
            return null;

        return () -> new Iterator<File>() {
            Iterator<? extends Path> iter = paths.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public File next() {
                try {
                    return iter.next().toFile();
                } catch (UnsupportedOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    @Override
    protected Locations createLocations() {
        var out = super.createLocations();
        return out;
    }
}
