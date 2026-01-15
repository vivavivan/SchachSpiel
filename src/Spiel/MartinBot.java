package Spiel;

/**
 * Minimax-Algorithmus + Alpha-Beta-Suche.
 * Er kann mehrere Züge vorausschauen.
 * Hat Martin auf Chess.com besiegt
 */
public class MartinBot extends BotSpieler {
    public MartinBot(Figur.Farbe farbe) {
        super(farbe);

    }

    // Die Hauptmethode des Bots.
    // Passt die Suchtiefe an die Anzahl der verbleibenden Figuren an
    @Override
    public int[] berechneZug(Brett brett) {

        Brett brettKopie = brett.erstelleKopie();
        int dynamischeTiefe = 4;
        int anzFiguren = 8;
        if(farbe == Figur.Farbe.WEISS) {
            anzFiguren = brettKopie.getWeisseFiguren().size();
        }if(farbe == Figur.Farbe.SCHWARZ) {
            anzFiguren = brettKopie.getSchwarzeFiguren().size();
        }
        if(anzFiguren < 8) {
            dynamischeTiefe = 5;
        }if(anzFiguren < 4) {
            dynamischeTiefe = 5;
        }
        System.out.println(dynamischeTiefe);
        int besterZug = minimaxStart(brettKopie, dynamischeTiefe-1);
        if (besterZug == 0) return null;
        return zugDekodieren(besterZug);
    }

    // Start-Funktion für Minimax: Iteriert über alle Züge auf der obersten Ebene.
    private int minimaxStart(Brett brett, int tiefe) {
        int[] alleZuege = generiereAlleZuege(brett, this.farbe);

        // Züge mischen damit der Bot mehr random spielt
        mischeZuege(alleZuege);

        if (alleZuege.length == 0) return 0;

        int besterZugEncodiert = alleZuege[0];
        int besteBewertung = -2000000000; // Minus "Unendlich"

        for (int zugEncodiert : alleZuege) {
            int[] zugDecodiert = zugDekodieren(zugEncodiert);
            brett.bewegeFigur(zugDecodiert[0], zugDecodiert[1], zugDecodiert[2], zugDecodiert[3], zugDecodiert[4], zugDecodiert[5]);

            // Nach unserem Zug ist der Gegner dran (minimizing)
            int wert = minimax(brett, tiefe - 1, -2000000000, 2000000000, false);

            brett.undo();

            if (wert > besteBewertung) {
                besteBewertung = wert;
                besterZugEncodiert = zugEncodiert;
            }
        }
        return besterZugEncodiert;
    }

    /**
     * Die rekursive vom Bot.
     * Simuliert abwechselnd Züge für sich selbst (maximiere) und den Gegner (minimiere).
     */
    private int minimax(Brett brett, int tiefe, int alpha, int beta, boolean bistDuDran) {
        if (tiefe == 0) {
            return evaluieren(brett, this.farbe);
        }

        // Wer ist gerade am Zug in der Simulation
        Figur.Farbe aktuelleFarbe = bistDuDran ? this.farbe : gegenFarbe(this.farbe);
        int[] alleZuege = generiereAlleZuege(brett, aktuelleFarbe);

        if (alleZuege.length == 0) {
            // Keine Züge mehr -> Matt oder Patt evaluieren
            return evaluieren(brett, this.farbe);
        }

        //Zug für uns evaluieren
        if (bistDuDran) {
            int maxEvalualtion = -2000000000;
            for (int zugEncodiert : alleZuege) {
                int[] zugDecodiert = zugDekodieren(zugEncodiert);
                brett.bewegeFigur(zugDecodiert[0], zugDecodiert[1], zugDecodiert[2], zugDecodiert[3], zugDecodiert[4], zugDecodiert[5]);

                int evaluation = minimax(brett, tiefe - 1, alpha, beta, false);
                brett.undo();

                maxEvalualtion = Math.max(maxEvalualtion, evaluation);
                alpha = Math.max(alpha, evaluation);
                //alpha beta vergleich: Evaluation vom Zug vom Gegner darf nicht besser sein als die Evaluation von unserem Zug;
                if (beta <= alpha) break;
            }
            return maxEvalualtion;
        } else {
            // Zug für den Gegner evaluieren
            int minEvaluation = 2000000000;
            for (int zugEncodiert : alleZuege) {
                int[] zugDecodiert = zugDekodieren(zugEncodiert);
                brett.bewegeFigur(zugDecodiert[0], zugDecodiert[1], zugDecodiert[2], zugDecodiert[3], zugDecodiert[4], zugDecodiert[5]);

                int evaluation = minimax(brett, tiefe - 1, alpha, beta, true);
                brett.undo();

                minEvaluation = Math.min(minEvaluation, evaluation);
                beta = Math.min(beta, evaluation);
                if (beta <= alpha) break;
            }
            return minEvaluation;
        }
    }

    /**
     * Bewertet die Brettstellung.
     * Berücksichtigt Material, Schachgebote und ob eine Figur angegriffen und ungedeckt ist.
     */
    private int evaluieren(Brett brett, Figur.Farbe farbe) {
        int ende = brett.getSchachmatt();
        int wert = 0;
        if(farbe == Figur.Farbe.WEISS) {
            if(ende == -1) return 2000000; // Schwarz Matt -> Weiß gewinnt
            if(ende == 1) return -2000000; // Weiß Matt -> Weiß verliert
        }else{
            if(ende == 1) return 2000000; // Weiß Matt -> Schwarz gewinnt
            if(ende == -1) return -2000000; // Schwarz Matt -> Schwarz verliert
        }
        if(ende == 2 || ende == -2 || ende == 3) return 0;

        if(brett.istKoenigBedroht(gegenFarbe(farbe))) {
            wert += 50;
        }

        return materialWert(brett, farbe) + wert - materialWert(brett, (farbe == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS);
    }

    // Berechnet den reinen Materialwert für eine Farbe.
    private int materialWert(Brett brett, Figur.Farbe farbe) {
        int wert = 0;
        // Scanne das ganze Brett, um sicherzugehen, dass wir keine "Geister-Figuren" zählen
        for (int z = 0; z < 8; z++) {
            for (int s = 0; s < 8; s++) {
                Figur f = brett.getFigur(z, s);
                if (f != null && f.getFarbe() == farbe) {
                    boolean feldBedroht = brett.istFeldBedroht(z, s, farbe);
                    boolean feldGedeckt = brett.istFeldBedroht(z, s, gegenFarbe(farbe));
                    if (f instanceof Bauer) {
                        wert += 100;
                        if (feldBedroht && !feldGedeckt) wert -= 50;
                    }
                    else if (f instanceof Springer) {
                        wert += 300;
                        if (feldBedroht && !feldGedeckt) wert -= 200;
                    }
                    else if (f instanceof Laeufer) {
                        wert += 300;
                        if (feldBedroht && !feldGedeckt) wert -= 200;
                    }
                    else if (f instanceof Turm) {
                        wert += 500;
                        if (feldBedroht && !feldGedeckt) wert -= 400;
                    }
                    else if (f instanceof Koenigin) {
                        wert += 900;
                        if (feldBedroht && !feldGedeckt) wert -= 700;
                    }
                    else if (f instanceof Koenig) {
                        wert += 10000;
                        if (feldBedroht) wert -= 50;
                    }
                }
            }
        }
        return wert;
    }

    private Figur.Farbe gegenFarbe(Figur.Farbe farbe) {
        return (farbe == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
    }
}
