package Spiel;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * Ein wiederverwendbares Panel, das dem Benutzer erlaubt,
 * die Spielertypen (Mensch, verschiedene Bots) für Weiß und Schwarz auszuwählen.
 */
public class SpielerAuswahlPanel extends JPanel {

    private final JComboBox<String> weissAuswahl;
    private final JComboBox<String> schwarzAuswahl;

    /**
     * @param zurueckCallback Funktion, um zum Hauptmenü zurückzukehren.
     * @param startCallback Funktion, um das Spiel mit den ausgewählten Spielern zu starten.
     * @param menschErlaubt Gibt an, ob "Mensch" eine wählbare Option ist (für Bot-vs-Bot nicht).
     */
    public SpielerAuswahlPanel(Runnable zurueckCallback, BiConsumer<Spieler, Spieler> startCallback, boolean menschErlaubt) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // --- Titel ---
        JLabel titleLabel = new JLabel("Spieler auswählen");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Optionen für die Dropdowns ---
        String[] botOptionen = {"Mensch", "Random Bot", "Gieriger Bot", "Martin Bot", "Besser als Stockfish"};

        // --- Weißer Spieler ---
        JLabel weissLabel = new JLabel("Weißer Spieler:");
        weissLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        weissAuswahl = new JComboBox<>(botOptionen);
        weissAuswahl.setMaximumSize(new Dimension(200, 30));
        if (!menschErlaubt) {
            weissAuswahl.removeItem("Mensch");
        }

        // --- Schwarzer Spieler ---
        JLabel schwarzLabel = new JLabel("Schwarzer Spieler:");
        schwarzLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        schwarzAuswahl = new JComboBox<>(botOptionen);
        schwarzAuswahl.setMaximumSize(new Dimension(200, 30));
        if (!menschErlaubt) {
            schwarzAuswahl.removeItem("Mensch");
        }

        // Standardauswahl für Spieler vs Bot
        if (menschErlaubt) {
            schwarzAuswahl.setSelectedItem("Martin Bot");
        }

        // --- Buttons ---
        JButton startBtn = new JButton("Spiel starten");
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.addActionListener(e -> {
            Spieler weiss = erstelleSpieler((String) weissAuswahl.getSelectedItem(), Figur.Farbe.WEISS);
            Spieler schwarz = erstelleSpieler((String) schwarzAuswahl.getSelectedItem(), Figur.Farbe.SCHWARZ);
            // Verhindern, dass Mensch gegen Mensch hier ausgewählt wird, wenn nicht vorgesehen
            if (!menschErlaubt && (weiss instanceof MenschSpieler || schwarz instanceof MenschSpieler)) {
                JOptionPane.showMessageDialog(this, "Im 'Bot vs Bot'-Modus müssen beide Spieler Bots sein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (weiss instanceof MenschSpieler && schwarz instanceof MenschSpieler) {
                 JOptionPane.showMessageDialog(this, "Für 'Spieler vs Spieler' bitte den Button im Hauptmenü verwenden.", "Info", JOptionPane.INFORMATION_MESSAGE);
                 return;
            }
            startCallback.accept(weiss, schwarz);
        });

        JButton backBtn = new JButton("Zurück");
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.addActionListener(e -> zurueckCallback.run());

        // --- Panel zusammenbauen ---
        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 40)));
        add(weissLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(weissAuswahl);
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(schwarzLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(schwarzAuswahl);
        add(Box.createRigidArea(new Dimension(0, 40)));
        add(startBtn);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(backBtn);
    }

    // Erstellt basierend auf der Auswahl im Dropdown-Menü das passende Spieler-Objekt.
    private Spieler erstelleSpieler(String typ, Figur.Farbe farbe) {
        if (typ == null) return new MenschSpieler(farbe); // Fallback
        switch (typ) {
            case "Mensch": return new MenschSpieler(farbe);
            case "Random Bot": return new RandomBotSpieler(farbe);
            case "Gieriger Bot": return new ersterBotSpieler(farbe);
            case "Martin Bot": return new MartinBot(farbe);
            case "Besser als Stockfish": return new BesserAlsStockFishBot(farbe);
            default: return new MenschSpieler(farbe);
        }
    }
}