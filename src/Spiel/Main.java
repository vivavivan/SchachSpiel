package Spiel;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import javax.swing.*;

/**
 * Startet das Spiel
 * @author Vivan Chandrasekhara
 * @since JDK23
 * @see Spiel.MainMenu
 */

public class Main {
    public static void main(String[] args) {
        FlatMacLightLaf.setup();
        SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
    }
}