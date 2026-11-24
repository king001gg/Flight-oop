package oop.config;

import oop.AuthService;
import oop.CustomerStore;
import oop.FlightSystem;
import oop.UserStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 数据目录与业务服务的装配。
 */
@Configuration
public class DataConfig {

    private static final Logger log = LoggerFactory.getLogger(DataConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FlightSystem flightSystem(@Value("${app.data-dir:./data}") String dataDir)
            throws IOException {
        Path dir = resolveDataDir(dataDir);
        DataInitializer.ensureSeedFiles(dir);
        FlightSystem system = new FlightSystem(dir);
        FlightSystem.LoadReport report = system.load();
        if (!report.getWarnings().isEmpty()) {
            log.warn("航班/订单数据加载提示：{}", report.getWarnings());
        }
        log.info("数据目录：{}，航班 {} 条，订单 {} 条",
                dir, report.getFlightCount(), report.getOrderCount());
        return system;
    }

    @Bean
    public CustomerStore customerStore(@Value("${app.data-dir:./data}") String dataDir)
            throws IOException {
        Path dir = resolveDataDir(dataDir);
        DataInitializer.ensureSeedFiles(dir);
        CustomerStore store = new CustomerStore(dir);
        List<String> warnings = store.load();
        if (!warnings.isEmpty()) {
            log.warn("客户数据加载提示：{}", warnings);
        }
        log.info("客户数据：{} 条", store.getAll().size());
        return store;
    }

    @Bean
    public UserStore userStore(@Value("${app.data-dir:./data}") String dataDir,
                               PasswordEncoder encoder)
            throws IOException, oop.FlightSystemException {
        Path dir = resolveDataDir(dataDir);
        DataInitializer.ensureSeedFiles(dir);
        UserStore store = new UserStore(dir, encoder);
        List<String> warnings = store.load();
        if (!warnings.isEmpty()) {
            log.warn("用户数据加载提示：{}", warnings);
        }
        if (store.isEmpty()) {
            store.seedDefaults();
            log.info("已创建默认账号：admin / admin123、zhangwei / 123456");
        }
        return store;
    }

    @Bean
    public AuthService authService(UserStore userStore, CustomerStore customerStore,
                                   PasswordEncoder encoder) {
        return new AuthService(userStore, customerStore, encoder);
    }

    private static Path resolveDataDir(String dataDir) {
        return Paths.get(dataDir).toAbsolutePath().normalize();
    }
}
