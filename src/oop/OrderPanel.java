package oop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 退票 / 订单页签：按订单号、姓名或证件号查询订单，对选中的有效订单退票。
 */
public class OrderPanel extends JPanel {

    private final FlightSystem system;
    private final FlightSystemGUI owner;

    private final JTextField txtKeyword = UiUtils.textField("订单号 / 客户姓名 / 证件号 / 航班号");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<Order> displayedOrders = new ArrayList<>();
    private String selectedOrderId;

    public OrderPanel(FlightSystem system, FlightSystemGUI owner) {
        this.system = system;
        this.owner = owner;

        setLayout(new BorderLayout(8, 8));
        setBackground(UiUtils.BG);

        tableModel = new DefaultTableModel(null, new String[]{
                "订单号", "航班号", "航线", "航班日期", "客户姓名", "证件号",
                "数量", "单价", "金额", "下单时间", "状态", "退票时间"
        }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        UiUtils.styleTable(table);
        UiUtils.setColumnWidths(table, 230, 85, 105, 95, 110, 185, 50, 70, 80, 165, 55, 165);

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
                setForeground("已退".equals(value) ? UiUtils.DANGER : UiUtils.SUCCESS);
                setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                return component;
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0 && row < displayedOrders.size()) {
                selectedOrderId = displayedOrders.get(row).getOrderId();
            }
        });

        add(createQueryPanel(), BorderLayout.NORTH);
        add(UiUtils.scrollPane("订单列表（单击一行可选中，用于退票）", table), BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createQueryPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtils.PRIMARY, 1),
                "订单查询 / 退票",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                UiUtils.FONT_BOLD,
                UiUtils.PRIMARY));

        JLabel label = new JLabel("订单号 / 姓名 / 证件号：");
        label.setFont(UiUtils.FONT_BOLD);
        panel.add(label);

        txtKeyword.setPreferredSize(new Dimension(240, 30));
        panel.add(txtKeyword);

        JButton btnQuery = UiUtils.button("查询订单", UiUtils.PRIMARY, true);
        JButton btnShowAll = UiUtils.button("显示全部订单", UiUtils.NEUTRAL, false);
        JButton btnRefund = UiUtils.button("退票", UiUtils.DANGER, false);
        panel.add(btnQuery);
        panel.add(btnShowAll);
        panel.add(btnRefund);

        JLabel hint = new JLabel("退票前请先在下方表格中选中一条「已订」订单");
        hint.setFont(UiUtils.FONT_SMALL);
        hint.setForeground(new Color(130, 130, 130));
        panel.add(hint);

        btnQuery.addActionListener(e -> queryOrders());
        btnShowAll.addActionListener(e -> showAllOrders());
        btnRefund.addActionListener(e -> refundSelected());

        return panel;
    }

    /** 数据变更后由主窗口调用；有查询条件时保持过滤 */
    public void refreshData() {
        String keyword = txtKeyword.getText().trim();
        List<Order> orders = keyword.isEmpty()
                ? system.getOrders()
                : system.searchOrders(keyword);
        showOrders(orders);
    }

    private void showOrders(List<Order> orders) {
        String keepId = selectedOrderId;

        displayedOrders.clear();
        displayedOrders.addAll(orders);

        tableModel.setRowCount(0);
        for (Order order : orders) {
            Flight flight = system.findFlight(order.getFlightId());
            tableModel.addRow(new Object[]{
                    order.getOrderId(),
                    order.getFlightId(),
                    flight == null ? "未知" : flight.getRoute(),
                    flight == null ? "未知" : flight.getDateText(),
                    order.getCustomerName(),
                    order.getCustomerId(),
                    order.getQuantity(),
                    UiUtils.money(order.getUnitPrice()),
                    UiUtils.money(order.getAmount()),
                    order.getOrderTimeText(),
                    order.getStatusText(),
                    order.getRefundTimeText()
            });
        }
        table.clearSelection();

        if (keepId != null) {
            int row = indexOfOrder(keepId);
            if (row >= 0) {
                table.setRowSelectionInterval(row, row);
            } else {
                selectedOrderId = null;
            }
        }
    }

    private int indexOfOrder(String orderId) {
        for (int i = 0; i < displayedOrders.size(); i++) {
            if (displayedOrders.get(i).getOrderId().equals(orderId)) {
                return i;
            }
        }
        return -1;
    }

    private void queryOrders() {
        String keyword = txtKeyword.getText().trim();
        if (keyword.isEmpty()) {
            owner.showStatus("请输入订单号、客户姓名或证件号", "error");
            return;
        }
        List<Order> result = system.searchOrders(keyword);
        showOrders(result);
        if (result.isEmpty()) {
            owner.showStatus("未找到匹配「" + keyword + "」的订单", "error");
        } else {
            owner.showStatus("查询到 " + result.size() + " 条订单", "success");
        }
    }

    private void showAllOrders() {
        txtKeyword.setText("");
        showOrders(system.getOrders());
        owner.showStatus("已显示全部订单", "info");
    }

    private void refundSelected() {
        if (selectedOrderId == null) {
            owner.showStatus("请先在表格中选中要退票的订单", "error");
            return;
        }
        Order order = system.findOrder(selectedOrderId);
        if (order == null) {
            owner.showStatus("订单不存在：" + selectedOrderId, "error");
            return;
        }
        if (order.isRefunded()) {
            owner.showStatus("订单「" + selectedOrderId + "」已退票，不能重复退票", "error");
            return;
        }

        int option = JOptionPane.showConfirmDialog(this,
                "确定要退票吗？\n\n"
                        + "订单号：" + order.getOrderId() + "\n"
                        + "航班号：" + order.getFlightId() + "\n"
                        + "客户：" + order.getCustomerName() + "\n"
                        + "数量：" + order.getQuantity() + " 张\n"
                        + "金额：¥" + UiUtils.money(order.getAmount()) + "\n\n"
                        + "退票后座位会自动回补，此操作不可撤销。",
                "确认退票", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            system.refund(selectedOrderId);
            String refundedId = selectedOrderId;
            owner.dataChanged();
            owner.showStatus("订单「" + refundedId + "」退票成功", "success");
        } catch (FlightSystemException e) {
            owner.showStatus(e.getMessage(), "error");
        }
    }
}
