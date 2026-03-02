package minilibrary;

import javax.swing.*;
import minilibrary.gui.LoginGUI;

// ─────────────────────────────────────────────
//  ENTRY POINT
//  Run this file to launch the application.
// ─────────────────────────────────────────────
public class Exer6_MiniLibrary {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LoginGUI().show());
    }
}
