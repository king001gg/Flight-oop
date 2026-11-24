package oop;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * 航空公司航班订票系统主窗口。
 * <p>窗口采用页签组织五个功能模块：航班管理、航班查询、订票、退票 / 订单、统计汇总。</p>
 */
public class FlightSystemGUI extends JFrame {

    private final FlightSystem system = new FlightSystem();
    private final JLabel lblStatus = new JLabel("就绪");
    private final JLabel lblCounts = new JLabel();

    private final FlightManagePanel flightManagePanel;
    private final FlightQueryPanel flightQueryPanel;
    private final BookingPanel bookingPanel;
    private final OrderPanel orderPanel;
    private final StatisticsPanel statisticsPanel;

    public FlightSystemGUI() {
        setLookAndFeel();

        FlightSystem.LoadReport report = system.load();

        flightManagePanel = new FlightManagePanel(system, this);
        flightQueryPanel = new FlightQueryPanel(system, this);
        bookingPanel = new BookingPanel(system, this);
        orderPanel = new OrderPanel(system, this);
        statisticsPanel = new StatisticsPanel(system, this);

        initUI();
        refreshAll();

        StringBuilder message = new StringBuilder("数据加载完成：航班 ")
                .append(system.getFlightCount())
                .append(" 条，订单 ")
                .append(system.getOrderCount())
                .append(" 条");
        if (!report.getWarnings().isEmpty()) {
            message.append("，").append(report.getWarnings().size()).append(" 条提示");
        }
        showStatus(message.toString(), "info");

        if (report.getWarnings().stream().anyMatch(w -> w.contains("行"))) {
            SwingUtilities.invokeLater(() -> showLoadWarnings(report.getWarnings()));
        }
    }

    private void setLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    UIManager.put("defaultFont", UiUtils.FONT_NORMAL);
                    UIManager.put("Label.font", UiUtils.FONT_NORMAL);
                    UIManager.put("Button.font", UiUtils.FONT_NORMAL);
                    UIManager.put("TextField.font", UiUtils.FONT_NORMAL);
                    UIManager.put("Table.font", UiUtils.FONT_NORMAL);
                    UIManager.put("TableHeader.font", UiUtils.FONT_BOLD);
                    UIManager.put("TitledBorder.font", UiUtils.FONT_BOLD);
                    UIManager.put("TabbedPane.font", UiUtils.FONT_BOLD);
                    break;
                }
            }
        } catch (Exception ignored) {
            // 无法设置 Nimbus 时降级为系统默认外观
        }
    }

    private void initUI() {
        setTitle("航空公司航班订票系统");
        setSize(1380, 880);
        setMinimumSize(new Dimension(1180, 740));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(UiUtils.BG);

        add(createTitlePanel(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UiUtils.FONT_BOLD);
        tabs.setBackground(UiUtils.BG);
        tabs.addTab("航班管理", flightManagePanel);
        tabs.addTab("航班查询", flightQueryPanel);
        tabs.addTab("订票", bookingPanel);
        tabs.addTab("退票 / 订单", orderPanel);
        tabs.addTab("统计汇总", statisticsPanel);
        add(tabs, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiUtils.PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("航空公司航班订票系统");
        title.setFont(UiUtils.FONT_TITLE);
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.WEST);

        JLabel subTitle = new JLabel("Flight Booking System v2.0");
        subTitle.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        subTitle.setForeground(new Color(255, 255, 255, 190));
        panel.add(subTitle, BorderLayout.EAST);

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        panel.setBackground(new Color(245, 245, 245));

        lblStatus.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblStatus.setForeground(new Color(100, 100, 100));
        panel.add(lblStatus, BorderLayout.WEST);

        lblCounts.setFont(UiUtils.FONT_SMALL);
        lblCounts.setForeground(new Color(150, 150, 150));
        panel.add(lblCounts, BorderLayout.EAST);

        return panel;
    }

    /** 状态栏消息：type 取 success / error / info */
    public void showStatus(String message, String type) {
        Color color = switch (type) {
            case "success" -> UiUtils.SUCCESS;
            case "error" -> UiUtils.DANGER;
            default -> new Color(90, 90, 90);
        };
        lblStatus.setText("● " + message);
        lblStatus.setForeground(color);
        updateCounts();
    }

    /** 数据发生变更后刷新所有页签 */
    public void dataChanged() {
        refreshAll();
    }

    public void refreshAll() {
        flightManagePanel.refreshData();
        flightQueryPanel.refreshData();
        bookingPanel.refreshData();
        orderPanel.refreshData();
        statisticsPanel.refreshData();
        updateCounts();
    }

    private void updateCounts() {
        lblCounts.setText("航班 " + system.getFlightCount()
                + " 条  |  订单 " + system.getOrderCount() + " 条");
    }

    private void showLoadWarnings(List<String> warnings) {
        StringBuilder text = new StringBuilder("以下数据行存在问题，已跳过：\n\n");
        int shown = 0;
        for (String warning : warnings) {
            if (warning.contains("行") && shown < 10) {
                text.append("• ").append(warning).append('\n');
                shown++;
            }
        }
        if (warnings.size() > shown) {
            text.append("…… 共 ").append(warnings.size()).append(" 条提示");
        }
        JOptionPane.showMessageDialog(this, text.toString(),
                "数据加载提示", JOptionPane.WARNING_MESSAGE);
    }
}
