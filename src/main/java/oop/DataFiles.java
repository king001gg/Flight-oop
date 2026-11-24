package oop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 文本数据文件的公共读写工具：UTF-8，临时文件 + 替换。
 */
public final class DataFiles {

    private DataFiles() {
    }

    public static List<String> readLines(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        if (!Files.exists(file)) {
            return lines;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    public static void writeLines(Path file, List<String> lines) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = absolute.resolveSibling(absolute.getFileName().toString() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        Files.move(tmp, absolute, StandardCopyOption.REPLACE_EXISTING);
    }
}
