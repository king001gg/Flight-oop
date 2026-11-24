package oop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * customers.dat 的加载、保存与 CRUD。
 */
public class CustomerStore {

    private final Path file;
    private final List<Customer> customers = new ArrayList<>();
    private final Map<String, Customer> byId = new HashMap<>();

    public CustomerStore(String dataDir) {
        this(Paths.get(dataDir));
    }

    public CustomerStore(Path dataDir) {
        this.file = dataDir.toAbsolutePath().normalize().resolve("customers.dat");
    }

    public synchronized List<String> load() {
        customers.clear();
        byId.clear();
        List<String> warnings = new ArrayList<>();
        if (!Files.exists(file)) {
            warnings.add("未找到 customers.dat");
            return warnings;
        }
        List<String> lines;
        try {
            lines = DataFiles.readLines(file);
        } catch (IOException e) {
            warnings.add("读取 customers.dat 失败：" + e.getMessage());
            return warnings;
        }
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                Customer customer = Customer.fromDataLine(line);
                if (byId.containsKey(customer.getCustomerId())) {
                    warnings.add("customers.dat 第 " + lineNo + " 行：证件号 "
                            + customer.getCustomerId() + " 重复，已忽略");
                    continue;
                }
                Customer.validate(customer);
                customers.add(customer);
                byId.put(customer.getCustomerId(), customer);
            } catch (RuntimeException e) {
                warnings.add("customers.dat 第 " + lineNo + " 行：" + e.getMessage());
            }
        }
        sortAndRebuild();
        return warnings;
    }

    public synchronized void save() throws FlightSystemException {
        List<String> lines = new ArrayList<>();
        for (Customer customer : customers) {
            lines.add(customer.toDataLine());
        }
        try {
            DataFiles.writeLines(file, lines);
        } catch (IOException e) {
            throw new FlightSystemException("保存 customers.dat 失败：" + e.getMessage(), e);
        }
    }

    public synchronized List<Customer> getAll() {
        return new ArrayList<>(customers);
    }

    public synchronized Customer find(String customerId) {
        return byId.get(normalize(customerId));
    }

    public synchronized boolean exists(String customerId) {
        return find(customerId) != null;
    }

    public synchronized boolean isEmpty() {
        return customers.isEmpty();
    }

    public synchronized List<Customer> search(String keyword) {
        String text = normalize(keyword);
        if (text.isEmpty()) {
            return getAll();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        List<Customer> result = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.getCustomerId().toLowerCase(Locale.ROOT).contains(lower)
                    || customer.getName().toLowerCase(Locale.ROOT).contains(lower)
                    || customer.getPhone().toLowerCase(Locale.ROOT).contains(lower)
                    || customer.getEmail().toLowerCase(Locale.ROOT).contains(lower)) {
                result.add(customer);
            }
        }
        return result;
    }

    public synchronized void add(Customer customer) throws FlightSystemException {
        if (customer == null) {
            throw new FlightSystemException("客户信息不能为空");
        }
        if (customer.getCreatedAt() == null) {
            customer.setCreatedAt(LocalDateTime.now());
        }
        try {
            Customer.validate(customer);
        } catch (IllegalArgumentException e) {
            throw new FlightSystemException(e.getMessage());
        }
        String id = customer.getCustomerId().trim();
        customer.setCustomerId(id);
        if (byId.containsKey(id)) {
            throw new FlightSystemException("证件号「" + id + "」已存在");
        }
        customers.add(customer);
        sortAndRebuild();
        try {
            save();
        } catch (FlightSystemException e) {
            customers.remove(customer);
            sortAndRebuild();
            throw e;
        }
    }

    public synchronized void update(String customerId, Customer updated) throws FlightSystemException {
        String id = normalize(customerId);
        Customer existing = byId.get(id);
        if (existing == null) {
            throw new FlightSystemException("客户不存在：" + customerId);
        }
        if (updated == null) {
            throw new FlightSystemException("客户信息不能为空");
        }
        if (!id.equals(normalize(updated.getCustomerId()))) {
            throw new FlightSystemException("证件号是客户主键，不能修改");
        }
        if (updated.getCreatedAt() == null) {
            updated.setCreatedAt(existing.getCreatedAt());
        }
        try {
            Customer.validate(updated);
        } catch (IllegalArgumentException e) {
            throw new FlightSystemException(e.getMessage());
        }
        customers.remove(existing);
        customers.add(updated);
        sortAndRebuild();
        try {
            save();
        } catch (FlightSystemException e) {
            customers.remove(updated);
            customers.add(existing);
            sortAndRebuild();
            throw e;
        }
    }

    public synchronized void delete(String customerId) throws FlightSystemException {
        String id = normalize(customerId);
        Customer existing = byId.get(id);
        if (existing == null) {
            throw new FlightSystemException("客户不存在：" + customerId);
        }
        customers.remove(existing);
        sortAndRebuild();
        try {
            save();
        } catch (FlightSystemException e) {
            customers.add(existing);
            sortAndRebuild();
            throw e;
        }
    }

    private void sortAndRebuild() {
        customers.sort(Comparator.comparing(Customer::getCustomerId));
        byId.clear();
        for (Customer customer : customers) {
            byId.put(customer.getCustomerId(), customer);
        }
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
