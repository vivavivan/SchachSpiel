package Spiel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Die Hauptklasse für die grafische Benutzeroberfläche (GUI).
 * Sie ist verantwortlich für das Zeichnen des Bretts, der Figuren und die Verarbeitung von Spielereingaben.
 */
public class SpielGUI extends JPanel implements ActionListener {

    // GUI-Komponenten
    private final JPanel brettPanel;
    private final JButton[][] felder = new JButton[8][8];
    private final JPanel linkesPanel;
    private final JPanel rechtesPanel;
    private final JLayeredPane brettEbenenPanel;
    private final JPanel umwandlungsPanel;
    private JLabel statusAnzeige;
    private JLabel timerAnzeigeWeiss;
    private JLabel timerAnzeigeSchwarz;

    // Spiel-Logik und Status
    private final Brett brett;
    private boolean istAmUmwandeln = false;
    private int umwandlungsZeile = -1;
    private int umwandlungsSpalte = -1;
    private List<int[]> moeglicheZuege = new ArrayList<>();
    private boolean spielBeendet = false;

    // ausgewaehlteFigur speichert den "ersten Klick" auf dem Brett
    private Figur ausgewaehlteFigur;
    private int vonZeile = -1;
    private int vonSpalte = -1;
    private Figur.Farbe amZug = Figur.Farbe.WEISS;
    
    private int zeitWeiss = 600; // 10 Minuten in Sekunden standard
    private int zeitSchwarz = 600;
    private Timer spielTimer;

    // Spieler Auswahl
    private final Spieler spielerWeiss;
    private final Spieler spielerSchwarz;

    /**
     * Erstellt das komplette Spielfenster.
     * @param zurueckZumMenu Eine Funktion, die aufgerufen wird, um zum Hauptmenü zurückzukehren.
     * @param zeitLimit Das Zeitlimit pro Spieler in Sekunden (-1 für unendlich).
     * @param spielerWeiss Das Spieler-Objekt für Weiß.
     * @param spielerSchwarz Das Spieler-Objekt für Schwarz.
     */
    public SpielGUI(Runnable zurueckZumMenu, int zeitLimit, Spieler spielerWeiss, Spieler spielerSchwarz) {
        super(new BorderLayout());
        this.brett = new Brett();
        
        this.zeitWeiss = zeitLimit;
        this.zeitSchwarz = zeitLimit;
        this.spielerWeiss = spielerWeiss;
        this.spielerSchwarz = spielerSchwarz;

        // Platzhalter für das Schachbrett
        brettPanel = new JPanel(new GridLayout(8, 8));

        //Seiten-Panels erstellen
        linkesPanel = new JPanel(new BorderLayout());
        linkesPanel.setPreferredSize(new Dimension(180, 0));
        linkesPanel.setBackground(Color.LIGHT_GRAY);

        rechtesPanel = new JPanel();
        rechtesPanel.setPreferredSize(new Dimension(180, 0));
        rechtesPanel.setBackground(Color.LIGHT_GRAY);
        rechtesPanel.setLayout(new BorderLayout());

        timerAnzeigeSchwarz = erstelleTimerAnzeige();
        rechtesPanel.add(timerAnzeigeSchwarz, BorderLayout.NORTH);

        timerAnzeigeWeiss = erstelleTimerAnzeige();
        rechtesPanel.add(timerAnzeigeWeiss, BorderLayout.SOUTH);

        JPanel menueContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        menueContainer.setOpaque(false);

        JButton menueKnopf = new JButton("\u2190");
        menueKnopf.setFont(new Font("SansSerif", Font.PLAIN, 24));
        menueKnopf.setFocusPainted(false);
        menueKnopf.setMargin(new Insets(5, 5, 5, 5));
        menueKnopf.addActionListener(e -> {
            if (spielTimer != null) spielTimer.stop();
            zurueckZumMenu.run();
        });

        // Undo-Button
        JButton undoKnopf = new JButton("\u21A9"); // Unicode für Undo-Pfeil
        undoKnopf.setFont(new Font("SansSerif", Font.PLAIN, 24));
        undoKnopf.setFocusPainted(false);
        undoKnopf.setMargin(new Insets(5, 5, 5, 5));
        undoKnopf.addActionListener(e -> {
            // Verhindern, dass Undo gedrückt wird, während der Bot rechnet
            Spieler aktuellerSpieler = (amZug == Figur.Farbe.WEISS) ? spielerWeiss : spielerSchwarz;
            if (aktuellerSpieler.istBot() && !spielBeendet) return;

            if (brett.undo()) {
                // Spieler wechseln und Status zurücksetzen nur wenn Undo erfolgreich war
                amZug = (amZug == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
                spielBeendet = false; // Spiel wieder aufnehmen

                // Bei Mensch gegen Bot muss 2 Züge zurück gemacht werden
                Spieler jetzigerSpieler = (amZug == Figur.Farbe.WEISS) ? spielerWeiss : spielerSchwarz;
                Spieler andererSpieler = (amZug == Figur.Farbe.WEISS) ? spielerSchwarz : spielerWeiss;

                if (jetzigerSpieler.istBot() && !andererSpieler.istBot()) {
                    if (brett.undo()) {
                        amZug = (amZug == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
                    } else {
                        // Sonderfall: Bot hat das Spiel begonnen
                        // Dann muss der Bot jetzt neu ziehen
                        pruefeUndMacheBotZug();
                    }
                }

                ausgewaehlteFigur = null;
                moeglicheZuege.clear();
                aktualisiereBrett();
                if(brettPanel != null){
                    brettPanel.repaint();
                }
                pruefeSpielStatus();

                // Timer fortsetzen falls er gestoppt war
                if (spielTimer != null && !spielTimer.isRunning() && zeitWeiss != -1 && zeitWeiss > 0 && zeitSchwarz > 0) {
                    spielTimer.start();
                }
            }
        });

        menueContainer.add(menueKnopf);
        if (!(spielerWeiss.istBot() && spielerSchwarz.istBot())) {
            menueContainer.add(undoKnopf);
        }
        linkesPanel.add(menueContainer, BorderLayout.NORTH);

        // Status Label unten links
        statusAnzeige = new JLabel("", SwingConstants.CENTER);
        statusAnzeige.setFont(new Font("SansSerif", Font.BOLD, 16));
        statusAnzeige.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        linkesPanel.add(statusAnzeige, BorderLayout.SOUTH);

        add(linkesPanel, BorderLayout.WEST);
        add(rechtesPanel, BorderLayout.EAST);

        JPanel hauptContainer = new JPanel(new GridBagLayout());
        hauptContainer.setBackground(Color.LIGHT_GRAY);

        brettEbenenPanel = new JLayeredPane();


        brettPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        brettEbenenPanel.add(brettPanel, JLayeredPane.DEFAULT_LAYER);

        // Bauer umwandlung Auswahl über dem Brett
        umwandlungsPanel = new JPanel(new GridLayout(4, 1));
        umwandlungsPanel.setVisible(false);
        brettEbenenPanel.add(umwandlungsPanel, JLayeredPane.POPUP_LAYER);

        for (int zeile = 8 - 1; zeile >= 0; zeile--) {
            for (int spalte = 0; spalte < 8; spalte++) {
                final int z = zeile;
                final int s = spalte;
                felder[zeile][spalte] = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        for (int[] zug : moeglicheZuege) {
                            if (zug[0] == z && zug[1] == s) {
                                Graphics2D g2 = (Graphics2D) g.create();
                                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                g2.setColor(new Color(100, 100, 100, 100));
                                int d = getWidth() / 4;
                                g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                                g2.dispose();
                                break;
                            }
                        }
                    }
                };
                felder[zeile][spalte].setMargin(new Insets(0, 0, 0, 0));
                felder[zeile][spalte].putClientProperty("JButton.buttonType", "square"); // FlatLaf: Eckige Buttons erzwingen
                felder[zeile][spalte].setFont(new Font("SansSerif", Font.PLAIN, 20));
                felder[zeile][spalte].setFocusPainted(false); // Entfernt den Rahmen beim Anklicken
                felder[zeile][spalte].setBorder(null);
                felder[zeile][spalte].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        JButton btn = (JButton) e.getSource();
                        if (btn.getBackground().equals(Color.YELLOW)) return;

                        Color c = btn.getBackground();
                        int darken = 30;
                        btn.setBackground(new Color(
                                Math.max(0, c.getRed() - darken),
                                Math.max(0, c.getGreen() - darken),
                                Math.max(0, c.getBlue() - darken)
                        ));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        JButton btn = (JButton) e.getSource();
                        if (btn.getBackground().equals(Color.YELLOW)) return;

                        if ((z + s) % 2 == 0) {
                            btn.setBackground(Color.LIGHT_GRAY);
                        } else {
                            btn.setBackground(Color.WHITE);
                        }
                    }
                });
                if ((zeile + spalte) % 2 == 0) {
                    felder[zeile][spalte].setBackground(Color.LIGHT_GRAY);
                } else {
                    felder[zeile][spalte].setBackground(Color.WHITE);
                }
                felder[zeile][spalte].addActionListener(this);
                brettPanel.add(felder[zeile][spalte]);
            }
        }

        hauptContainer.add(brettEbenenPanel);
        add(hauptContainer, BorderLayout.CENTER);

        // Stellt sicher, das das Brett quadratisch bleibt
        // und passt die Schriftgröße der Figuren dynamisch an.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int availableWidth = getWidth() - linkesPanel.getPreferredSize().width - rechtesPanel.getPreferredSize().width;
                int availableHeight = getHeight();

                int minSize = Math.min(availableWidth, availableHeight) - 50;
                if (minSize <= 0) return;

                brettEbenenPanel.setPreferredSize(new Dimension(minSize, minSize));
                brettPanel.setBounds(0, 0, minSize, minSize);

                if (istAmUmwandeln) {
                    passeUmwandlungsGroesseAn(minSize);
                }

                brettEbenenPanel.revalidate();

                int fontSize = (int) ((minSize / 8.0) * 0.6);
                Font font = new Font("SansSerif", Font.PLAIN, Math.max(1, fontSize));

                for (int zeile = 0; zeile < 8; zeile++) {
                    for (int spalte = 0; spalte < 8; spalte++) {
                        felder[zeile][spalte].setFont(font);
                    }
                }

                for (Component c : umwandlungsPanel.getComponents()) {
                    c.setFont(font);
                }
            }
        });

        aktualisiereBrett();
        starteTimer();

        // Falls Weiß ein Bot ist, muss er direkt anfangen
        pruefeUndMacheBotZug();
    }

    // Zeichnet alle Figuren basierend auf dem internen `brett`-Objekt neu.
    private void aktualisiereBrett() {
        for (int zeile = 0; zeile < 8; zeile++) {
            for (int spalte = 0; spalte < 8; spalte++) {
                Figur figur = brett.getFigur(zeile, spalte);
                if (figur != null) {
                    felder[zeile][spalte].setText(figur.getFigurIcon());
                } else {
                    felder[zeile][spalte].setText("");
                }
                if ((zeile + spalte) % 2 == 0) {
                    felder[zeile][spalte].setBackground(Color.LIGHT_GRAY);
                } else {
                    felder[zeile][spalte].setBackground(Color.WHITE);
                }
            }
        }
    }

    /**
     * Die zentrale Methode, die auf alle Klicks auf dem Schachbrett reagiert.
     * Sie unterscheidet, ob eine Figur ausgewählt oder ein Zug gemacht wird.
     * @param e welches Feld wurde angeclickt
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (spielBeendet || istAmUmwandeln) return;

        // Wenn ein Bot am Zug ist, darf der Mensch nicht klicken
        Spieler aktuellerSpieler = (amZug == Figur.Farbe.WEISS) ? spielerWeiss : spielerSchwarz;
        if (aktuellerSpieler.istBot() && !spielBeendet) return;

        Object source = e.getSource();
        for (int zeile = 0; zeile < 8; zeile++) {
            for (int spalte = 0; spalte < 8; spalte++) {
                if (source == felder[zeile][spalte]) {
                    // Welche Figur wurde beim ersten Klick ausgewählt
                    if (ausgewaehlteFigur == null) {
                        Figur figur = brett.getFigur(zeile, spalte);
                        if (figur != null && figur.getFarbe() == amZug) {
                            ausgewaehlteFigur = figur;
                            vonZeile = zeile;
                            vonSpalte = spalte;
                            felder[zeile][spalte].setBackground(Color.YELLOW);
                            moeglicheZuege = brett.getLegaleZuege(zeile, spalte);
                            brettPanel.repaint();
                        }
                    // Wenn eine Figur schon augewählt wurde, wird geschaut wohin sie bewegt werden soll
                    } else {
                        // Bauernumwandlung?
                        boolean istBauer = ausgewaehlteFigur instanceof Bauer;
                        boolean zielIstEndreihe = (zeile == 0 || zeile == 7);

                        if (istBauer && zielIstEndreihe && brett.istZugGueltig(vonZeile, vonSpalte, zeile, spalte)) {
                            // Zug ist eine Promotion -> noch nicht ausführen, erst Auswahl anzeigen
                            istAmUmwandeln = true;

                            if ((vonZeile + vonSpalte) % 2 == 0) {
                                felder[vonZeile][vonSpalte].setBackground(Color.LIGHT_GRAY);
                            } else {
                                felder[vonZeile][vonSpalte].setBackground(Color.WHITE);
                            }
                            moeglicheZuege.clear();
                            ausgewaehlteFigur = null;
                            brettPanel.repaint();

                            zeigeUmwandlungsAuswahl(vonZeile, vonSpalte, zeile, spalte);
                            return;
                        } else if (brett.istZugGueltig(vonZeile, vonSpalte, zeile, spalte)) {
                            // Normaler Zug
                            brett.bewegeFigur(vonZeile, vonSpalte, zeile, spalte);
                            amZug = (amZug == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
                            pruefeSpielStatus();
                            if (!spielBeendet) pruefeUndMacheBotZug(); // Prüfen, ob jetzt ein Bot dran ist
                        }
                        ausgewaehlteFigur = null;
                        if ((vonZeile + vonSpalte) % 2 == 0) {
                            felder[vonZeile][vonSpalte].setBackground(Color.LIGHT_GRAY);
                        } else {
                            felder[vonZeile][vonSpalte].setBackground(Color.WHITE);
                        }
                        moeglicheZuege.clear();
                        aktualisiereBrett();
                        brettPanel.repaint();
                    }
                    return;
                }
            }
        }
    }

    // Passt die Größe des Umwandlungs-Popups an die aktuelle Brettgröße an.
    private void passeUmwandlungsGroesseAn(int boardSize) {
        Insets insets = brettPanel.getInsets();
        int availableWidth = boardSize - insets.left - insets.right;
        int availableHeight = boardSize - insets.top - insets.bottom;

        int felderBreite = availableWidth / 8;
        int felderHoehe = availableHeight / 8;

        int xPos = insets.left + umwandlungsSpalte * felderBreite;
        int yPos;

        if (amZug == Figur.Farbe.WEISS) {
            // Weiß ist am Zug (hat gerade gezogen), Ziel ist oben
            yPos = insets.top;
        } else {
            yPos = insets.top + 4 * felderHoehe;
        }

        umwandlungsPanel.setBounds(xPos, yPos, felderBreite, 4 * felderHoehe);
    }

    // Zeigt das Auswahlmenü für die Bauernumwandlung an der richtigen Position an.
    private void zeigeUmwandlungsAuswahl(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        umwandlungsZeile = nachZeile;
        umwandlungsSpalte = nachSpalte;
        umwandlungsPanel.removeAll();

        String[] symbole = new String[4];

        symbole[0] = new Koenigin(amZug).getFigurIcon();
        symbole[1] = new Turm(amZug).getFigurIcon();
        symbole[2] = new Laeufer(amZug).getFigurIcon();
        symbole[3] = new Springer(amZug).getFigurIcon();

        boolean isWhite = (amZug == Figur.Farbe.WEISS);
        int start = isWhite ? 0 : 3;
        int end = isWhite ? 4 : -1;
        int step = isWhite ? 1 : -1;

        int[] promoTypIndices = {0, 1, 2, 3}; // Dame, Turm, Läufer, Springer

        for (int i = start; i != end; i += step) {
            int promoTypIndex = promoTypIndices[i];
            JButton btn = new JButton(symbole[i]);
            // Schriftgröße vom aktuellen Brett übernehmen
            btn.setFont(felder[0][0].getFont());
            btn.setMargin(new Insets(0, 0, 0, 0));
            btn.putClientProperty("JButton.buttonType", "square"); // Auch hier eckig für Konsistenz
            btn.setFocusPainted(false);
            btn.setBackground(Color.WHITE);
            
            btn.addActionListener(e -> {
                brett.bewegeFigur(vonZeile, vonSpalte, nachZeile, nachSpalte, 1, promoTypIndex);
                umwandlungsPanel.setVisible(false); // Panel wieder verstecken
                istAmUmwandeln = false; // Brett wieder freigeben
                amZug = (amZug == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
                aktualisiereBrett();
                pruefeSpielStatus(); // This was missing
                if (!spielBeendet) pruefeUndMacheBotZug(); // Nach Promotion könnte ein Bot dran sein
            });
            umwandlungsPanel.add(btn);
        }

        passeUmwandlungsGroesseAn(brettPanel.getWidth());
        umwandlungsPanel.setVisible(true);
    }

    // Erstellt ein Label für die Zeitanzeige.
    private JLabel erstelleTimerAnzeige() {
        String text = (zeitWeiss == -1) ? "" : formatiereZeit(zeitWeiss);
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 24));
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        return label;
    }

    // Startet den Spiel-Timer, der jede Sekunde die Zeit des aktiven Spielers reduziert.
    private void starteTimer() {
        if (zeitWeiss == -1) return;
        spielTimer = new Timer(1000, e -> {
            if (amZug == Figur.Farbe.WEISS) {
                zeitWeiss--;
                timerAnzeigeWeiss.setText(formatiereZeit(zeitWeiss));
            } else {
                zeitSchwarz--;
                timerAnzeigeSchwarz.setText(formatiereZeit(zeitSchwarz));
            }
            
            if (zeitWeiss <= 0 || zeitSchwarz <= 0) {
                spielTimer.stop();
                spielBeendet = true;
                String Nachricht = (zeitWeiss <= 0) ? "Zeit abgelaufen: Weiß hat verloren" : "Zeit abgelaufen: Schwarz hat verloren";
                statusAnzeige.setText("<html><center>" + Nachricht + "</center></html>");
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, Nachricht));
            }
        });
        spielTimer.start();
    }

    // Formatiert die verbleibenden Sekunden in das "MM:SS"-Format.
    private String formatiereZeit(int sekunden) {
        int min = sekunden / 60;
        int sek = sekunden % 60;
        return String.format("%02d:%02d", min, sek);
    }

    /**
     * Prüft, ob der aktuelle Spieler ein Bot ist.
     * Wenn ja, wird die Zugberechnung in einem separaten Thread gestartet,
     * um ein Einfrieren vom GUI zu verhindern.
     */
    private void pruefeUndMacheBotZug() {
        Spieler aktuellerSpieler = (amZug == Figur.Farbe.WEISS) ? spielerWeiss : spielerSchwarz;

        if (aktuellerSpieler.istBot() && aktuellerSpieler instanceof BotSpieler) {
            BotSpieler bot = (BotSpieler) aktuellerSpieler;

            // Berechnung in einem neuen Thread, damit GUI nicht einfriert
            new Thread(() -> {
                try {
                    // Kurze Verzögerung für besseres Spielgefühl
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Bot berechnet den Zug (gibt int[] {vonZ, vonS, nachZ, nachS} zurück)
                int[] zug = bot.berechneZug(brett);

                // Zug im GUI-Thread ausführen
                SwingUtilities.invokeLater(() -> {
                    if (spielBeendet) return;
                    if (zug != null && zug.length == 6) {
                        brett.bewegeFigur(zug[0], zug[1], zug[2], zug[3], zug[4], zug[5]);

                        amZug = (amZug == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
                        aktualisiereBrett();
                        pruefeSpielStatus();
                        
                        // Rekursiver Aufruf, falls Bot vs Bot
                        if (!spielBeendet) pruefeUndMacheBotZug();
                    }
                });
            }).start();
        }
    }

    /**
     * Prüft nach jedem Zug den Spielstatus.
     * Aktualisiert die "Schach"-Anzeige oder zeigt ein Popup bei Spielende (Matt, Patt, Remis).
     */
    private void pruefeSpielStatus() {
        int status = brett.getSchachmatt();
        if (status != 0) {
            spielBeendet = true;
            if (spielTimer != null) spielTimer.stop();

            String nachricht = "";
            if (status == -1) nachricht = "Schachmatt: Weiß hat gewonnen";
            else if (status == 1) nachricht = "Schachmatt: Schwarz hat gewonnen";
            else if (status == -2 || status == 2) nachricht = "Patt: Unentschieden";
            else if (status == 3) nachricht = "Remis: Zu wenig Material";

            statusAnzeige.setText("<html><center>" + nachricht + "</center></html>");
            String finalNachricht = nachricht;
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, finalNachricht));
        } else {
            boolean weissImSchach = brett.istKoenigBedroht(Figur.Farbe.WEISS);
            boolean schwarzImSchach = brett.istKoenigBedroht(Figur.Farbe.SCHWARZ);

            if (weissImSchach) {
                statusAnzeige.setText("<html><center>Weiß<br>im Schach</center></html>");
            } else if (schwarzImSchach) {
                statusAnzeige.setText("<html><center>Schwarz<br>im Schach</center></html>");
            } else {
                statusAnzeige.setText("");
            }
        }
    }
}
