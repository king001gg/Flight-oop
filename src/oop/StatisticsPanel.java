package oop;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 统计汇总页签：
 * <ul>
 *   <li>按航线 + 时间段统计飞行次数、提供座位、载客人数与客座率；</li>
 *   <li>按自然月统计售票张数、退票张数、净售票与净销售额。</li>
 * </ul>
 */
public class StatisticsPanel extends JPanel {

    private final FlightSystem system;
    private final FlightSystemGUI owner;

    private final JTextField txtStartCity = UiUtils.textField("留空表示全部航线");
    private final JTextField txtEndCity = UiUtils.textField("留空表示全部航线");
    private final JTextField txtFrom = UiUtils.textField("yyyy-MM-dd");
    private final JTextField txtTo = UiUtils.textField("yyyy-MM-dd");

    private final JLabel lblFlightCount = createResultLabel("飞行次数：0 次");
    private final JLabel lblSeats = createResultLabel("提供座位：0 个");
    private final JLabel lblPassengers = createResultLabel("载客人数：0 人");
    private final JLabel lblOccupancy = createResultLabel("客座率：0.0%");

    private final DefaultTableModel monthModel;
    private final JTable monthTable;

    public StatisticsPanel(FlightSystem system, FlightSystemGUI owner) {
        this.system = system;
        this.owner = owner;

        setLayout(new BorderLayout(8, 8));
        setBackground(UiUtils.BG);

        monthModel = new DefaultTableModel(null, new String[]{
                "月份", "售票张数", "退票张数", "净售票", "净销售额(元)"
        }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        monthTable = new JTable(monthModel);
        UiUtils.styleTable(monthTable);
        UiUtils.setColumnWidths(monthTable, 100, 100, 100, 100, 140);

        add(createRoutePanel(), BorderLayout.NORTH);
        add(createMonthPanel(), BorderLayout.CENTER);

        initDefaultDates();
        refreshData();
    }

    private static JLabel createResultLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiUtils.FONT_BOLD);
        label.setForeground(UiUtils.PRIMARY);
        return label;
    }

    private void initDefaultDates() {
        LocalDate today = LocalDate.now();
        txtFrom.setText(today.withDayOfMonth(1).toString());
        txtTo.setText(today.withDayOfMonth(today.lengthOfMonth()).toString());
    }

    private JPanel createRoutePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtils.PRIMARY, 1),
                "航线时段统计",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                UiUtils.FONT_BOLD,
                UiUtils.PRIMARY));

        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        inputs.setOpaque(false);

        JButton btnStatistics = UiUtils.button("开始统计", UiUtils.PRIMARY, true);

        inputs.add(UiUtils.formLabel("出发城市"));
        inputs.add(txtStartCity);
        inputs.add(UiUtils.formLabel("到达城市"));
        inputs.add(txtEndCity);
        inputs.add(UiUtils.formLabel("开始日期"));
        inputs.add(txtFrom);
        inputs.add(UiUtils.formLabel("结束日期"));
        inputs.add(txtTo);
        inputs.add(btnStatistics);

        btnStatistics.addActionListener(e -> runStatistics(true));

        JPanel results = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 8));
        results.setOpaque(false);
        results.add(lblFlightCount);
        results.add(lblSeats);
        results.add(lblPassengers);
        results.add(lblOccupancy);

        JLabel hint = new JLabel("提示：出发/到达城市都留空表示统计全部航线；日期格式为 yyyy-MM-dd。");
        hint.setFont(UiUtils.FONT_SMALL);
        hint.setForeground(new Color(130, 130, 130));
        hint.setBorder(BorderFactory.createEmptyBorder(0, 12, 6, 12));

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setOpaque(false);
        south.add(results);
        south.add(hint);
        south.add(Box.createVerticalStrut(2));

        panel.add(inputs, BorderLayout.NORTH);
        panel.add(south, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMonthPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtils.PRIMARY, 1),
                "月度售票统计",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                UiUtils.FONT_BOLD,
                UiUtils.PRIMARY));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        JButton btnRefresh = UiUtils.button("刷新月度统计", UiUtils.NEUTRAL, false);
        btnRefresh.addActionListener(e -> refreshMonthly());
        top.add(btnRefresh);

        JLabel hint = new JLabel("售票按下单月份统计；退票按退票月份统计。");
        hint.setFont(UiUtils.FONT_SMALL);
        hint.setForeground(new Color(130, 130, 130));
        top.add(hint);

        JScrollPane scrollPane = new JScrollPane(monthTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /** 数据变更后由主窗口调用：刷新月度统计并重新计算当前航线条件 */
    public void refreshData() {
        refreshMonthly();
        runStatistics(false);
    }

    private void refreshMonthly() {
        List<MonthStatistics> statistics = system.monthlySales();
        monthModel.setRowCount(0);
        for (MonthStatistics month : statistics) {
            monthModel.addRow(new Object[]{
                    month.getMonthText(),
                    month.getSoldTickets(),
                    month.getRefundedTickets(),
                    month.getNetTickets(),
                    UiUtils.money(month.getNetRevenue())
            });
        }
    }

    /**
     * 执行航线统计。
     *
     * @param announce 为 true 时在状态栏反馈结果；为 false 时用于自动刷新，静默失败
     */
    private void runStatistics(boolean announce) {
        String fromText = txtFrom.getText().trim();
        String toText = txtTo.getText().trim();
        if (fromText.isEmpty() || toText.isEmpty()) {
            if (announce) {
                owner.showStatus("请填写开始日期和结束日期", "error");
            }
            return;
        }

        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromText);
            to = LocalDate.parse(toText);
        } catch (DateTimeParseException e) {
            if (announce) {
                owner.showStatus("日期格式应为 yyyy-MM-dd", "error");
            }
            return;
        }

        try {
            RouteStatistics stats = system.statisticsForRoute(
                    txtStartCity.getText().trim(),
                    txtEndCity.getText().trim(),
                    from, to);
            lblFlightCount.setText("飞行次数：" + stats.getFlightCount() + " 次");
            lblSeats.setText("提供座位：" + stats.getTotalSeats() + " 个");
            lblPassengers.setText("载客人数：" + stats.getPassengers() + " 人");
            lblOccupancy.setText("客座率：" + String.format("%.1f%%", stats.getOccupancyRate()));
            if (announce) {
                owner.showStatus("统计完成：飞行 " + stats.getFlightCount()
                        + " 次，载客 " + stats.getPassengers() + " 人", "success");
            }
        } catch (FlightSystemException e) {
            if (announce) {
                owner.showStatus(e.getMessage(), "error");
            }
        }
    }
}
