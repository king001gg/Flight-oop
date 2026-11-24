package oop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 航班管理页签：录入、修改、删除、按航班号查询、显示全部。
 */
public class FlightManagePanel extends JPanel {

    private final FlightSystem system;
    private final FlightSystemGUI owner;

    private final JTextField txtFlightId = UiUtils.textField("如 CA1234");
    private final JTextField txtDate = UiUtils.textField("yyyy-MM-dd，如 2026-11-25");
    private final JTextField txtTime = UiUtils.textField("HH:mm，如 08:30");
    private final JTextField txtDuration = UiUtils.textField("分钟数，如 120");
    private final JTextField txtStartCity = UiUtils.textField("如 桂林");
    private final JTextField txtEndCity = UiUtils.textField("如 北京");
    private final JTextField txtModel = UiUtils.textField("如 A320");
    private final JTextField txtPrice = UiUtils.textField("原价，如 1280");
    private final JTextField txtDiscount = UiUtils.textField("0~1，如 0.85 表示 8.5 折");
    private final JTextField txtSeats = UiUtils.textField("总座位数，如 180");
    private final JTextField txtAirline = UiUtils.textField("如 中国国际航空");
    private final JTextField txtOnTime = UiUtils.textField("0~100，如 96.5");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<Flight> displayedFlights = new ArrayList<>();
    private String selectedFlightId;

    public FlightManagePanel(FlightSystem system, FlightSystemGUI owner) {
        this.system = system;
        this.owner = owner;

        setLayout(new BorderLayout(8, 8));
        setBackground(UiUtils.BG);

        tableModel = new DefaultTableModel(null, new String[]{
                "航班号", "日期", "起飞", "到达", "航线", "机型",
                "票价", "折扣", "实付价", "余票/总", "航空公司", "准点率"
        }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        UiUtils.styleTable(table);
        UiUtils.setColumnWidths(table, 90, 95, 65, 80, 125, 90, 75, 70, 80, 80, 120, 65);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0 && row < displayedFlights.size()) {
                Flight flight = displayedFlights.get(row);
                selectedFlightId = flight.getFlightId();
                fillForm(flight);
            }
        });

        add(createFormPanel(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setOpaque(false);
        content.add(createButtonPanel(), BorderLayout.NORTH);

        JScrollPane scrollPane = UiUtils.scrollPane("航班列表（单击一行可回填到上方表单）", table);
        scrollPane.setPreferredSize(new Dimension(0, 330));
        content.add(scrollPane, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtils.PRIMARY, 1),
                "航班信息录入 / 修改",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                UiUtils.FONT_BOLD,
                UiUtils.PRIMARY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 8, 5, 8);

        addFormRow(panel, gbc, 0, "航班编号", txtFlightId,
                "执飞日期", txtDate, "起飞时间", txtTime);
        addFormRow(panel, gbc, 1, "飞行时长(分钟)", txtDuration,
                "出发城市", txtStartCity, "到达城市", txtEndCity);
        addFormRow(panel, gbc, 2, "飞机型号", txtModel,
                "票价(元)", txtPrice, "折扣(0-1)", txtDiscount);
        addFormRow(panel, gbc, 3, "总座位数", txtSeats,
                "航空公司", txtAirline, "准点率(%)", txtOnTime);
        return panel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row,
                            String label1, JComponent field1,
                            String label2, JComponent field2,
                            String label3, JComponent field3) {
        final double labelWeight = 0.075;
        final double fieldWeight = 0.24;

        gbc.gridy = row;

        gbc.gridx = 0;
        gbc.weightx = labelWeight;
        panel.add(UiUtils.formLabel(label1), gbc);

        gbc.gridx = 1;
        gbc.weightx = fieldWeight;
        panel.add(field1, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.01;
        panel.add(new JLabel(), gbc);

        gbc.gridx = 3;
        gbc.weightx = labelWeight;
        panel.add(UiUtils.formLabel(label2), gbc);

        gbc.gridx = 4;
        gbc.weightx = fieldWeight;
        panel.add(field2, gbc);

        gbc.gridx = 5;
        gbc.weightx = 0.01;
        panel.add(new JLabel(), gbc);

        gbc.gridx = 6;
        gbc.weightx = labelWeight;
        panel.add(UiUtils.formLabel(label3), gbc);

        gbc.gridx = 7;
        gbc.weightx = fieldWeight;
        panel.add(field3, gbc);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        panel.setOpaque(false);

        JButton btnAdd = UiUtils.button("新增航班", UiUtils.PRIMARY, true);
        JButton btnUpdate = UiUtils.button("修改航班", UiUtils.WARNING, false);
        JButton btnDelete = UiUtils.button("删除航班", UiUtils.DANGER, false);
        JButton btnQuery = UiUtils.button("按航班号查询", UiUtils.NEUTRAL, false);
        JButton btnShowAll = UiUtils.button("显示全部航班", UiUtils.NEUTRAL, false);
        JButton btnClear = UiUtils.button("清空输入框", new Color(120, 120, 120), false);

        panel.add(btnAdd);
        panel.add(btnUpdate);
        panel.add(btnDelete);
        panel.add(btnQuery);
        panel.add(btnShowAll);
        panel.add(btnClear);

        btnAdd.addActionListener(e -> addFlight());
        btnUpdate.addActionListener(e -> updateFlight());
        btnDelete.addActionListener(e -> deleteFlight());
        btnQuery.addActionListener(e -> queryById());
        btnShowAll.addActionListener(e -> showAllFlights());
        btnClear.addActionListener(e -> clearForm(true));

        return panel;
    }

    /** 数据变更后由主窗口调用：保留当前选中行 */
    public void refreshData() {
        String keepId = selectedFlightId;
        showFlights(system.getFlights());
        if (keepId != null) {
            int row = indexOfFlight(keepId);
            if (row >= 0) {
                table.setRowSelectionInterval(row, row);
            } else {
                selectedFlightId = null;
            }
        }
    }

    private void showFlights(List<Flight> flights) {
        displayedFlights.clear();
        displayedFlights.addAll(flights);

        tableModel.setRowCount(0);
        for (Flight flight : flights) {
            tableModel.addRow(toRow(flight));
        }
        table.clearSelection();
    }

    private Object[] toRow(Flight flight) {
        return new Object[]{
                flight.getFlightId(),
                flight.getDateText(),
                flight.getTimeText(),
                flight.getArrivalTimeText(),
                flight.getRoute(),
                flight.getPlaneModel(),
                UiUtils.money(flight.getPrice()),
                UiUtils.discountText(flight.getDiscount()),
                UiUtils.money(flight.getActualPrice()),
                flight.getRemainingSeats() + "/" + flight.getTotalSeats(),
                flight.getAirline(),
                String.format("%.1f", flight.getOnTimeRate())
        };
    }

    private int indexOfFlight(String flightId) {
        for (int i = 0; i < displayedFlights.size(); i++) {
            if (displayedFlights.get(i).getFlightId().equals(flightId)) {
                return i;
            }
        }
        return -1;
    }

    private void addFlight() {
        Flight flight = readForm();
        if (flight == null) {
            return;
        }
        try {
            system.addFlight(flight);
            selectedFlightId = null;
            clearForm(false);
            owner.dataChanged();
            owner.showStatus("航班「" + flight.getFlightId() + "」新增成功", "success");
        } catch (FlightSystemException e) {
            owner.showStatus(e.getMessage(), "error");
        }
    }

    private void updateFlight() {
        if (selectedFlightId == null) {
            owner.showStatus("请先在表格中选中要修改的航班", "error");
            return;
        }
        Flight updated = readForm();
        if (updated == null) {
            return;
        }
        try {
            system.updateFlight(selectedFlightId, updated);
            selectedFlightId = updated.getFlightId();
            owner.dataChanged();
            owner.showStatus("航班「" + updated.getFlightId() + "」修改成功", "success");
        } catch (FlightSystemException e) {
            owner.showStatus(e.getMessage(), "error");
        }
    }

    private void deleteFlight() {
        if (selectedFlightId == null) {
            owner.showStatus("请先在表格中选中要删除的航班", "error");
            return;
        }
        int option = JOptionPane.showConfirmDialog(this,
                "确定删除航班「" + selectedFlightId + "」吗？\n此操作不可撤销。",
                "确认删除", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (option != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            system.deleteFlight(selectedFlightId);
            String deletedId = selectedFlightId;
            selectedFlightId = null;
            clearForm(false);
            owner.dataChanged();
            owner.showStatus("航班「" + deletedId + "」已删除", "success");
        } catch (FlightSystemException e) {
            owner.showStatus(e.getMessage(), "error");
        }
    }

    private void queryById() {
        String id = txtFlightId.getText().trim();
        if (id.isEmpty()) {
            owner.showStatus("请输入要查询的航班编号", "error");
            return;
        }
        List<Flight> found = system.searchFlightById(id);
        if (found.isEmpty()) {
            showFlights(new ArrayList<>());
            selectedFlightId = null;
            owner.showStatus("未找到航班「" + id + "」", "error");
            return;
        }
        selectedFlightId = found.get(0).getFlightId();
        showFlights(found);
        table.setRowSelectionInterval(0, 0);
        owner.showStatus("找到航班「" + id + "」", "success");
    }

    private void showAllFlights() {
        selectedFlightId = null;
        showFlights(system.getFlights());
        owner.showStatus("已显示全部航班", "info");
    }

    /** 清空表单；showMessage 为 true 时同时更新状态栏 */
    private void clearForm(boolean showMessage) {
        txtFlightId.setText("");
        txtDate.setText("");
        txtTime.setText("");
        txtDuration.setText("");
        txtStartCity.setText("");
        txtEndCity.setText("");
        txtModel.setText("");
        txtPrice.setText("");
        txtDiscount.setText("");
        txtSeats.setText("");
        txtAirline.setText("");
        txtOnTime.setText("");
        selectedFlightId = null;
        table.clearSelection();
        if (showMessage) {
            owner.showStatus("就绪 — 请录入航班信息", "info");
        }
    }

    private void fillForm(Flight flight) {
        txtFlightId.setText(flight.getFlightId());
        txtDate.setText(flight.getDateText());
        txtTime.setText(flight.getTimeText());
        txtDuration.setText(String.valueOf(flight.getDurationMinutes()));
        txtStartCity.setText(flight.getStartCity());
        txtEndCity.setText(flight.getEndCity());
        txtModel.setText(flight.getPlaneModel());
        txtPrice.setText(String.valueOf(flight.getPrice()));
        txtDiscount.setText(String.valueOf(flight.getDiscount()));
        txtSeats.setText(String.valueOf(flight.getTotalSeats()));
        txtAirline.setText(flight.getAirline());
        txtOnTime.setText(String.valueOf(flight.getOnTimeRate()));
    }

    /** 从表单读取并校验航班信息，失败返回 null 并提示 */
    private Flight readForm() {
        String id = txtFlightId.getText().trim();
        String dateText = txtDate.getText().trim();
        String timeText = txtTime.getText().trim();
        String durationText = txtDuration.getText().trim();
        String startCity = txtStartCity.getText().trim();
        String endCity = txtEndCity.getText().trim();
        String model = txtModel.getText().trim();
        String priceText = txtPrice.getText().trim();
        String discountText = txtDiscount.getText().trim();
        String seatsText = txtSeats.getText().trim();
        String airline = txtAirline.getText().trim();
        String onTimeText = txtOnTime.getText().trim();

        if (id.isEmpty() || dateText.isEmpty() || timeText.isEmpty() || durationText.isEmpty()
                || startCity.isEmpty() || endCity.isEmpty() || model.isEmpty()
                || priceText.isEmpty() || discountText.isEmpty() || seatsText.isEmpty()
                || airline.isEmpty() || onTimeText.isEmpty()) {
            owner.showStatus("所有字段均不能为空，请检查", "error");
            return null;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            owner.showStatus("日期格式应为 yyyy-MM-dd，如 2026-11-25", "error");
            return null;
        }
        LocalTime time;
        try {
            time = LocalTime.parse(timeText);
        } catch (DateTimeParseException e) {
            owner.showStatus("起飞时间格式应为 HH:mm，如 08:30", "error");
            return null;
        }

        Integer duration = parseInt(durationText, "飞行时长");
        if (duration == null) {
            return null;
        }
        if (duration <= 0) {
            owner.showStatus("飞行时长必须大于 0", "error");
            return null;
        }

        Double price = parseDouble(priceText, "票价");
        if (price == null) {
            return null;
        }
        if (price < 0) {
            owner.showStatus("票价不能为负数", "error");
            return null;
        }

        Double discount = parseDouble(discountText, "折扣");
        if (discount == null) {
            return null;
        }
        if (discount <= 0 || discount > 1) {
            owner.showStatus("折扣必须在 0~1 之间（如 0.85 表示 8.5 折）", "error");
            return null;
        }

        Integer seats = parseInt(seatsText, "总座位数");
        if (seats == null) {
            return null;
        }
        if (seats <= 0) {
            owner.showStatus("总座位数必须大于 0", "error");
            return null;
        }

        Double onTime = parseDouble(onTimeText, "准点率");
        if (onTime == null) {
            return null;
        }
        if (onTime < 0 || onTime > 100) {
            owner.showStatus("准点率必须在 0~100 之间", "error");
            return null;
        }

        return new Flight(id, date, time, duration, startCity, endCity, model,
                price, discount, seats, airline, onTime);
    }

    private Integer parseInt(String text, String fieldName) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            owner.showStatus(fieldName + "必须是整数", "error");
            return null;
        }
    }

    private Double parseDouble(String text, String fieldName) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            owner.showStatus(fieldName + "必须是数字", "error");
            return null;
        }
    }
}
