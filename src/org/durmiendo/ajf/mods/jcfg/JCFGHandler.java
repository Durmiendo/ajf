package org.durmiendo.ajf.mods.jcfg;

import arc.files.Fi;
import arc.struct.LongMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Strings;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import mindustry.Vars;
import org.antlr.v4.runtime.tree.ParseTree;
import org.durmiendo.ajf.utils.clazz.ClassFinder;
import org.durmiendo.ajf.utils.memorycompiler.InMemoryFileManager;
import org.durmiendo.ajf.utils.memorycompiler.InMemoryInputJavaFile;
import org.durmiendo.ajf.utils.memorycompiler.DynamicJavacFileManager;
import org.durmiendo.mods.parser.jcfg.JCFG;

import javax.lang.model.element.Modifier;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JCFGHandler {
    public static final LongMap<JCFGHandler> registeredHandlers = new LongMap<>();
    public static long ids = 0;

    public long id;

    public String staticAccessString = "@.registeredHandlers.get(@)";
    public String utilsAccessString = "@.utils";

    public Fi file;
    public Seq<Object> objects = new Seq<>();
    public ObjectMap<String, Class<?>> imports = new ObjectMap<>();
    public int classId;

    public JCFGUtils utils;

    public JavaCompiler compiler;
    public DiagnosticCollector<JavaFileObject> diagnostics;
    public InMemoryFileManager fileManager;

    // section debug start

    public boolean writeGeneratedSourcesToFiles = false;
    public Fi generatedSourcesDir = Vars.dataDirectory.child("AJFGeneratedSources");

    // section debug end

    public JCFGHandler(JCFGUtils utils) {
        this.utils = utils;

        registeredHandlers.put((id = ids++), this);

        staticAccessString = Strings.format(staticAccessString, getClass().getCanonicalName(), id);
        utilsAccessString = Strings.format(utilsAccessString, staticAccessString);
    }

    public Seq<Object> handleFile(JCFG.FileContext fileContext, Fi file) {
        this.file = file;
        classId = 0;

        fileContext.macro().forEach(this::handleMacro);
        fileContext.object().forEach(this::handleObject);

        var out = objects.copy();
        objects.clear();
        imports.clear();
        this.file = null;
        return out;
    }

    public void handleMacro(JCFG.MacroContext macroContext) {
        var names = macroContext.Name().stream().map(ParseTree::getText).toList();

        String name = names.get(0);

        if (name.equals("import")) {
            try {
                String anImport = names.get(1);
                imports.put(anImport.substring(anImport.lastIndexOf('.')+1), ClassFinder.forName(anImport, null));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        compiler = ToolProvider.getSystemJavaCompiler();
        diagnostics = new DiagnosticCollector<>();
        fileManager = new InMemoryFileManager(DynamicJavacFileManager.getStandardFileManager(null, null, null));
    }

    public Object handleObject(JCFG.ObjectContext ctx) {
        try {
            JCFG.ConstructorContext constructorContext = ctx.constructor(0);
            String superClassName = constructorContext.Name(0).getText();
            Class<?> superClass = ClassFinder.forName(superClassName, imports);

            var size = constructorContext.value().size();
            Class<?>[] classesArgs = new Class[size];
            Object[] objectsArgs = new Object[size];
            for (int i = 0; i < size; i++) {
                String className = constructorContext.Name(1 + i).getText();
                classesArgs[i] = ClassFinder.forName(className, imports);

                JCFG.ValueContext valueContext = constructorContext.value(i);

                objectsArgs[i] = parseValue(valueContext);
                if(objectsArgs[i] instanceof JCFGVar var)
                    objectsArgs[i] = var.get(classesArgs[i]);
            }

            String customClassName = "Object" + file.nameWithoutExtension() + classId++;

            var constructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
            String customConstructorCode = "super(";
            for (int i = 0; i < classesArgs.length; i++) {
                constructorBuilder.addParameter(classesArgs[i], "var" + i);
                customConstructorCode += (i != 0 ? ", " : "") + ("var" + i);
            }
            customConstructorCode += ");";
            constructorBuilder.addStatement(customConstructorCode);

            ctx.setValue().stream()
                    .map(setValue -> Strings.format("this.@ = @.getGlobalObject(\"@\", @.getFieldType(this, \"@\"));",
                            setValue.Name().getText(), utilsAccessString, parseAndStoreValue(setValue.value()), utilsAccessString, setValue.Name().getText()))
                    .forEach(constructorBuilder::addStatement);

            var typeBuilder = TypeSpec.classBuilder(customClassName)
                    .addMethod(constructorBuilder.build())
                    .superclass(superClass)
                    .addModifiers(Modifier.PUBLIC);

            ctx.func().forEach(func -> typeBuilder.addMethod(handleFunc(func)));

            var javaFileBuilder = JavaFile.builder("jcfg.customclasses", typeBuilder.build()).indent("    ");

            var out = compileCode(javaFileBuilder.build()).getDeclaredConstructor(classesArgs).newInstance(objectsArgs);

            objects.add(out);
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MethodSpec handleFunc(JCFG.FuncContext funcContext) {
        try {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(funcContext.funcName().getText());

            String retTypeName = funcContext.retType().getText();
            if (!retTypeName.equals("void"))
                methodBuilder.returns(ClassFinder.forName(retTypeName, imports));

            var names = funcContext.Name();
            int size = names.size();
            for (int i = 0; i < size; i+=2)
                methodBuilder.addParameter(ClassFinder.forName(names.get(i).getText(), imports), names.get(i+1).getText());

            methodBuilder.addCode(funcContext.Code().getText().replaceAll("\\\\>", ">"));

            if (funcContext.override() != null)
                methodBuilder.addAnnotation(Override.class);

            methodBuilder.addModifiers(funcContext.modifier().stream().map(modifierContext -> {
                String text = modifierContext.getText();
                return switch (text) {
                    case "public" -> Modifier.PUBLIC;
                    case "private" -> Modifier.PRIVATE;
                    case "protected" -> Modifier.PROTECTED;
                    case "final" -> Modifier.FINAL;
                    case "default" -> Modifier.DEFAULT;
                    case "static" -> Modifier.STATIC;
                    default -> throw new IllegalStateException("Unexpected value: " + text);
                };

            }).toList());

            return methodBuilder.build();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Class<?> compileCode(JavaFile sourceFile) {
        try {
            String sourceCode = injectImports(sourceFile, imports.values().toSeq().map(Class::getCanonicalName));
            //String sourceCode = sourceFile.toString();
            String canonicalName = sourceFile.packageName + "." + sourceFile.typeSpec.name;
            String sourceFileName = sourceFile.packageName.replaceAll("\\.", "/") + "/" + sourceFile.typeSpec.name + ".java";

            if (writeGeneratedSourcesToFiles)
                generatedSourcesDir.child(sourceFileName).writeString(sourceCode, false);

            List<JavaFileObject> sourceFiles = Collections.singletonList(new InMemoryInputJavaFile(canonicalName, sourceCode));

            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, sourceFiles);

            boolean result = task.call();
            if (!result) {
                StringBuilder error = new StringBuilder();
                diagnostics.getDiagnostics().forEach(d -> error.append(d.toString()));
                System.err.print(error);
                throw new CompilationException(Strings.format("@ compilation error (see std::err output)", canonicalName));
            }

            ClassLoader classLoader = fileManager.getClassLoader(null);

            return classLoader.loadClass(canonicalName);
        } catch (CompilationException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String injectImports(JavaFile javaFile, Seq<String> imports) {
        String rawSource = javaFile.toString();
        List<String> result = new ArrayList<>();
        for (String s : rawSource.split("\n", -1)) {
            result.add(s);
            if (s.startsWith("package ")) {
                result.add("");
                for (String i : imports) {
                    result.add("import " + i + ";");
                }
            }
        }
        return String.join("\n", result);
    }

    public String parseAndStoreValue(JCFG.ValueContext valueContext) {
        JCFGVar out = parseValue(valueContext);
        return utils.addGlobalObject("anonymousGlobalVar" + file.nameWithoutExtension() + valueContext.hashCode(), out);
    }

    public JCFGVar parseValue(JCFG.ValueContext valueContext) {
        if (valueContext.Number() != null)
            return utils.wrapObject(valueContext.Number().getText());
        else if (valueContext.object() != null)
            return utils.wrapObject(handleObject(valueContext.object()));
        else if (valueContext.StringD() != null)
            return utils.wrapObject(valueContext.StringD().getText().replaceAll("\"", ""));
        else if (valueContext.StringS() != null)
            return utils.wrapObject(valueContext.StringS().getText().replaceAll("'", ""));
        // TODO Name as var parsing
        else if (valueContext.Name() != null) {
            String name = valueContext.Name().getText();

            if (imports.containsKey(name))
                return utils.wrapObject(imports.get(name));
            else
                return utils.wrapObject(null);
        }

        return null;
    }
}
