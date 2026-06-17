package oop;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * 航空公司航班查询系统 GUI 主类
 * 功能：增、删、改、查、显示全部
 */
public class FlightSystemGUI extends JFrame implements ActionListener {
    // ── 颜色常量 ──
    private static final Color COLOR_PRIMARY   = new Color(25, 118, 210);   // 主色调（蓝）
    private static final Color COLOR_SUCCESS   = new Color(46, 125, 50);    // 成功（绿）
    private static final Color COLOR_DANGER    = new Color(198, 40, 40);    // 错误（红）
    private static final Color COLOR_BG        = new Color(250, 250, 250);  // 背景
    private static final Color COLOR_ROW_EVEN  = new Color(240, 245, 252);  // 表格偶数行
    private static final Color COLOR_ROW_ODD   = Color.WHITE;               // 表格奇数行

    // ── 字体 ──
    private static final Font FONT_NORMAL = new Font("Microsoft YaHei", Font.PLAIN, 14);
    private static final Font FONT_BOLD   = new Font("Microsoft YaHei", Font.BOLD, 14);
    private static final Font FONT_TITLE  = new Font("Microsoft YaHei", Font.BOLD, 18);

    // ── 持久化文件路径 ──
    private static final String DATA_FILE = "flights.dat";

    // ── 数据容器 ──
    private List<Flight> flightList = new ArrayList<>();

    // ── 输入组件 ──
    private JTextField txtFlightId, txtTakeOffTime, txtDuration;
    private JTextField txtStartCity, txtEndCity, txtPlaneModel;
    private JTextField txtPrice, txtOnTimeRate, txtAirline;

    // ── 按钮 ──
    private JButton btnAdd, btnDelete, btnUpdate, btnQuery, btnShowAll, btnClear;

    // ── 表格 ──
    private JTable table;
    private DefaultTableModel tableModel;
    private final String[] columnNames = {
            "航班编号", "起飞时间", "飞行时长", "出发城市", "到达城市",
            "飞机型号", "票价", "准点率(%)", "所属航空公司"
    };

    // ── 状态栏 ──
    private JLabel lblStatus;

    // ── 选中航班编号 ──
    private String selectedFlightId = null;

    // ════════════════════════════════════════════════════════════════
    //  构造方法
    // ════════════════════════════════════════════════════════════════
    public FlightSystemGUI() {
        setLookAndFeel();
        initUI();
        loadFlightsFromFile();  // 启动时从文件读取持久化数据
        refreshTable();
    }

    /**
     * 设置 Nimbus 外观，提升整体质感
     */
    private void setLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    // 微调 Nimbus 默认字号
                    UIManager.put("defaultFont", FONT_NORMAL);
                    UIManager.put("Label.font", FONT_NORMAL);
                    UIManager.put("Button.font", FONT_NORMAL);
                    UIManager.put("TextField.font", FONT_NORMAL);
                    UIManager.put("Table.font", FONT_NORMAL);
                    UIManager.put("TableHeader.font", FONT_BOLD);
                    UIManager.put("TitledBorder.font", FONT_BOLD);
                    break;
                }
            }
        } catch (Exception ignored) {
            // 降级为系统默认样式
        }
    }

    /**
     * 初始化整体界面结构
     */
    private void initUI() {
        setTitle("航空公司航班查询系统");
        setSize(1150, 680);
        setMinimumSize(new Dimension(950, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(COLOR_BG);

        // ── 顶部标题栏 ──
        add(createTitlePanel(), BorderLayout.NORTH);

        // ── 中部：输入面板 + 按钮 + 表格 ──
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(createInputPanel(), BorderLayout.NORTH);
        centerPanel.add(createButtonPanel(), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // ── 底部：表格 + 状态栏 ──
        JPanel southPanel = new JPanel(new BorderLayout(8, 8));
        southPanel.setOpaque(false);
        southPanel.add(createTablePanel(), BorderLayout.CENTER);
        southPanel.add(createStatusBar(), BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
    }

    // ──────────────────────────────────────────────────────────────
    //  标题面板
    // ──────────────────────────────────────────────────────────────
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("✈  航空公司航班查询系统");
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.WEST);

        JLabel subTitle = new JLabel("Flight Management System v1.0");
        subTitle.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        subTitle.setForeground(new Color(255, 255, 255, 180));
        panel.add(subTitle, BorderLayout.EAST);

        return panel;
    }

    // ──────────────────────────────────────────────────────────────
    //  输入面板（GridBagLayout 表单布局）
    // ──────────────────────────────────────────────────────────────
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_PRIMARY, 1),
                "航班信息录入区",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                FONT_BOLD,
                COLOR_PRIMARY));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        // 初始化输入框
        txtFlightId    = createTextField("例: CA1234");
        txtTakeOffTime = createTextField("例: 2026-06-15 08:30");
        txtDuration    = createTextField("例: 2h30m");
        txtStartCity   = createTextField("例: 北京");
        txtEndCity     = createTextField("例: 上海");
        txtPlaneModel  = createTextField("例: Boeing 737-800");
        txtPrice       = createTextField("例: 1280.00");
        txtOnTimeRate  = createTextField("例: 95.5");
        txtAirline     = createTextField("例: 中国国际航空");

        // 第一行：航班编号 | 起飞时间 | 飞机型号
        addFormRow(panel, gbc, 0, "航班编号", txtFlightId,
                   "起飞时间", txtTakeOffTime,
                   "飞机型号", txtPlaneModel);

        // 第二行：出发城市 | 到达城市 | 飞行时长
        addFormRow(panel, gbc, 1, "出发城市", txtStartCity,
                   "到达城市", txtEndCity,
                   "飞行时长", txtDuration);

        // 第三行：票价 | 准点率(%) | 所属航空公司
        addFormRow(panel, gbc, 2, "票价 (¥)", txtPrice,
                   "准点率 (%)", txtOnTimeRate,
                   "所属航空公司", txtAirline);

        return panel;
    }

    /**
     * 在 GridBagLayout 中添加一行 3 组（标签+输入框）
     */
    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row,
                            String label1, JTextField field1,
                            String label2, JTextField field2,
                            String label3, JTextField field3) {
        // 标签权重小，输入框权重大
        final double labelWeight = 0.08;
        final double fieldWeight = 0.25;

        // 标签1
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = labelWeight;
        panel.add(createFormLabel(label1), gbc);

        // 输入框1
        gbc.gridx = 1;
        gbc.weightx = fieldWeight;
        panel.add(field1, gbc);

        // 间距
        gbc.gridx = 2;
        gbc.weightx = 0.01;
        panel.add(new JLabel(), gbc);

        // 标签2
        gbc.gridx = 3;
        gbc.weightx = labelWeight;
        panel.add(createFormLabel(label2), gbc);

        // 输入框2
        gbc.gridx = 4;
        gbc.weightx = fieldWeight;
        panel.add(field2, gbc);

        // 间距
        gbc.gridx = 5;
        gbc.weightx = 0.01;
        panel.add(new JLabel(), gbc);

        // 标签3
        gbc.gridx = 6;
        gbc.weightx = labelWeight;
        panel.add(createFormLabel(label3), gbc);

        // 输入框3
        gbc.gridx = 7;
        gbc.weightx = fieldWeight;
        panel.add(field3, gbc);
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text + "：", SwingConstants.RIGHT);
        label.setFont(FONT_BOLD);
        label.setForeground(new Color(80, 80, 80));
        return label;
    }

    private JTextField createTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(FONT_NORMAL);
        tf.setPreferredSize(new Dimension(100, 32));
        // 使用 putClientProperty 存储占位提示（仅用于内部说明）
        tf.setToolTipText(placeholder.replace("例: ", ""));
        return tf;
    }

    // ──────────────────────────────────────────────────────────────
    //  按钮面板
    // ──────────────────────────────────────────────────────────────
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.setBackground(COLOR_BG);

        // 增删改（核心操作）
        btnAdd    = createButton("新增航班", COLOR_PRIMARY, true);
        btnDelete = createButton("删除航班", COLOR_DANGER, false);
        btnUpdate = createButton("修改航班", new Color(255, 145, 0), false);

        panel.add(btnAdd);
        panel.add(btnDelete);
        panel.add(btnUpdate);

        // 分隔符
        panel.add(Box.createHorizontalStrut(15));
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 28));
        panel.add(sep);
        panel.add(Box.createHorizontalStrut(15));

        // 查询/显示
        btnQuery   = createButton("按航班号查询", new Color(80, 80, 80), false);
        btnShowAll = createButton("显示全部航班", new Color(80, 80, 80), false);
        btnClear   = createButton("清空输入框", new Color(120, 120, 120), false);

        panel.add(btnQuery);
        panel.add(btnShowAll);
        panel.add(btnClear);

        // 绑定监听
        btnAdd.addActionListener(this);
        btnDelete.addActionListener(this);
        btnUpdate.addActionListener(this);
        btnQuery.addActionListener(this);
        btnShowAll.addActionListener(this);
        btnClear.addActionListener(this);

        return panel;
    }

    private JButton createButton(String text, Color bgColor, boolean isPrimary) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Nimbus 下通过 setBackground 即可改变按钮颜色
        btn.setBackground(bgColor);
        if (isPrimary) {
            btn.setForeground(Color.WHITE);
        } else {
            // 非主按钮使用深色背景时设为白色文字
            double brightness = (bgColor.getRed() * 0.299
                              + bgColor.getGreen() * 0.587
                              + bgColor.getBlue() * 0.114);
            btn.setForeground(brightness < 140 ? Color.WHITE : Color.DARK_GRAY);
        }

        btn.setPreferredSize(new Dimension(text.length() > 5 ? 130 : 110, 34));
        return btn;
    }

    // ──────────────────────────────────────────────────────────────
    //  表格面板
    // ──────────────────────────────────────────────────────────────
    private JScrollPane createTablePanel() {
        tableModel = new DefaultTableModel(null, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 禁止编辑表格
            }
        };
        table = new JTable(tableModel);
        setupTableAppearance();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_PRIMARY, 1),
                "航班信息列表",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                FONT_BOLD,
                COLOR_PRIMARY));
        scrollPane.setPreferredSize(new Dimension(1100, 220));
        scrollPane.getViewport().setBackground(Color.WHITE);

        // 表格点击事件
        table.getSelectionModel().addListSelectionListener(unused -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                selectedFlightId = (String) tableModel.getValueAt(row, 0);
                fillTextFromTable();
            }
        });

        return scrollPane;
    }

    private void setupTableAppearance() {
        // 基本设置
        table.setRowHeight(32);
        table.setShowGrid(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(187, 222, 251));
        table.setSelectionForeground(Color.BLACK);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // 表头样式
        JTableHeader header = table.getTableHeader();
        header.setBackground(COLOR_PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(FONT_BOLD);
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);

        // 交替行颜色渲染器
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row, int col) {
                Component c = super.getTableCellRendererComponent(
                        tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? COLOR_ROW_EVEN : COLOR_ROW_ODD);
                }
                setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                return c;
            }
        });

        // 列宽分配
        int[] colWidths = {90, 140, 80, 80, 80, 130, 80, 80, 110};
        for (int i = 0; i < colWidths.length && i < columnNames.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(colWidths[i]);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  状态栏
    // ──────────────────────────────────────────────────────────────
    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        panel.setBackground(new Color(245, 245, 245));

        lblStatus = new JLabel("就绪 — 请录入航班信息");
        lblStatus.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblStatus.setForeground(new Color(100, 100, 100));
        panel.add(lblStatus, BorderLayout.WEST);

        // 记录数显示
        JLabel lblCount = new JLabel("共 0 条记录");
        lblCount.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        lblCount.setForeground(new Color(150, 150, 150));
        panel.add(lblCount, BorderLayout.EAST);

        return panel;
    }

    /**
     * 在状态栏显示消息
     * @param msg 消息文本
     * @param type "success" | "error" | "info"
     */
    private void showStatus(String msg, String type) {
        Color color = switch (type) {
            case "success" -> COLOR_SUCCESS;
            case "error"   -> COLOR_DANGER;
            default        -> new Color(100, 100, 100);
        };
        String prefix = switch (type) {
            case "success" -> "✅ ";
            case "error"   -> "❌ ";
            default        -> "ℹ️  ";
        };
        lblStatus.setText(prefix + msg);
        lblStatus.setForeground(color);

        // 更新记录数
        JPanel bar = (JPanel) lblStatus.getParent();
        for (Component c : bar.getComponents()) {
            if (c instanceof JLabel && c != lblStatus) {
                ((JLabel) c).setText("共 " + flightList.size() + " 条记录");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  文件流持久化（长期存储）
    // ════════════════════════════════════════════════════════════════

    /**
     * 使用 BufferedReader 从文件加载航班数据
     * 文件格式：每行一个航班，字段用 :: 分隔
     */
    private void loadFlightsFromFile() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            showStatus("数据文件不存在，将创建新文件", "info");
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; // 跳过空行
                String[] parts = line.split("::");
                if (parts.length != 9) {
                    System.err.println("跳过格式错误的行: " + line);
                    continue;
                }
                Flight f = new Flight(
                        parts[0], parts[1], parts[2],
                        parts[3], parts[4], parts[5],
                        Double.parseDouble(parts[6]),
                        Double.parseDouble(parts[7]),
                        parts[8]
                );
                flightList.add(f);
                count++;
            }
            showStatus("已从文件加载 " + count + " 条航班记录", "success");
        } catch (IOException e) {
            showStatus("读取数据文件失败: " + e.getMessage(), "error");
        } catch (NumberFormatException e) {
            showStatus("数据文件格式错误: " + e.getMessage(), "error");
        }
    }

    /**
     * 使用 BufferedWriter 将航班数据保存到文件
     * 每行一个航班，字段用 :: 分隔
     */
    private void saveFlightsToFile() {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(DATA_FILE), "UTF-8"))) {
            for (Flight f : flightList) {
                String line = String.join("::",
                        f.getFlightId(),
                        f.getTakeOffTime(),
                        f.getDuration(),
                        f.getStartCity(),
                        f.getEndCity(),
                        f.getPlaneModel(),
                        String.valueOf(f.getPrice()),
                        String.valueOf(f.getOnTimeRate()),
                        f.getAirline()
                );
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            showStatus("保存数据文件失败: " + e.getMessage(), "error");
        }
    }

    private void fillTextFromTable() {
        if (selectedFlightId == null) return;
        for (Flight f : flightList) {
            if (f.getFlightId().equals(selectedFlightId)) {
                txtFlightId.setText(f.getFlightId());
                txtTakeOffTime.setText(f.getTakeOffTime());
                txtDuration.setText(f.getDuration());
                txtStartCity.setText(f.getStartCity());
                txtEndCity.setText(f.getEndCity());
                txtPlaneModel.setText(f.getPlaneModel());
                txtPrice.setText(String.valueOf(f.getPrice()));
                txtOnTimeRate.setText(String.valueOf(f.getOnTimeRate()));
                txtAirline.setText(f.getAirline());
                return;
            }
        }
    }

    private void clearText() {
        txtFlightId.setText("");
        txtTakeOffTime.setText("");
        txtDuration.setText("");
        txtStartCity.setText("");
        txtEndCity.setText("");
        txtPlaneModel.setText("");
        txtPrice.setText("");
        txtOnTimeRate.setText("");
        txtAirline.setText("");
        selectedFlightId = null;
        table.clearSelection();
        showStatus("就绪 — 请录入航班信息", "info");
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Flight f : flightList) {
            tableModel.addRow(new Object[]{
                    f.getFlightId(), f.getTakeOffTime(), f.getDuration(),
                    f.getStartCity(), f.getEndCity(), f.getPlaneModel(),
                    f.getPrice(), f.getOnTimeRate(), f.getAirline()
            });
        }
        showStatus("共 " + flightList.size() + " 条航班记录", "info");
    }

    private Flight getFlightFromText() {
        try {
            String fid     = txtFlightId.getText().trim();
            String time    = txtTakeOffTime.getText().trim();
            String dur     = txtDuration.getText().trim();
            String sCity   = txtStartCity.getText().trim();
            String eCity   = txtEndCity.getText().trim();
            String model   = txtPlaneModel.getText().trim();
            double price   = Double.parseDouble(txtPrice.getText().trim());
            double rate    = Double.parseDouble(txtOnTimeRate.getText().trim());
            String airline = txtAirline.getText().trim();

            if (fid.isEmpty() || time.isEmpty() || dur.isEmpty() || sCity.isEmpty()
                    || eCity.isEmpty() || model.isEmpty() || airline.isEmpty()) {
                showStatus("所有字段均不能为空，请检查！", "error");
                return null;
            }

            return new Flight(fid, time, dur, sCity, eCity, model, price, rate, airline);
        } catch (NumberFormatException e) {
            showStatus("票价、准点率必须为数字，请检查！", "error");
            return null;
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            addFlight();
        } else if (e.getSource() == btnDelete) {
            delFlight();
        } else if (e.getSource() == btnUpdate) {
            updateFlight();
        } else if (e.getSource() == btnQuery) {
            queryFlight();
        } else if (e.getSource() == btnShowAll) {
            refreshTable();
            showStatus("已显示全部航班记录", "info");
        } else if (e.getSource() == btnClear) {
            clearText();
        }
    }

    private void addFlight() {
        Flight flight = getFlightFromText();
        if (flight == null) return;

        for (Flight f : flightList) {
            if (f.getFlightId().equals(flight.getFlightId())) {
                showStatus("航班编号「" + flight.getFlightId() + "」已存在！", "error");
                return;
            }
        }
        flightList.add(flight);
        saveFlightsToFile();  // 持久化到文件
        refreshTable();
        clearText();
        showStatus("航班「" + flight.getFlightId() + "」新增成功！", "success");
    }

    private void delFlight() {
        if (selectedFlightId == null) {
            showStatus("请先在下方表格中选中要删除的航班！", "error");
            return;
        }
        int option = JOptionPane.showConfirmDialog(this,
                "确定删除航班「" + selectedFlightId + "」吗？\n此操作不可撤销。",
                "确认删除", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (option == JOptionPane.YES_OPTION) {
            flightList.removeIf(f -> f.getFlightId().equals(selectedFlightId));
            saveFlightsToFile();  // 持久化到文件
            refreshTable();
            clearText();
            showStatus("航班「" + selectedFlightId + "」已删除", "success");
        }
    }

    private void updateFlight() {
        if (selectedFlightId == null) {
            showStatus("请先在下方表格中选中要修改的航班！", "error");
            return;
        }
        Flight newFlight = getFlightFromText();
        if (newFlight == null) return;

        if (!newFlight.getFlightId().equals(selectedFlightId)) {
            for (Flight f : flightList) {
                if (f.getFlightId().equals(newFlight.getFlightId())) {
                    showStatus("航班编号「" + newFlight.getFlightId() + "」已存在！", "error");
                    return;
                }
            }
        }

        for (int i = 0; i < flightList.size(); i++) {
            if (flightList.get(i).getFlightId().equals(selectedFlightId)) {
                flightList.set(i, newFlight);
                saveFlightsToFile();  // 持久化到文件
                break;
            }
        }
        refreshTable();
        clearText();
        showStatus("航班「" + newFlight.getFlightId() + "」修改成功！", "success");
    }

    private void queryFlight() {
        String key = txtFlightId.getText().trim();
        if (key.isEmpty()) {
            showStatus("请输入要查询的航班编号！", "error");
            return;
        }
        tableModel.setRowCount(0);
        boolean found = false;
        for (Flight f : flightList) {
            if (f.getFlightId().equals(key)) {
                tableModel.addRow(new Object[]{
                        f.getFlightId(), f.getTakeOffTime(), f.getDuration(),
                        f.getStartCity(), f.getEndCity(), f.getPlaneModel(),
                        f.getPrice(), f.getOnTimeRate(), f.getAirline()
                });
                found = true;
            }
        }
        if (found) {
            showStatus("找到航班「" + key + "」", "success");
        } else {
            showStatus("未找到航班「" + key + "」", "error");
            refreshTable(); // 恢复显示全部
        }
    }
}