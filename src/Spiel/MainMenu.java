package Spiel;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * Das Hauptfenster der Anwendung.
 * Es verwendet ein CardLayout, um zwischen verschiedenen Ansichten
 * (Hauptmenü, Einstellungen, Spieler-Auswahl) zu wechseln.
 */
public class MainMenu extends JFrame {

    private CardLayout kartenLayout;
    private JPanel hauptContainer;
    private int spielZeit = 600; // Standard 10 Minuten

    // Konstruktor baut das Fenster auf und fügt alle "Karten" hinzu.
    public MainMenu() {
        setTitle("Schach Spiel Java");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500); // Fenster größer machen für das Spiel
        setLocationRelativeTo(null); // Fenster zentrieren

        kartenLayout = new CardLayout();
        hauptContainer = new JPanel(kartenLayout);

        // Callback-Funktion, die von den Auswahl-Panels aufgerufen wird, um das Spiel zu starten
        BiConsumer<Spieler, Spieler> startAktion = (weiss, schwarz) -> {
            hauptContainer.add(new SpielGUI(() -> kartenLayout.show(hauptContainer, "Menu"), spielZeit, weiss, schwarz), "Spiel");
            kartenLayout.show(hauptContainer, "Spiel");
        };
        Runnable zurueckAktion = () -> kartenLayout.show(hauptContainer, "Menu");

        // Panels erstellen und als Karten hinzufügen
        hauptContainer.add(erstelleMenuePanel(startAktion), "Menu");
        hauptContainer.add(erstelleEinstellungsPanel(), "Settings");
        hauptContainer.add(new SpielerAuswahlPanel(zurueckAktion, startAktion, true), "PvB_Select");
        hauptContainer.add(new SpielerAuswahlPanel(zurueckAktion, startAktion, false), "BvB_Select");

        add(hauptContainer);
    }

    // Erstellt das Panel für den Hauptmenü-Bildschirm mit den Spielmodus-Buttons.
    private JPanel erstelleMenuePanel(BiConsumer<Spieler, Spieler> startAktion) {
        // Hauptpanel mit vertikalem Layout (BoxLayout)
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50)); // Ränder

        // 1. Titel Label
        JLabel titleLabel = new JLabel("Schach Spiel Java");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        menuPanel.add(titleLabel);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 40))); // Abstand nach unten

        // 2. Buttons erstellen
        JButton botVsSpielerKnopf = erstelleKnopf("Bot vs Spieler");
        JButton spielerVsSpielerKnopf = erstelleKnopf("Spieler vs Spieler");
        JButton botVsBotKnopf = erstelleKnopf("Bot vs Bot");
        JButton einstellungenKnopf = erstelleKnopf("Einstellungen");

        botVsSpielerKnopf.addActionListener(e -> {
            kartenLayout.show(hauptContainer, "PvB_Select");
        });

        // Spieler vs Spieler
        spielerVsSpielerKnopf.addActionListener(e -> {
            Spieler weiss = new MenschSpieler(Figur.Farbe.WEISS);
            Spieler schwarz = new MenschSpieler(Figur.Farbe.SCHWARZ);
            startAktion.accept(weiss, schwarz);
        });

        // Bot vs Bot
        botVsBotKnopf.addActionListener(e -> {
            kartenLayout.show(hauptContainer, "BvB_Select");
        });

        einstellungenKnopf.addActionListener(e -> kartenLayout.show(hauptContainer, "Settings"));

        // Buttons zum Panel hinzufügen (mit Abständen)
        menuPanel.add(botVsSpielerKnopf);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        menuPanel.add(spielerVsSpielerKnopf);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        menuPanel.add(botVsBotKnopf);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        menuPanel.add(einstellungenKnopf);

        return menuPanel;
    }

    // Eine kleine Hilfsmethode, um Buttons mit einheitlichem Stil zu erstellen.
    private JButton erstelleKnopf(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false); // Entfernt den Rahmen beim Anklicken
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(250, 40)); // Breite festlegen
        return button;
    }

    // Erstellt das Panel für den Einstellungs-Bildschirm (aktuell nur für die Zeitwahl).
    private JPanel erstelleEinstellungsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JLabel titleLabel = new JLabel("Einstellungen");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel zeitAnzeige = new JLabel("Zeitlimit pro Spieler:");
        zeitAnzeige.setAlignmentX(Component.CENTER_ALIGNMENT);

        String[] zeitOptionen = {"03:00", "05:00", "10:00", "30:00", "1h", "Kein Zeitlimit"};
        JComboBox<String> zeitAuswahl = new JComboBox<>(zeitOptionen);
        zeitAuswahl.setMaximumSize(new Dimension(200, 30));
        zeitAuswahl.setSelectedIndex(2); // Default 10:00
        
        zeitAuswahl.addActionListener(e -> {
            String selected = (String) zeitAuswahl.getSelectedItem();
            switch (selected) {
                case "03:00": spielZeit = 180; break;
                case "05:00": spielZeit = 300; break;
                case "10:00": spielZeit = 600; break;
                case "30:00": spielZeit = 1800; break;
                case "1h": spielZeit = 3599; break; // 59:59
                case "Kein Zeitlimit": spielZeit = -1; break;
            }
        });

        JButton zurueckKnopf = erstelleKnopf("Zurück");
        zurueckKnopf.addActionListener(e -> kartenLayout.show(hauptContainer, "Menu"));

        settingsPanel.add(titleLabel);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        settingsPanel.add(zeitAnzeige);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        settingsPanel.add(zeitAuswahl);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        settingsPanel.add(zurueckKnopf);

        return settingsPanel;
    }
}