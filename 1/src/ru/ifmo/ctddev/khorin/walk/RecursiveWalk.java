package ru.ifmo.ctddev.khorin.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RecursiveWalk {
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Неверные аргументы");
        } else {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8); PrintWriter writer = new PrintWriter(args[1])) {
                String s;
                SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                    private MessageDigest messageDigest = MessageDigest.getInstance("MD5");

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                        try (InputStream fileInputStream = Files.newInputStream(file)) {
                            byte[] bytes = new byte[1024];
                            messageDigest.reset();
                            int n;
                            while ((n = fileInputStream.read(bytes)) >= 0) {
                                messageDigest.update(bytes, 0, n);
                            }
                            bytes = messageDigest.digest();
                            StringBuilder sb = new StringBuilder();
                            for (byte b : bytes) {
                                sb.append(String.format("%02X", b));
                            }
                            writer.write(sb.toString() + " " + file.toString() + "\n");
                            return FileVisitResult.CONTINUE;
                        } catch (Exception e) {
                            writer.write("00000000000000000000000000000000 " + file.toString() + "\n");
                            return FileVisitResult.CONTINUE;
                        }
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        writer.write("00000000000000000000000000000000 " + file.toString() + "\n");
                        return FileVisitResult.CONTINUE;
                    }
                };
                while ((s = reader.readLine()) != null) {
                    Path path = Paths.get(s);
                    Files.walkFileTree(path, visitor);
                }
            } catch (NoSuchFileException e) {
                System.out.println("Файл " + e.getFile() + " не найден");
            } catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
            } catch (InvalidPathException e) {
                System.out.println("Неверный путь к файлу " + e.getInput());
            } catch (AccessDeniedException e) {
                System.out.println("Отказано в доступе к файлу " + e.getFile());
            } catch (IOException e) {
                System.out.println("Something went wrong");
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Алгоритм не найден");
            }
        }
    }
}
