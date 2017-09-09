package ru.ifmo.ctddev.khorin.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Implementor implements Impler {

    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null) {
            throw new ImplerException("Class is null");
        }
        if (root == null) {
            throw new ImplerException("Path is null");
        }
        if (Modifier.isFinal(token.getModifiers()) || token.isPrimitive()) {
            throw new ImplerException("Can't extends from given class");
        }
        try {
            root = root.resolve(token.getPackage().getName().replace(".", File.separator));
            Files.createDirectories(root);
            root = root.resolve(token.getSimpleName() + "Impl.java");
            Files.createFile(root);
        } catch (IOException e) {
            throw new ImplerException("Can't create a file");
        }
        boolean b = false;
        for (Constructor c : token.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(c.getModifiers())) {
                b = true;
                break;
            }
        }
        if (!b && !token.isInterface()) {
            throw new ImplerException("All constructors are private");
        }
        try (BufferedWriter out = Files.newBufferedWriter(root)) {
            writeClass(token, out);
        } catch (IOException e) {
            throw new ImplerException("Can't write to file");
        }
    }

    private static void writeClass(Class<?> token, BufferedWriter out) throws IOException {
        out.write("package " + token.getPackage().getName() + ";");
        out.newLine();
        out.newLine();
        out.write("public class " + token.getSimpleName() + "Impl " + (Modifier.isInterface(token.getModifiers()) ? "implements " : "extends ") + token.getSimpleName() + " {");
        out.newLine();
        out.newLine();
        writeConstructors(token, out);
        writeMethods(token, out);
        out.write("}");
    }

    private static void writeConstructors(Class<?> token, BufferedWriter out) throws IOException {
        StringBuilder s1 = new StringBuilder("");
        StringBuilder s2 = new StringBuilder("");
        Class[] exceptions;
        for (Constructor c : token.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(c.getModifiers())) {
                s1.delete(0, s1.length());
                s2.delete(0, s2.length());
                s1.append(Modifier.toString(c.getModifiers() & Modifier.methodModifiers()));
                s1.append(" ");
                s1.append(token.getSimpleName());
                s1.append("Impl");
                s1.append(printArguments(c.getParameterTypes()));
                if (c.getExceptionTypes().length > 0) {
                    exceptions = c.getExceptionTypes();
                    s1.append("throws ");
                    for (int i = 0; i < exceptions.length; i++) {
                        s1.append(exceptions[i].getName());
                        if (i != exceptions.length - 1) {
                            s1.append(", ");
                        }
                    }
                }
                s1.append(" {");
                if (c.getParameterCount() > 0) {
                    s2.append("\tsuper(arg0");
                    for (int i = 1; i < c.getParameterCount(); i++) {
                        s2.append(", arg");
                        s2.append(i);
                    }
                    s2.append(");");
                }
                write(out, s1.toString());
                write(out, s2.toString());
                write(out, "}");
                out.newLine();
            }
        }
    }

    private static void writeMethods(Class<?> token, BufferedWriter out) throws IOException {
        Map<String, Method> map = new HashMap<>();
        map = getMethods(token, map);
        StringBuilder s = new StringBuilder("");
        for (Method m : map.values()) {
            if (Modifier.isAbstract(m.getModifiers())) {
                s.delete(0, s.length());
                s.append(Modifier.toString(m.getModifiers() & ~Modifier.ABSTRACT & Modifier.methodModifiers()));
                s.append(" ");
                s.append(m.getReturnType().getCanonicalName());
                s.append(" ");
                s.append(m.getName());
                s.append(printArguments(m.getParameterTypes()));
                s.append(" {");
                write(out, s.toString());
                if (!m.getReturnType().equals(void.class)) {
                    write(out, "\treturn " + (m.getReturnType().isPrimitive() ? m.getReturnType().equals(boolean.class) ? "false" : 0 : "null") + ";");
                } else {
                    write(out, "");
                }
                write(out, "}");
                out.newLine();
            }
        }
    }

    private static Map<String, Method> getMethods(Class<?> token, Map<String, Method> map) {
        for (Method m : token.getDeclaredMethods()) {
            if (!Modifier.isPrivate(m.getModifiers())) {
                map.putIfAbsent(methodHash(m), m);
            }
        }
        if (token.getSuperclass() != null) {
            getMethods(token.getSuperclass(), map);
        }
        for (Class c : token.getInterfaces()) {
            getMethods(c, map);
        }
        return map;
    }

    private static void write(BufferedWriter out, String s) throws IOException{
        out.write("\t" + s);
        out.newLine();
    }

    private static String printArguments(Class[] args) {
        StringBuilder s = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            s.append(args[i].getCanonicalName());
            s.append(" arg");
            s.append(i);
            if (i != args.length - 1) {
                s.append(", ");
            }
        }
        s.append(")");
        return s.toString();
    }

    private static String methodHash(Method m) {
        StringBuilder s = new StringBuilder(m.getName());
        for (Class c : m.getParameterTypes()) {
            s.append(c.getName());
        }
        return s.toString();
    }
}