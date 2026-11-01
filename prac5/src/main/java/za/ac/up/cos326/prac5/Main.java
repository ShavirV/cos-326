package za.ac.up.cos326.prac5;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Entry point of the COS326 Practical 5 social-network investigation tool. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default Swing look and feel
        }
        SwingUtilities.invokeLater(() -> new MainFrame(new Neo4jService()).setVisible(true));
    }
}
