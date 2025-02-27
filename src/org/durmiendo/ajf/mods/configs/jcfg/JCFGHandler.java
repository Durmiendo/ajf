package org.durmiendo.ajf.mods.configs.jcfg;

import arc.files.Fi;
import arc.func.Func;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Strings;
import org.antlr.v4.runtime.tree.ParseTree;
import org.durmiendo.ajf.utils.codeconstructor.ClassConstructorSpec;
import org.durmiendo.ajf.utils.codeconstructor.ClassSpec;
import org.durmiendo.ajf.utils.codeconstructor.MethodSpec;
import org.durmiendo.ajf.utils.memorycompiler.InMemoryFileManager;
import org.durmiendo.ajf.utils.memorycompiler.JavaSourceFromString;
import org.durmiendo.mods.parser.jcfg.JCFG;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JCFGHandler {
    public static ObjectMap<String, Object> globalObjectsList = new ObjectMap<>();

    public static String addGlobalObject(String name, Object object) {
        globalObjectsList.put(name, object);
        return name;
    }

    @SuppressWarnings("unused")
    public static <T> T getGlobalObject(String name) {
        return (T) globalObjectsList.get(name);
    }

    public Fi file;
    public Seq<Object> objects = new Seq<>();
    public StringMap aliases = new StringMap();

    public JCFGHandler() {

    }

    public Seq<Object> handleFile(JCFG.FileContext fileContext, Fi file) {
        this.file = file;

        fileContext.macro().forEach(this::handleMacro);
        fileContext.object().forEach(this::handleObject);

        var out = objects.copy();
        objects.clear();
        aliases.clear();
        this.file = null;
        return out;
    }

    public void handleMacro(JCFG.MacroContext macroContext) {
        var names = macroContext.Name().stream().map(ParseTree::getText).collect(Collectors.toList());

        String name = names.get(0);

        if (name.equals("import")) {
            String anImport = names.get(1);
            aliases.put(anImport.substring(anImport.lastIndexOf('.')), anImport);
        }
    }

    public Object handleObject(org.durmiendo.mods.parser.jcfg.JCFG.ObjectContext ctx) {
        try {
            org.durmiendo.mods.parser.jcfg.JCFG.ConstructorContext constructorContext = ctx.constructor(0);
            String name = constructorContext.Name(0).getText();

            var size = constructorContext.value().size();
            Class<?>[] classesArgs = new Class[size];
            Object[] objectsArgs = new Object[size];
            for (int i = 0; i < size; i++) {
                String className = constructorContext.Name(1 + i).getText();
                classesArgs[i] = Class.forName(className);

                org.durmiendo.mods.parser.jcfg.JCFG.ValueContext valueContext = constructorContext.value(i);

                objectsArgs[i] = parseValue(valueContext);
            }

            String customClassName = "CustomObject" + file.nameWithoutExtension() + ctx.hashCode();

            var constructorSpec = new ClassConstructorSpec(customClassName, Modifier.PUBLIC);
            String customConstructorCode = "super(";
            for (int i = 0; i < classesArgs.length; i++) {
                constructorSpec.addArg(classesArgs[i], "var" + i);
                customConstructorCode += (i != 0 ? ", " : "") + ("var" + i);
            }
            customConstructorCode += ");";
            constructorSpec.addCode(customConstructorCode);

            /*ctx.setValue().stream()
                    .map(setValue -> Strings.format("this.@ = @.getGlobalObject(\"@\");",
                            setValue.Name().getText(), getClass().getCanonicalName(), parseAndStoreValue(setValue.value())))
                    .forEach(constructorSpec::addCode);*/

            var classSpec = new ClassSpec("jcfg.customclasses", customClassName, Modifier.PUBLIC)
                    .addMethod(constructorSpec).extend(name);

            aliases.values().forEach(classSpec::addImport);

            ctx.function().stream().map(func -> new MethodSpec().setSource(func.code().getText().replaceAll("\\\\>", ">")))
                    .forEach(classSpec::addMethod);

            String sourceCode = classSpec.toString();
            Log.info(sourceCode);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            InMemoryFileManager manager = new InMemoryFileManager(compiler.getStandardFileManager(null, null, null));

            List<JavaFileObject> sourceFiles =
                    Collections.singletonList(new JavaSourceFromString(customClassName, sourceCode));

            JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagnostics, null, null, sourceFiles);

            boolean result = task.call();
            Log.info("Compiled @", result);
            if (!result)
                diagnostics.getDiagnostics().forEach(d -> Log.err(d.toString()));

            ClassLoader classLoader = manager.getClassLoader(null);
            Class<?> customClazz = classLoader.loadClass("jcfg.customclasses." + customClassName);

            var out = customClazz.getDeclaredConstructor(classesArgs).newInstance(objectsArgs);

            objects.add(out);
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String parseAndStoreValue(org.durmiendo.mods.parser.jcfg.JCFG.ValueContext valueContext) {
        Object out = parseValue(valueContext);
        return addGlobalObject("anonymousGlobalVar" + file.nameWithoutExtension() + valueContext.hashCode(), out);
    }

    public Object parseValue(org.durmiendo.mods.parser.jcfg.JCFG.ValueContext valueContext) {
        Log.info("Parsed value from @", valueContext.children.get(0));
        if (valueContext.Number() != null)
            return Strings.parseFloat(valueContext.Number().getText());
        else if (valueContext.object() != null)
            return handleObject(valueContext.object());
        else if (valueContext.StringD() != null)
            return valueContext.StringD().getText().replaceAll("\"", "");
        else if (valueContext.StringS() != null)
            return valueContext.StringS().getText().replaceAll("'", "");
        // TODO Name as var parsing
        else if (valueContext.Name() != null) {
            String name = valueContext.Name().getText();

            if (aliases.containsKey(name))
                return aliases.get(name);
            else
                return null;
        }

        return null;
    }
}
