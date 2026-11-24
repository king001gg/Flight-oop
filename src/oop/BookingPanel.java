package oop;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
 * 订票页签：填写客户资料与航班号完成订票；
 * 余票不足时自动推荐同航线的可替代航班。
 */
public class BookingPanel extends JPanel {

    private final FlightSystem system;
    private final FlightSystemGUI owner;

    private final JTextField txtName = UiUtils.textField("客户姓名");
    private final JTextField txtCustomerId = UiUtils.textField("身份证 / 护照号");
    private final JTextField txtFlightId = UiUtils.textField("可直接输入，或在下方列表中选择");
    private final JTextField txtQuantity = UiUtils.textField("正整数，如 2");
    private final JLabel lblFlightInfo = new JLabel("未选择航班");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<Flight> displayedFlights = new ArrayList<>();

    public BookingPanel(FlightSystem system, FlightSystemGUI owner) {
        this.system = system;
        this.owner = owner;

        setLayout(new BorderLayout(8, 8));
        setBackground(UiUtils.BG);

        tableModel = new DefaultTableModel(null, new String[]{
                "航班号", "日期", "起飞", "航线", "实付价", "余票", "状态"
        }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        UiUtils.styleTable(table);
        UiUtils.setColumnWidths(table, 100, 110, 80, 160, 100, 80, 80);

        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
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

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0 && row < displayedFlights.size()) {
                txtFlightId.setText(displayedFlights.get(row).getFlightId());
                updateFlightInfo();
            }
        });

        // 手动输入航班号时同步更新航班信息
        txtFlightId.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFlightInfo();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFlightInfo();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFlightInfo();
            }
        });

        add(createFormPanel(), BorderLayout.NORTH);

        JScrollPane scrollPane = UiUtils.scrollPane("航班余票列表（单击一行可选择该航班）", table);
        scrollPane.setPreferredSize(new Dimension(0, 320));
        add(scrollPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createFormPanel() {
        JPanel outer = new JPanel(new BorderLayout(6, 6));
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtils.PRIMARY, 1),
                "订票",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                UiUtils.FONT_BOLD,
                UiUtils.PRIMARY));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 8, 5, 8);

        // 第一行：客户姓名、证件号
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(UiUtils.formLabel("客户姓名"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        form.add(txtName, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        form.add(UiUtils.formLabel("证件号"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.4;
        form.add(txtCustomerId, gbc);

        // 第二行：航班号、订票数量
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(UiUtils.formLabel("航班号"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        form.add(txtFlightId, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        form.add(UiUtils.formLabel("订票数量"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.4;
        form.add(txtQuantity, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        buttons.setOpaque(false);
        JButton btnBook = UiUtils.button("订票", UiUtils.PRIMARY, true);
        JButton btnQuery = UiUtils.button("查询余票", UiUtils.NEUTRAL, false);
        JButton btnClear = UiUtils.button("清空", new Color(120, 120, 120), false);
        buttons.add(btnBook);
        buttons.add(btnQuery);
        buttons.add(btnClear);

        btnBook.addActionListener(e -> doBook());
        btnQuery.addActionListener(e -> queryFlight());
        btnClear.addActionListener(e -> clearForm(true));

        lblFlightInfo.setFont(UiUtils.FONT_SMALL);
        lblFlightInfo.setForeground(UiUtils.PRIMARY);
        lblFlightInfo.setBorder(BorderFactory.createEmptyBorder(0, 12, 6, 12));

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        bottom.add(buttons);
        bottom.add(lblFlightInfo);
        bottom.add(Box.createVerticalStrut(2));

        outer.add(form, BorderLayout.NORTH);
        outer.add(bottom, BorderLayout.CENTER);
        return outer;
    }

    /** 数据变更后由主窗口调用 */
    public void refreshData() {
        String keepId = txtFlightId.getText().trim();
        displayedFlights.clear();
        displayedFlights.addAll(system.getFlights());

        tableModel.setRowCount(0);
        for (Flight flight : displayedFlights) {
            tableModel.addRow(new Object[]{
                    flight.getFlightId(),
                    flight.getDateText(),
                    flight.getTimeText(),
                    flight.getRoute(),
                    UiUtils.money(flight.getActualPrice()),
                    flight.getRemainingSeats(),
                    flight.isFull() ? "满仓" : "可订"
            });
        }
        table.clearSelection();

        if (!keepId.isEmpty()) {
            int row = indexOfFlight(keepId);
            if (row >= 0) {
                table.setRowSelectionInterval(row, row);
            }
        }
        updateFlightInfo();
    }

    private int indexOfFlight(String flightId) {
        for (int i = 0; i < displayedFlights.size(); i++) {
            if (displayedFlights.get(i).getFlightId().equals(flightId)) {
                return i;
            }
        }
        return -1;
    }

    private void doBook() {
        String name = txtName.getText().trim();
        String customerId = txtCustomerId.getText().trim();
        String flightId = txtFlightId.getText().trim();
        String quantityText = txtQuantity.getText().trim();

        if (name.isEmpty() || customerId.isEmpty() || flightId.isEmpty() || quantityText.isEmpty()) {
            owner.showStatus("客户姓名、证件号、航班号和订票数量均不能为空", "error");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            owner.showStatus("订票数量必须是整数", "error");
            return;
        }
        if (quantity <= 0) {
            owner.showStatus("订票数量必须大于 0", "error");
            return;
        }

        try {
            Order order = system.book(flightId, name, customerId, quantity);
            owner.dataChanged();
            owner.showStatus("订票成功：订单 " + order.getOrderId(), "success");
            JOptionPane.showMessageDialog(this,
                    "订票成功！\n\n"
                            + "订单号：" + order.getOrderId() + "\n"
                            + "航班号：" + order.getFlightId() + "\n"
                            + "客户：" + order.getCustomerName() + "\n"
                            + "数量：" + order.getQuantity() + " 张\n"
                            + "单价：¥" + UiUtils.money(order.getUnitPrice()) + "\n"
                            + "总金额：¥" + UiUtils.money(order.getAmount()),
                    "订票成功", JOptionPane.INFORMATION_MESSAGE);
            clearForm(false);
            owner.dataChanged();
        } catch (NoSeatsException e) {
            owner.showStatus(e.getMessage(), "error");
            showAlternatives(e);
        } catch (FlightSystemException e) {
            owner.showStatus(e.getMessage(), "error");
        }
    }

    /** 满仓/余票不足时，列出同航线的可替代航班供选择 */
    private void showAlternatives(NoSeatsException exception) {
        List<Flight> alternatives = exception.getAlternatives();
        if (alternatives.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    exception.getMessage() + "\n同航线暂时没有其他可订航班。",
                    "余票不足", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] options = new String[alternatives.size()];
        for (int i = 0; i < alternatives.size(); i++) {
            options[i] = alternativeText(alternatives.get(i));
        }

        Object choice = JOptionPane.showInputDialog(this,
                exception.getMessage() + "\n\n请选择同航线的可替代航班：",
                "余票不足 - 推荐航班",
                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        if (choice != null) {
            String text = choice.toString();
            String id = text.substring(0, text.indexOf(" | "));
            txtFlightId.setText(id);
            updateFlightInfo();
            owner.showStatus("已选择替代航班「" + id + "」，请确认后再次点击「订票」", "info");
        }
    }

    private String alternativeText(Flight flight) {
        return flight.getFlightId()
                + " | " + flight.getDateText() + " " + flight.getTimeText()
                + " | " + flight.getRoute()
                + " | 余票 " + flight.getRemainingSeats();
    }

    private void queryFlight() {
        String id = txtFlightId.getText().trim();
        if (id.isEmpty()) {
            owner.showStatus("请先输入或选择航班号", "error");
            return;
        }
        Flight flight = system.findFlight(id);
        if (flight == null) {
            owner.showStatus("航班不存在：" + id, "error");
            return;
        }
        updateFlightInfo();
        if (flight.isFull()) {
            owner.showStatus("航班「" + id + "」已满仓，余票 0 张", "info");
        } else {
            owner.showStatus("航班「" + id + "」余票 " + flight.getRemainingSeats() + " 张", "success");
        }
    }

    private void updateFlightInfo() {
        String id = txtFlightId.getText().trim();
        if (id.isEmpty()) {
            lblFlightInfo.setText("未选择航班");
            lblFlightInfo.setForeground(UiUtils.NEUTRAL);
            return;
        }
        Flight flight = system.findFlight(id);
        if (flight == null) {
            lblFlightInfo.setText("航班不存在：" + id);
            lblFlightInfo.setForeground(UiUtils.DANGER);
            return;
        }
        String text = flight.getFlightId()
                + " | " + flight.getDateText() + " " + flight.getTimeText()
                + " - " + flight.getArrivalTimeText()
                + " | " + flight.getRoute()
                + " | 余票 " + flight.getRemainingSeats() + "/" + flight.getTotalSeats()
                + " | 实付 ¥" + UiUtils.money(flight.getActualPrice());
        if (flight.isFull()) {
            text += " | 已满仓";
            lblFlightInfo.setForeground(UiUtils.DANGER);
        } else {
            lblFlightInfo.setForeground(UiUtils.PRIMARY);
        }
        lblFlightInfo.setText(text);
    }

    private void clearForm(boolean showMessage) {
        txtName.setText("");
        txtCustomerId.setText("");
        txtFlightId.setText("");
        txtQuantity.setText("");
        table.clearSelection();
        updateFlightInfo();
        if (showMessage) {
            owner.showStatus("订票信息已清空", "info");
        }
    }
}
