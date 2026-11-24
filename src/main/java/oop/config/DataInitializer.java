package oop.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 首次启动时把 classpath 下的示例数据复制到运行时数据目录。
 * users.dat 不在这里复制，由 UserStore 首次启动生成默认账号。
 */
public final class DataInitializer {

    private static final List<String> SEED_FILES =
            List.of("flights.dat", "orders.dat", "customers.dat");

    private DataInitializer() {
    }

    public static synchronized void ensureSeedFiles(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        for (String name : SEED_FILES) {
            Path target = dataDir.resolve(name);
            if (Files.exists(target)) {
                continue;
            }
            try (InputStream input = DataInitializer.class.getResourceAsStream("/seed/" + name)) {
                if (input == null) {
                    throw new IOException("缺少 classpath:seed/" + name);
                }
                Files.copy(input, target);
            }
        }
    }
}
