package Spiel;

import javax.swing.*;
import java.awt.*;

public class MainMenu extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainContainer;
    private int spielZeit = 600; // Standard 10 Minuten

    public MainMenu() {
        setTitle("Schach Spiel Java");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500); // Fenster größer machen für das Spiel
        setLocationRelativeTo(null); // Fenster zentrieren

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(createMenuPanel(), "Menu");
        mainContainer.add(createSettingsPanel(), "Settings");

        add(mainContainer);
    }

    private JPanel createMenuPanel() {
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
        JButton botVsSpielerBtn = createButton("Bot vs Spieler");
        JButton spielerVsSpielerBtn = createButton("Spieler vs Spieler");
        JButton botVsBotBtn = createButton("Bot vs Bot");
        JButton einstellungenBtn = createButton("Einstellungen");

        // Event-Listener für "Spieler vs Spieler" (Startet dein SpielGUI)
        spielerVsSpielerBtn.addActionListener(e -> {
            mainContainer.add(new SpielGUI(() -> cardLayout.show(mainContainer, "Menu"), spielZeit), "Spiel");
            cardLayout.show(mainContainer, "Spiel");
        });

        einstellungenBtn.addActionListener(e -> cardLayout.show(mainContainer, "Settings"));

        // Buttons zum Panel hinzufügen (mit Abständen)
        menuPanel.add(botVsSpielerBtn);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        menuPanel.add(spielerVsSpielerBtn);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        menuPanel.add(botVsBotBtn);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        menuPanel.add(einstellungenBtn);

        return menuPanel;
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false); // Entfernt den Rahmen beim Anklicken
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(250, 40)); // Breite festlegen
        return button;
    }

    private JPanel createSettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JLabel titleLabel = new JLabel("Einstellungen");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel timeLabel = new JLabel("Zeitlimit pro Spieler:");
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String[] timeOptions = {"03:00", "05:00", "10:00", "30:00", "1h", "Kein Zeitlimit"};
        JComboBox<String> timeDropdown = new JComboBox<>(timeOptions);
        timeDropdown.setMaximumSize(new Dimension(200, 30));
        timeDropdown.setSelectedIndex(2); // Default 10:00
        
        timeDropdown.addActionListener(e -> {
            String selected = (String) timeDropdown.getSelectedItem();
            switch (selected) {
                case "03:00": spielZeit = 180; break;
                case "05:00": spielZeit = 300; break;
                case "10:00": spielZeit = 600; break;
                case "30:00": spielZeit = 1800; break;
                case "1h": spielZeit = 3599; break; // 59:59
                case "Kein Zeitlimit": spielZeit = -1; break;
            }
        });

        JButton backBtn = createButton("Zurück");
        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "Menu"));

        settingsPanel.add(titleLabel);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        settingsPanel.add(timeLabel);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        settingsPanel.add(timeDropdown);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        settingsPanel.add(backBtn);

        return settingsPanel;
    }
}