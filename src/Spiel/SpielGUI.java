package Spiel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class SpielGUI extends JPanel implements ActionListener {

    private final JPanel brettPanel;
    private final JButton[][] felder = new JButton[8][8];
    private final Brett brett;
    private final JPanel leftPanel;
    private final JPanel rightPanel;
    private final JLayeredPane boardLayeredPane;
    private final JPanel promotionPanel;
    private boolean isPromoting = false;
    private int promotionZeile = -1;
    private int promotionSpalte = -1;

    private Figur ausgewaehlteFigur;
    private int vonZeile = -1;
    private int vonSpalte = -1;
    private Figur.Farbe amZug = Figur.Farbe.WEISS;
    
    private JLabel timerWeissLabel;
    private JLabel timerSchwarzLabel;
    private int zeitWeiss = 600; // 10 Minuten in Sekunden
    private int zeitSchwarz = 600;
    private Timer gameTimer;

    public SpielGUI(Runnable zurueckZumMenu, int zeitLimit) {
        super(new BorderLayout());
        this.brett = new Brett();
        
        this.zeitWeiss = zeitLimit;
        this.zeitSchwarz = zeitLimit;

        //Seiten-Panels erstellen
        leftPanel = new JPanel();
        leftPanel.setPreferredSize(new Dimension(180, 0));
        leftPanel.setBackground(Color.LIGHT_GRAY);
        leftPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); 

        rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(180, 0));
        rightPanel.setBackground(Color.LIGHT_GRAY);
        rightPanel.setLayout(new BorderLayout());
        
        timerSchwarzLabel = createTimerLabel();
        rightPanel.add(timerSchwarzLabel, BorderLayout.NORTH);
        
        timerWeissLabel = createTimerLabel();
        rightPanel.add(timerWeissLabel, BorderLayout.SOUTH);

        // Menü-Button Container (damit er oben links bleibt)
        JPanel menuContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        menuContainer.setOpaque(false);

        // Menü-Button (Hamburger-Icon)
        JButton menuBtn = new JButton("\u2630");
        menuBtn.setFont(new Font("SansSerif", Font.PLAIN, 24));
        menuBtn.setFocusPainted(false);
        menuBtn.setMargin(new Insets(5, 5, 5, 5));
        menuBtn.addActionListener(e -> {
            if (gameTimer != null) gameTimer.stop();
            zurueckZumMenu.run();
        });

        menuContainer.add(menuBtn);
        leftPanel.add(menuContainer, BorderLayout.NORTH);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        JPanel mainContainer = new JPanel(new GridBagLayout());
        mainContainer.setBackground(Color.LIGHT_GRAY);

        boardLayeredPane = new JLayeredPane();

        // Das eigentliche Schachbrett
        brettPanel = new JPanel(new GridLayout(8, 8));
        brettPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        boardLayeredPane.add(brettPanel, JLayeredPane.DEFAULT_LAYER);

        // Bauer umwandlung Auswahl über dem Brett
        promotionPanel = new JPanel(new GridLayout(4, 1));
        promotionPanel.setVisible(false);
        boardLayeredPane.add(promotionPanel, JLayeredPane.POPUP_LAYER);

        for (int zeile = 8 - 1; zeile >= 0; zeile--) {
            for (int spalte = 0; spalte < 8; spalte++) {
                felder[zeile][spalte] = new JButton();
                felder[zeile][spalte].setMargin(new Insets(0, 0, 0, 0));
                felder[zeile][spalte].setFont(new Font("SansSerif", Font.PLAIN, 20));
                felder[zeile][spalte].setFocusPainted(false); // Entfernt den Rahmen beim Anklicken
                if ((zeile + spalte) % 2 == 0) {
                    felder[zeile][spalte].setBackground(Color.LIGHT_GRAY);
                } else {
                    felder[zeile][spalte].setBackground(Color.WHITE);
                }
                felder[zeile][spalte].addActionListener(this);
                brettPanel.add(felder[zeile][spalte]);
            }
        }

        mainContainer.add(boardLayeredPane);
        add(mainContainer, BorderLayout.CENTER);

        // Stellt sicher, das das Brett quadratisch bleibt
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int availableWidth = getWidth() - leftPanel.getPreferredSize().width - rightPanel.getPreferredSize().width;
                int availableHeight = getHeight();

                int minSize = Math.min(availableWidth, availableHeight) - 50;
                if (minSize <= 0) return;

                boardLayeredPane.setPreferredSize(new Dimension(minSize, minSize));
                brettPanel.setBounds(0, 0, minSize, minSize);

                if (isPromoting) {
                    updatePromotionPanelBounds(minSize);
                }
                
                boardLayeredPane.revalidate();

                int fontSize = (int) ((minSize / 0.8) * 0.6); // Faktor weiter verringert
                Font font = new Font("SansSerif", Font.PLAIN, Math.max(1, fontSize));

                for (int zeile = 0; zeile < 8; zeile++) {
                    for (int spalte = 0; spalte < 8; spalte++) {
                        felder[zeile][spalte].setFont(font);
                    }
                }

                for (Component c : promotionPanel.getComponents()) {
                    c.setFont(font);
                }
            }
        });

        aktualisiereBrett();
        startTimer();
    }

    private void aktualisiereBrett() {
        for (int zeile = 0; zeile < 8; zeile++) {
            for (int spalte = 0; spalte < 8; spalte++) {
                Figur figur = brett.getFigur(zeile, spalte);
                if (figur != null) {
                    felder[zeile][spalte].setText(getFigurSymbol(figur));
                } else {
                    felder[zeile][spalte].setText("");
                }
            }
        }
    }

    private String getFigurSymbol(Figur figur) {
        if (figur.getFarbe() == Figur.Farbe.WEISS) {
            if (figur instanceof Bauer) return "\u2659";
            if (figur instanceof Turm) return "\u2656";
            if (figur instanceof Springer) return "\u2658";
            if (figur instanceof Laeufer) return "\u2657";
            if (figur instanceof Koenigin) return "\u2655";
            if (figur instanceof Koenig) return "\u2654";
        } else {
            if (figur instanceof Bauer) return "\u265F";
            if (figur instanceof Turm) return "\u265C";
            if (figur instanceof Springer) return "\u265E";
            if (figur instanceof Laeufer) return "\u265D";
            if (figur instanceof Koenigin) return "\u265B";
            if (figur instanceof Koenig) return "\u265A";
        }
        return "?";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isPromoting) return;

        Object source = e.getSource();
        for (int zeile = 0; zeile < 8; zeile++) {
            for (int spalte = 0; spalte < 8; spalte++) {
                if (source == felder[zeile][spalte]) {
                    //temporär
                    //System.out.println(brett.istFeldBedroht(zeile, spalte, amZug));
                    if (ausgewaehlteFigur == null) {
                        Figur figur = brett.getFigur(zeile, spalte);
                        if (figur != null && figur.getFarbe() == amZug) {
                            ausgewaehlteFigur = figur;
                            vonZeile = zeile;
                            vonSpalte = spalte;
                            felder[zeile][spalte].setBackground(Color.YELLOW);
                        }
                    } else {
                        if (brett.istZugGueltig(vonZeile, vonSpalte, zeile, spalte)) {
                            // Prüfen, ob es sich um eine Bauernumwandlung handelt
                            boolean istBauer = ausgewaehlteFigur instanceof Bauer;
                            // Weiß startet bei 1 und läuft zu 7. Schwarz startet bei 6 und läuft zu 0.
                            boolean zielIstEndreihe = (zeile == 0 || zeile == 7);

                            brett.bewegeFigur(vonZeile, vonSpalte, zeile, spalte);

                            if (istBauer && zielIstEndreihe) {
                                // Auswahl anzeigen und Zugwechsel pausieren
                                isPromoting = true;
                                
                                // Visuelles Feedback zurücksetzen (da wir hier returnen)
                                if ((vonZeile + vonSpalte) % 2 == 0) {
                                    felder[vonZeile][vonSpalte].setBackground(Color.LIGHT_GRAY);
                                } else {
                                    felder[vonZeile][vonSpalte].setBackground(Color.WHITE);
                                }
                                ausgewaehlteFigur = null;
                                aktualisiereBrett();
                                
                                zeigePromotionAuswahl(zeile, spalte);
                                return; // WICHTIG: Hier abbrechen und auf Button-Klick warten
                            }

                            amZug = (amZug == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
                        }
                        ausgewaehlteFigur = null;
                        if ((vonZeile + vonSpalte) % 2 == 0) {
                            felder[vonZeile][vonSpalte].setBackground(Color.LIGHT_GRAY);
                        } else {
                            felder[vonZeile][vonSpalte].setBackground(Color.WHITE);
                        }
                        aktualisiereBrett();
                    }
                    return;
                }
            }
        }
    }

    private void updatePromotionPanelBounds(int boardSize) {
        Insets insets = brettPanel.getInsets();
        int availableWidth = boardSize - insets.left - insets.right;
        int availableHeight = boardSize - insets.top - insets.bottom;

        int fieldWidth = availableWidth / 8;
        int fieldHeight = availableHeight / 8;

        int xPos = insets.left + promotionSpalte * fieldWidth;
        int yPos;

        // Weiß zieht zur Array-Zeile 7 (Visuell Oben -> y=0). Menü geht nach unten.
        // Schwarz zieht zur Array-Zeile 0 (Visuell Unten -> y=boardHeight). Menü geht nach oben.
        
        if (amZug == Figur.Farbe.WEISS) {
            // Weiß ist am Zug (hat gerade gezogen), Ziel ist oben
            yPos = insets.top;
        } else {
            // Schwarz ist am Zug, Ziel ist unten (Visuell Zeile 7)
            // Menü soll 4 Felder hoch sein und bei Zeile 7 enden.
            // Start y = (7 * fieldHeight) - (3 * fieldHeight) = 4 * fieldHeight
            yPos = insets.top + 4 * fieldHeight;
        }

        promotionPanel.setBounds(xPos, yPos, fieldWidth, 4 * fieldHeight);
    }

    private void zeigePromotionAuswahl(int zeile, int spalte) {
        promotionZeile = zeile;
        promotionSpalte = spalte;
        promotionPanel.removeAll();

        String[] typen = {"Dame", "Turm", "Läufer", "Springer"};
        String[] symbole;
        
        // Passende Symbole je nach Farbe wählen
        if (amZug == Figur.Farbe.WEISS) {
            symbole = new String[]{"\u2655", "\u2656", "\u2657", "\u2658"};
        } else {
            symbole = new String[]{"\u265B", "\u265C", "\u265D", "\u265E"};
        }

        // Reihenfolge anpassen:
        // Weiß (oben): Dame, Turm, Läufer, Springer (nach unten)
        // Schwarz (unten): Springer, Läufer, Turm, Dame (nach oben, damit Dame auf dem Ziel-Feld liegt)
        boolean isWhite = (amZug == Figur.Farbe.WEISS);
        int start = isWhite ? 0 : typen.length - 1;
        int end = isWhite ? typen.length : -1;
        int step = isWhite ? 1 : -1;

        for (int i = start; i != end; i += step) {
            String typ = typen[i];
            JButton btn = new JButton(symbole[i]);
            // Schriftgröße vom aktuellen Brett übernehmen
            btn.setFont(felder[0][0].getFont());
            btn.setMargin(new Insets(0, 0, 0, 0));
            btn.setFocusPainted(false);
            btn.setBackground(Color.WHITE);
            
            btn.addActionListener(e -> {
                brett.promoviereBauer(zeile, spalte, typ);
                promotionPanel.setVisible(false); // Panel wieder verstecken
                isPromoting = false; // Brett wieder freigeben
                amZug = (amZug == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
                aktualisiereBrett();
            });
            promotionPanel.add(btn);
        }

        updatePromotionPanelBounds(brettPanel.getWidth());
        promotionPanel.setVisible(true);
    }

    private JLabel createTimerLabel() {
        String text = (zeitWeiss == -1) ? "" : formatZeit(zeitWeiss);
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 24));
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        return label;
    }

    private void startTimer() {
        if (zeitWeiss == -1) return;
        gameTimer = new Timer(1000, e -> {
            if (amZug == Figur.Farbe.WEISS) {
                zeitWeiss--;
                timerWeissLabel.setText(formatZeit(zeitWeiss));
            } else {
                zeitSchwarz--;
                timerSchwarzLabel.setText(formatZeit(zeitSchwarz));
            }
            
            if (zeitWeiss <= 0 || zeitSchwarz <= 0) {
                gameTimer.stop();
                JOptionPane.showMessageDialog(this, "Zeit abgelaufen!");
            }
        });
        gameTimer.start();
    }

    private String formatZeit(int sekunden) {
        int min = sekunden / 60;
        int sek = sekunden % 60;
        return String.format("%02d:%02d", min, sek);
    }
}
