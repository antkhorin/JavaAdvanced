package ru.ifmo.ctddev.khorin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Class for implement other classes
 *
 * @author Khorin Anton
 */
public class Implementor implements JarImpler {

    /**
     * mapping typeName of types to string that tapes represent
     */
    private static Map<Class<?>, Map<String, String>> typeMapping;

    /**
     * for any method of superclass and interface of given class return need or not write generic for it
     */
    private static Map<Method, Boolean> generate;

    private static Map<Method, String> returnType;

    /**
     * Creates new instance of Implementor class
     */
    public Implementor() {}

    /**
     * Main method to run this class
     *
     * If number of args is two, create implementation of
     * given class and put it to given path.
     * If number of args is three, create implementation of
     * given class, pack it in .jar file and put it to given path.
     *
     * @param args arguments from command line
     *             "-jar classname path" for implement class and pack it in .jar
     *             "classname path" for implement class
     * @throws ImplerException if args is wrong or if classname is wrong
     * @throws InvalidPathException if path is invalid
     * @see #implement(Class, Path)
     * @see #implementJar(Class, Path)
     * @see ImplerException
     */
    public static void main(String[] args) throws ImplerException, InvalidPathException {
        if (args == null) {
            throw new ImplerException("Wrong args");
        }
        if (args.length == 3 && args[0].equals("-jar") || args.length == 2) {
            try {
                Implementor implementor = new Implementor();
                if (args.length == 2) {
                    implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
                } else {
                    implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                }
            } catch (ClassNotFoundException e) {
                throw new ImplerException("Wrong name of class");
            }
        } else {
            throw new ImplerException("Wrong args");
        }
    }

    /**
     *
     * Method to generate implementation
     *
     * Creates a file that correctly implements or extends interface or class.
     * Output files compiles without errors.
     *
     * @param token class or interface, which is going to be implemented
     * @param root directory where implementation should be placed to
     * @throws ImplerException if arguments is wrong or
     * if class is primitive or private or hasn't non-private constructors or
     * if can't create directory or implementation file in given pass
     * @throws InvalidPathException if path is invalid
     * @see #writeClass(Class, BufferedWriter)
     * @see ImplerException
     */
    public void implement(Class<?> token, Path root) throws ImplerException, InvalidPathException {
        generate = new HashMap<>();
        typeMapping = new HashMap<>();
        returnType = new HashMap<>();
        if (token == null) {
            throw new ImplerException("Class is null");
        }
        if (root == null) {
            throw new ImplerException("Path is null");
        }
        if (token.isMemberClass() && !Modifier.isStatic(token.getModifiers()) || Modifier.isFinal(token.getModifiers()) || token.isPrimitive()) {
            throw new ImplerException("Can't extends from given class");
        }
        try {
            if (token.getPackage() != null) {
                root = root.resolve(token.getPackage().getName().replace(".", File.separator));
            }
            Files.createDirectories(root);
            root = root.resolve(token.getSimpleName() + "Impl.java");
            if (Files.notExists(root)) {
                Files.createFile(root);
            }
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
        try (BufferedWriter out = Files.newBufferedWriter(root, Charset.defaultCharset())) {
            writeClass(token, out);
        } catch (IOException e) {
            throw new ImplerException(e.toString());
        }
    }

    /**
     * Write code of implementation of class in file
     *
     * @param token class for implement
     * @param out writer
     * @throws IOException if an error occurs while writing to the file
     * @see #writeConstructors(Class, BufferedWriter)
     * @see #writeMethods(Class, BufferedWriter)
     */
    private static void writeClass(Class<?> token, BufferedWriter out) throws IOException, ImplerException {
        if (token.getPackage() != null) {
            out.write("package " + token.getPackage().getName() + ";");
            out.newLine();
            out.newLine();
        }
        String s = "public class " + token.getSimpleName() + "Impl " + writeTypes(token.getTypeParameters(), true) +
                (Modifier.isInterface(token.getModifiers()) ? "implements " : "extends ") +
                token.getCanonicalName() + writeTypes(token.getTypeParameters(), false) + " {";
        out.write(s);
        out.newLine();
        out.newLine();
        writeConstructors(token, out);
        writeMethods(token, out);
        out.write("}");
    }

    /**
     * Write code of constructors in file
     *
     * @param token class for implement
     * @param out writer
     * @throws IOException if an error occurs while writing to the file
     * @see #printArguments(Type[])
     */
    private static void writeConstructors(Class<?> token, BufferedWriter out) throws IOException {
        StringBuilder s = new StringBuilder();
        for (Constructor c : token.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(c.getModifiers())) {
                s.delete(0, s.length());
                s.append(Modifier.toString(c.getModifiers() & Modifier.methodModifiers())).append(" ").append(writeTypes(c.getTypeParameters(), true));
                s.append(token.getSimpleName()).append("Impl").append(printArguments(c.getGenericParameterTypes()));
                if (c.getExceptionTypes().length > 0) {
                    String s1 = c.toGenericString();
                    s.append(s1.substring(s1.indexOf(" throws "), s1.length()));
                }
                s.append(" {");
                write(out, s.toString());
                s.setLength(0);
                if (c.getParameterCount() > 0) {
                    s.append("\tsuper(arg0");
                    for (int i = 1; i < c.getParameterCount(); i++) {
                        s.append(", arg").append(i);
                    }
                    s.append(");");
                }
                write(out, s.toString());
                write(out, "}");
                out.newLine();
            }
        }
    }

    /**
     * Write code of methods in file
     *
     * @param token class for implement
     * @param out writer
     * @throws IOException if an error occurs while writing to the file
     * @see #getMethods(Class, Class, Type, Map, boolean)
     * @see #printArguments(Type[])
     */
    private static void writeMethods(Class<?> token, BufferedWriter out) throws IOException , ImplerException{
        Map<String, Method> map = new HashMap<>();
        Map<String, String> mapping = Arrays.asList(token.getTypeParameters()).stream().collect(Collectors.toMap(Type::getTypeName, Type::getTypeName));
        typeMapping.put(token, mapping);
        getMethods(null, token, null, map, true);
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, Method> m : map.entrySet()) {
            boolean generify = generate.get(m.getValue());
            if (Modifier.isAbstract(m.getValue().getModifiers())) {
                s.delete(0, s.length());
                if (Arrays.asList(m.getValue().getDeclaredAnnotations()).stream().anyMatch(annotation -> annotation.annotationType().getSimpleName().equals("Deprecated"))) {
                    write(out, "@Deprecated");
                }
                s.append(Modifier.toString(m.getValue().getModifiers() & ~Modifier.ABSTRACT & Modifier.methodModifiers())).append(" ");
                if (generify) {
                    s.append(writeTypes(m.getValue().getTypeParameters(), true));
                }
                s.append(generify ? parse(m.getValue().getGenericReturnType().getTypeName()) : returnType.get(m.getValue())).append(" ").
                        append(generify ? m.getValue().getName() + printArguments(m.getValue().getGenericParameterTypes()) : m.getKey()).append(" {");
                String str = s.toString();
                if (generify) {
                    Set<String> set = new HashSet<>(typeMapping.get(m.getValue().getDeclaringClass()).values());
                    List<String> types = Arrays.asList(m.getValue().getTypeParameters()).stream().map(Type::getTypeName).collect(Collectors.toList());
                    Set<String> set2 = new HashSet<>(types);
                    for (int i = 0; i < types.size(); i++) {
                        int count = 1;
                        String typeName = types.get(i);
                        String tmp = typeName;
                        boolean b;
                        do {
                            b = false;
                            for (String st : set) {
                                while (st.equals(tmp) || st.contains(" " + tmp + ">") || st.contains("<" + tmp + ">") || st.contains("<" + tmp + ",") ||
                                        st.contains("<" + tmp + "[") || st.contains(" " + tmp + "[") || set2.contains(tmp) && !types.get(i).equals(tmp)) {
                                    tmp = typeName + count++;
                                    b = true;
                                }
                            }
                        } while (b);
                        if (!tmp.equals(typeName)) {
                            str = stringReplace(str, typeName, " (<", "> [,").replaceAll("\\*" + typeName + "\\*", tmp);
                            types.set(i, tmp);
                            set2.add(tmp);
                        }
                    }
                    set.clear();
                    set.addAll(Arrays.asList(m.getValue().getTypeParameters()).stream().map(TypeVariable::getTypeName).collect(Collectors.toList()));
                    for (Map.Entry<String, String> entry : typeMapping.get(m.getValue().getDeclaringClass()).entrySet()) {
                        if (!set.contains(entry.getKey())) {
                            str = stringReplace(str, entry.getKey(), " (<", "> [,");
                        }
                    }
                    for (Map.Entry<String, String> anotherEntry : typeMapping.get(m.getValue().getDeclaringClass()).entrySet()) {
                        if (!set.contains(anotherEntry.getKey())) {
                            str = str.replaceAll("\\*" + anotherEntry.getKey() + "\\*", anotherEntry.getValue());
                        }
                    }
                }
                write(out, str);
                if (!m.getValue().getReturnType().equals(void.class)) {
                    write(out, "\treturn " + (m.getValue().getReturnType().isPrimitive() ? m.getValue().getReturnType().equals(boolean.class) ? "false" : 0 : "null") + ";");
                } else {
                    write(out, "");
                }
                write(out, "}");
                out.newLine();
            }
        }
    }

    /**
     * get array of types and make string with them in java generic format
     *
     * @param types array of types
     * @param bounds need or not to write bounds
     * @return string with types
     */
    private static String writeTypes(TypeVariable<?>[] types, boolean bounds) {
        StringBuilder s = new StringBuilder();
        if (types.length > 0) {
            s.append("<");
            for (int i = 0; i < types.length; i++) {
                s.append(types[i].getName());
                if (bounds && !types[i].getBounds()[0].getTypeName().equals("java.lang.Object")) {
                    s.append(" extends ").append(parse(types[i].getBounds()[0].getTypeName()));
                }
                if (i != types.length - 1) {
                    s.append(", ");
                }
            }
            s.append("> ");
        }
        return s.toString();
    }

    /**
     * Get all methods of given class and put it into given map, make generate map and typeMapping
     *
     * @param underClass class which extend token
     * @param token class for get methods
     * @param type string with real type of generic of token
     * @param map map for collect methods
     * @param generated need or not to write generics for token
     * @see #generate
     * @see #typeMapping
     * @see #genericCheck(Class, Type, boolean)
     */
    private static void getMethods(Class<?> underClass, Class<?> token, Type type, Map<String, Method> map, boolean generated) throws IOException, ImplerException{
        Map<String, String> mapping = new HashMap<>();
        if (type != null && generated && token.getTypeParameters().length > 0) {
            TypeVariable<?>[] types = token.getTypeParameters();
            String s = parse(type.getTypeName());
            List<String> list = split(s.substring(s.indexOf('<') + 1, s.lastIndexOf('>')));
            for (int i = 0; i < types.length; i++) {
                s = list.get(i);
                for (TypeVariable var : underClass.getTypeParameters()) {
                    s = stringReplace(s, var.getTypeName(), "< ", ">, [");
                }
                for (TypeVariable var : underClass.getTypeParameters()) {
                    s = s.replaceAll("\\*" + var.getTypeName() + "\\*", typeMapping.get(underClass).get(var.getTypeName()));
                }
                mapping.put(types[i].getTypeName(), s);
            }
        }
        if (underClass != null) {
            typeMapping.put(token, mapping);
        }
        for (Method m : token.getDeclaredMethods()) {
            if (!Modifier.isPrivate(m.getModifiers())) {
                String s = methodHash(m, generated);
                if (map.containsKey(s)) {
                    Method method = map.get(s);
                    try {
                        if (!returnType.get(m).equals(returnType.get(method))) {
                            Class<?> c1 = Class.forName(returnType.get(method));
                            Class<?> c2 = Class.forName(returnType.get(m));
                            if (c1.isAssignableFrom(c2)) {
                                c1 = c2;
                                map.put(s, m);
                            } else if (!c2.isAssignableFrom(c1)) {
                                throw new ClassNotFoundException();
                            }
                            returnType.put(m, c1.getCanonicalName());
                            returnType.put(method, c1.getCanonicalName());
                        }
                    } catch (ClassNotFoundException e) {
                        throw new ImplerException("Can't extends from given class");
                    }
                    boolean b = generated && generate.get(method);
                    if (b) {
                        TypeVariable[] types1 = m.getTypeParameters();
                        TypeVariable[] types2 = method.getTypeParameters();
                        String bound1 = types1.length > 0 ? Arrays.asList(types1).stream().map(e -> e.getTypeName() + " " + parse(e.getBounds()[0].getTypeName())).reduce((e1, e2) -> e1 + ", " + e2).get() : "";
                        String bound2 = Arrays.asList(types2).stream().map(e -> e.getTypeName() + " " + parse(e.getBounds()[0].getTypeName())).collect(Collectors.joining(", "));
                        for (TypeVariable type1 : types1) {
                            bound1 = stringReplace(bound1, type1.getTypeName(), "< ", ">[, ");
                        }
                        int jj = 1;
                        for (TypeVariable type1 : types1) {
                            bound1 = bound1.replaceAll("\\*" + type1.getTypeName() + "\\*", "T" + jj++);
                        }
                        for (TypeVariable type2 : types2) {
                            bound2 = stringReplace(bound1, type2.getTypeName(), "< ", ">[, ");
                        }
                        jj = 1;
                        for (TypeVariable type2 : types2) {
                            bound2 = bound2.replace("*" + type2.getTypeName() + "*", "T" + jj++);
                        }
                        if (!bound1.equals(bound2)) {
                            b = false;
                        }
                        if (b) {
                            Type[] typeParameters1 = m.getGenericParameterTypes();
                            Type[] typeParameters2 = method.getGenericParameterTypes();
                            l: for (int i = 0; i < typeParameters1.length; i++) {
                                for (int j = 0; j < types1.length; j++) {
                                    b = typeParameters1[i].getTypeName().equals(types1[j].getTypeName()) == typeParameters2[i].getTypeName().equals(types2[j].getTypeName()) &&
                                            typeParameters1[i].getTypeName().equals(types1[j].getTypeName() + "[]") == typeParameters2[i].getTypeName().equals(types2[j].getTypeName() + "[]");
                                    if (!b) {
                                        break l;
                                    }
                                }
                            }
                        }
                    }
                    generate.put(m, b);
                    generate.put(method, b);
                } else {
                    map.put(s, m);
                }
            }
        }
        if (token.getSuperclass() != null) {
            getMethods(token, token.getSuperclass(), token.getGenericSuperclass(), map, genericCheck(token.getSuperclass(), token.getGenericSuperclass(), generated));
        }
        Class<?>[] interfaces = token.getInterfaces();
        Type[] typeInterfaces = token.getGenericInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            getMethods(token, interfaces[i], typeInterfaces[i], map, genericCheck(interfaces[i], typeInterfaces[i], generated));
        }
    }

    /**
     * For given class and his superclass return need or not to generify superclass
     *
     * @param superClass class for check about generify
     * @param superType type of class
     * @return need or not to generify superclass
     */
    private static boolean genericCheck(Class<?> superClass, Type superType, boolean generated) {
        return superClass.getTypeParameters().length == 0 || !superClass.getTypeName().equals(superType.getTypeName()) && generated;
    }

    /** Replace all occurrence of string in stringBuilder and replace it to *string*
     *
     * @param s String for search
     * @param s1 string for replace
     * @param before chars that need to be left at string
     * @param after chars that need to be right at string
     * @return StringBuilder with *string*
     */
    private static String stringReplace(String s, String s1, String before, String after) {
        StringBuilder str = new StringBuilder(s);
        str.insert(0, before.charAt(0));
        str.insert(str.length(), after.charAt(0));
        int i = 0;
        int k;
        while ((k = str.indexOf(s1, i)) >= 0) {
            if (before.contains("" + str.charAt(k - 1)) && after.contains("" + str.charAt(k + s1.length()))) {
                str.insert(k++, '*');
                str.insert(k++ + s1.length(), '*');
            }
            i = k + s1.length();
        }
        return str.toString().substring(1, str.length() - 1);
    }

    /**
     * Write given string with indent and newline using writer
     *
     * @param out writer
     * @param s string for write
     * @throws IOException if an error occurs while writing to the file
     */
    private static void write(BufferedWriter out, String s) throws IOException {
        out.write("\t" + s);
        out.newLine();
    }

    /**
     * Make string with given arguments
     *
     * @param args arguments
     * @return string with given arguments
     */
    private static String printArguments(Type[] args) {
        StringBuilder s = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            s.append(parse(args[i].getTypeName()));
            s.append(" arg");
            s.append(i);
            if (i != args.length - 1) {
                s.append(", ");
            }
        }
        s.append(")");
        return s.toString();
    }

    /**
     * Make from string that returns by {@code Type.getTypeName()} good string for write in file
     *
     * @param s bad string
     * @return good string
     * @see #split(String)
     */
    private static String parse(String s) {
        if (s.contains("<")) {
            StringBuilder b = new StringBuilder();
            boolean bool;
            do {
                bool = false;
                String a = s.substring(0, s.indexOf('<'));
                s = s.substring(s.indexOf('<') + 1, s.length());
                if (a.contains("$")) {
                    if (a.contains(" ")) {
                        b.append(a.substring(0, a.lastIndexOf(' ') + 1));
                        a = a.substring(a.lastIndexOf(' ') + 1, a.length());
                    }
                    a = a.substring(a.lastIndexOf('$') / 2 + 1, a.length()).replace('$', '.');
                }
                b.append(a).append("<");
                int balance = 1;
                int start = 0;
                int end = 0;
                for (int i = start; i < s.length(); i++) {
                    if (s.charAt(i) == '<') {
                        balance++;
                    } else if (s.charAt(i) == '>') {
                        balance--;
                    }
                    if (balance == 0) {
                        end = i;
                        break;
                    }
                }
                List<String> m = split(s.substring(start, end));
                for (int i = 0; i < m.size(); i++) {
                    b.append(parse(m.get(i)));
                    if (i != m.size() - 1) {
                        b.append(", ");
                    }
                }
                b.append(">");
                if (end != s.length() - 1) {
                    if (s.charAt(end + 1) == '[') {
                        b.append("[]");
                        end += 2;
                    }
                    if (end != s.length() - 1) {
                        bool = true;
                        b.append(".");
                        s = s.substring(end + 2, s.length());
                    }
                }
            } while (bool);
            return b.toString();
        } else {
            return s.replace("$", ".");
        }
    }

    /**
     * Split given string by separator ", " in list of generic types
     *
     * @param s string sor split
     * @return list with generic types
     */
    private static List<String> split(String s) {
        List<String> list = new ArrayList<>();
        int j = 0;
        int balance = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '<') {
                balance++;
            } else if (s.charAt(i) == '>') {
                balance--;
            } else if (balance == 0 && s.charAt(i) == ',') {
                list.add(s.substring(j, i));
                j = i + 2;
            }
        }
        list.add(s.substring(j, s.length()));
        return list;
    }

    /**
     * Make string which represent one method
     *
     * @param m method
     * @return string which represent given method
     */
    private static String methodHash(Method m, boolean generated) {
        StringBuilder s = new StringBuilder("," + m.getGenericReturnType().getTypeName() + "," + m.getName());
        s.append(",");
        if (!generated) {
            for (Class<?> c : m.getParameterTypes()) {
                s.append(c.getCanonicalName()).append(",");
            }
        } else {
            for (Type t : m.getGenericParameterTypes()) {
                s.append(parse(t.getTypeName())).append(",");
            }
            String str = s.toString();
            boolean b;
            do {
                b = false;
                for (TypeVariable var : m.getTypeParameters()) {
                    if (str.contains("," + var + ",") || str.contains("," + var + "[")) {
                        b = true;
                        str = stringReplace(str, var.getTypeName(), ",", ",[").replaceAll("\\*" + var + "\\*", parse(var.getBounds()[0].getTypeName()));
                    }
                }
            } while(b);
            for (String string : typeMapping.get(m.getDeclaringClass()).keySet()) {
                str = stringReplace(str, string, ",", ",[");
            }
            for (String string : typeMapping.get(m.getDeclaringClass()).keySet()) {
                str = str.replaceAll("\\*" + string + "\\*", typeMapping.get(m.getDeclaringClass()).get(string));
            }
            s = new StringBuilder(str);
            int balance = 0;
            int j = 0;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '<') {
                    if (balance++ == 0) {
                        j = i;
                    }
                } else if (s.charAt(i) == '>') {
                    if (--balance == 0) {
                        s = s.delete(j, i + 1);
                        i = j - 1;
                    }
                }
            }
        }
        String s1 = s.toString();
        returnType.put(m, s1.substring(1, s1.indexOf(",", 1)));
        s1 = s1.substring(s1.indexOf(",", 1) + 1, s1.length());
        s1 = s1.replaceFirst(",", "(");
        int i = 0;
        while (s1.contains(",")) {
            s1 = s1.replaceFirst(",", " arg" + i++ + "**");
        }
        s1 = s1.replaceAll("\\*\\*", ",");
        s1 = s1.substring(0, s1.length() - 1) + ")";
        generate.put(m, generated);
        return s1;
    }

    /**
     * Method to generate implementation and pack it into .jar file
     *
     * Takes given class. Makes implementation and tries to compile it.
     * If the compilation finish without errors, tries to make JAR file of the compiled
     * byte-code
     *
     * @param token class for implement
     * @param jarPath directory where .jar should be placed to
     * @throws ImplerException if arguments is wrong or
     * if compilation finish with error or
     * if can't create .jar file in given directory
     * @throws InvalidPathException if path is invalid
     * @see #implement(Class, Path)
     * @see ImplerException
     */
    public void implementJar(Class<?> token, Path jarPath) throws ImplerException, InvalidPathException {
        if (jarPath == null) {
            throw new ImplerException("Path is null");
        }
        Path root = jarPath.getParent();
        if (root == null) {
            root = Paths.get("");
        }
        implement(token, root);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage().getName().replace(".", File.separator) + File.separator);
        }
        root = root.resolve(token.getSimpleName() + "Impl.java");
        if (compiler.run(null, null, null, root.toString()) != 0) {
            throw new ImplerException("Can't compile class");
        }
        try {
            root = root.getParent();
            if (root == null) {
                root = Paths.get("");
            }
            root = root.resolve(token.getSimpleName() + "Impl.class");
            try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath), new Manifest()); InputStream reader = Files.newInputStream(root)) {
                String s = "";
                if (token.getPackage() != null) {
                    s += token.getPackage().getName().replace(".", "/") + "/";
                }
                s += token.getSimpleName() + "Impl.class";
                out.putNextEntry(new JarEntry(s));
                byte[] buf = new byte[1024];
                int count;
                while ((count = reader.read(buf)) >= 0) {
                    out.write(buf, 0, count);
                }
                Files.delete(root);
            }
        } catch (IOException e) {
            throw new ImplerException("Can't create .jar file");
        }
    }
}

