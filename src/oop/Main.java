package oop;
public class Main {
    public static void main(String[] args) {
        // 启动GUI界面
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                new FlightSystemGUI().setVisible(true);
            } catch (Exception e) {
                System.err.println("启动飞行系统GUI时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}