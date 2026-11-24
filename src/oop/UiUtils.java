package oop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;

/**
 * 界面公共样式与组件工具。
 */
public final class UiUtils {

    public static final Color PRIMARY = new Color(25, 118, 210);
    public static final Color SUCCESS = new Color(46, 125, 50);
    public static final Color DANGER = new Color(198, 40, 40);
    public static final Color WARNING = new Color(255, 145, 0);
    public static final Color NEUTRAL = new Color(90, 90, 90);
    public static final Color BG = new Color(250, 250, 250);
    public static final Color ROW_EVEN = new Color(240, 245, 252);
    public static final Color ROW_ODD = Color.WHITE;

    public static final Font FONT_NORMAL = new Font("Microsoft YaHei", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("Microsoft YaHei", Font.BOLD, 14);
    public static final Font FONT_TITLE = new Font("Microsoft YaHei", Font.BOLD, 18);
    public static final Font FONT_SMALL = new Font("Microsoft YaHei", Font.PLAIN, 12);

    private UiUtils() {
    }

    public static JLabel formLabel(String text) {
        JLabel label = new JLabel(text + "：", SwingConstants.RIGHT);
        label.setFont(FONT_BOLD);
        label.setForeground(new Color(80, 80, 80));
        return label;
    }

    public static JTextField textField(String tooltip) {
        JTextField field = new JTextField();
        field.setFont(FONT_NORMAL);
        field.setPreferredSize(new Dimension(110, 30));
        field.setToolTipText(tooltip);
        return field;
    }

    public static JButton button(String text, Color background, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(FONT_BOLD);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBackground(background);

        if (primary) {
            button.setForeground(Color.WHITE);
        } else {
            double brightness = background.getRed() * 0.299
                    + background.getGreen() * 0.587
                    + background.getBlue() * 0.114;
            button.setForeground(brightness < 140 ? Color.WHITE : Color.DARK_GRAY);
        }
        button.setPreferredSize(new Dimension(Math.max(100, text.length() * 16 + 24), 34));
        return button;
    }

    public static JScrollPane scrollPane(String title, Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(PRIMARY, 1),
                title,
                TitledBorder.LEADING,
                TitledBorder.TOP,
                FONT_BOLD,
                PRIMARY));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(187, 222, 251));
        table.setSelectionForeground(Color.BLACK);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(FONT_BOLD);
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component component = super.getTableCellRendererComponent(
                        source, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    component.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                }
                setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                return component;
            }
        });
    }

    /** 设置列宽（列数不足时忽略多余值） */
    public static void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public static String money(double value) {
        return String.format("%.2f", value);
    }

    public static String discountText(double discount) {
        if (discount >= 0.999) {
            return "无折扣";
        }
        return new DecimalFormat("0.#").format(discount * 10) + "折";
    }

    public static String percent(double value) {
        return String.format("%.1f%%", value);
    }
}
