package oop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * 航班查询页签：支持按航班号查询、按出发/到达城市查询。
 * 查询结果中展示起降时间、城市、票价、折扣与满仓状态。
 */
public class FlightQueryPanel extends JPanel {

    private final FlightSystem system;
    private final FlightSystemGUI owner;

    private final JTextField txtFlightId = UiUtils.textField("完整航班号，如 CA1234");
    private final JTextField txtStartCity = UiUtils.textField("如 桂林");
    private final JTextField txtEndCity = UiUtils.textField("如 北京");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<Flight> displayedFlights = new ArrayList<>();

    public FlightQueryPanel(FlightSystem system, FlightSystemGUI owner) {
        this.system = system;
        this.owner = owner;

        setLayout(new BorderLayout(8, 8));
        setBackground(UiUtils.BG);

        tableModel = new DefaultTableModel(null, new String[]{
                "航班号", "日期", "起飞", "到达", "航线", "机型",
                "票价", "折扣", "实付价", "余票", "状态", "航空公司"
        }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        UiUtils.styleTable(table);
        UiUtils.setColumnWidths(table, 90, 95, 65, 80, 125, 90, 75, 70, 80, 60, 70, 120);

        // 满仓状态用红色，其余用绿色
        table.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component component = super.getTableCellRendererComponent(
                        source, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    component.setBackground(row % 2 == 0 ? UiUtils.ROW_EVEN : UiUtils.ROW_ODD);
                }
                setHorizontalAlignment(CENTER);
                setFont(UiUtils.FONT_BOLD);
                setForeground("满仓".equals(value) ? UiUtils.DANGER : UiUtils.SUCCESS);
                setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                return component;
            }
        });

        add(createQueryPanel(), BorderLayout.NORTH);

        JScrollPane scrollPane = UiUtils.scrollPane("查询结果", table);
        add(scrollPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createQueryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtils.PRIMARY, 1),
                "航班查询",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                UiUtils.FONT_BOLD,
                UiUtils.PRIMARY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 第一行：按航班号
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(UiUtils.formLabel("航班号"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.2;
        panel.add(txtFlightId, gbc);

        JButton btnQueryId = UiUtils.button("按航班号查询", UiUtils.PRIMARY, true);
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(btnQueryId, gbc);

        JLabel spacer = new JLabel();
        gbc.gridx = 3;
        gbc.weightx = 0.4;
        panel.add(spacer, gbc);

        // 第二行：按城市
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(UiUtils.formLabel("出发城市"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.2;
        panel.add(txtStartCity, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(UiUtils.formLabel("到达城市"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.2;
        panel.add(txtEndCity, gbc);

        JButton btnQueryRoute = UiUtils.button("按航线查询", UiUtils.PRIMARY, true);
        JButton btnShowAll = UiUtils.button("显示全部航班", UiUtils.NEUTRAL, false);
        JButton btnClear = UiUtils.button("清空条件", new Color(120, 120, 120), false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(btnQueryRoute);
        buttons.add(btnShowAll);
        buttons.add(btnClear);

        gbc.gridx = 4;
        gbc.weightx = 0.6;
        panel.add(buttons, gbc);

        btnQueryId.addActionListener(e -> queryById());
        btnQueryRoute.addActionListener(e -> queryByRoute());
        btnShowAll.addActionListener(e -> showAll());
        btnClear.addActionListener(e -> clearConditions());

        return panel;
    }

    /** 数据变更后由主窗口调用 */
    public void refreshData() {
        showFlights(system.getFlights());
    }

    private void queryById() {
        String id = txtFlightId.getText().trim();
        if (id.isEmpty()) {
            owner.showStatus("请输入要查询的航班编号", "error");
            return;
        }
        List<Flight> result = system.searchFlightById(id);
        showFlights(result);
        if (result.isEmpty()) {
            owner.showStatus("未找到航班「" + id + "」", "error");
        } else {
            owner.showStatus("找到航班「" + id + "」", "success");
        }
    }

    private void queryByRoute() {
        String start = txtStartCity.getText().trim();
        String end = txtEndCity.getText().trim();
        if (start.isEmpty() || end.isEmpty()) {
            owner.showStatus("请同时输入出发城市和到达城市", "error");
            return;
        }
        List<Flight> result = system.searchFlightsByRoute(start, end);
        showFlights(result);
        if (result.isEmpty()) {
            owner.showStatus("没有找到 " + start + " → " + end + " 的航班", "error");
        } else {
            owner.showStatus("查询到 " + result.size() + " 个 " + start + " → " + end + " 航班", "success");
        }
    }

    private void showAll() {
        txtFlightId.setText("");
        txtStartCity.setText("");
        txtEndCity.setText("");
        showFlights(system.getFlights());
        owner.showStatus("已显示全部航班", "info");
    }

    private void clearConditions() {
        txtFlightId.setText("");
        txtStartCity.setText("");
        txtEndCity.setText("");
        owner.showStatus("查询条件已清空", "info");
    }

    private void showFlights(List<Flight> flights) {
        displayedFlights.clear();
        displayedFlights.addAll(flights);

        tableModel.setRowCount(0);
        for (Flight flight : flights) {
            tableModel.addRow(new Object[]{
                    flight.getFlightId(),
                    flight.getDateText(),
                    flight.getTimeText(),
                    flight.getArrivalTimeText(),
                    flight.getRoute(),
                    flight.getPlaneModel(),
                    UiUtils.money(flight.getPrice()),
                    UiUtils.discountText(flight.getDiscount()),
                    UiUtils.money(flight.getActualPrice()),
                    flight.getRemainingSeats(),
                    flight.isFull() ? "满仓" : "可订",
                    flight.getAirline()
            });
        }
        table.clearSelection();
    }
}
