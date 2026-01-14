package Spiel;

public class ersterBotSpieler extends BotSpieler {
    public ersterBotSpieler(Figur.Farbe farbe) {
        super(farbe);

    }

    @Override
    public int[] berechneZug(Brett brett) {
        // Starte Minimax mit Tiefe 3 (kannst du auf 4 erhöhen, wenn es performant genug ist)
        int besterZug = minimaxRoot(brett, 3, true);
        if (besterZug == 0) return null; // Verhindert NPE, wenn kein Zug möglich ist
        return zugDekodieren(besterZug);
    }

    // Root-Funktion: Iteriert über alle Züge und wählt den besten aus
    private int minimaxRoot(Brett brett, int tiefe, boolean isMaximizing) {
        int[] alleZuege = generiereAlleZuege(brett, this.farbe);
        
        // Fallback, falls keine Züge möglich sind
        if (alleZuege.length == 0) return 0;

        int besterZugEncodiert = alleZuege[0];
        int besteBewertung = -2000000000; // Minus Unendlich

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

    // Rekursive Minimax-Funktion mit Alpha-Beta Pruning
    private int minimax(Brett brett, int tiefe, int alpha, int beta, boolean isMaximizing) {
        if (tiefe == 0) {
            return evaluieren(brett, this.farbe);
        }

        // Wir müssen prüfen, wer gerade am Zug ist für die Zuggenerierung
        Figur.Farbe aktuelleFarbe = isMaximizing ? this.farbe : gegenFarbe(this.farbe);
        int[] alleZuege = generiereAlleZuege(brett, aktuelleFarbe);

        if (alleZuege.length == 0) {
            // Keine Züge mehr -> Matt oder Patt bewerten
            return evaluieren(brett, this.farbe);
        }

        if (isMaximizing) {
            int maxEval = -2000000000;
            for (int zugEncodiert : alleZuege) {
                int[] zugDecodiert = zugDekodieren(zugEncodiert);
                brett.bewegeFigur(zugDecodiert[0], zugDecodiert[1], zugDecodiert[2], zugDecodiert[3], zugDecodiert[4], zugDecodiert[5]);
                
                int eval = minimax(brett, tiefe - 1, alpha, beta, false);
                brett.undo();
                
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = 2000000000;
            for (int zugEncodiert : alleZuege) {
                int[] zugDecodiert = zugDekodieren(zugEncodiert);
                brett.bewegeFigur(zugDecodiert[0], zugDecodiert[1], zugDecodiert[2], zugDecodiert[3], zugDecodiert[4], zugDecodiert[5]);
                
                int eval = minimax(brett, tiefe - 1, alpha, beta, true);
                brett.undo();
                
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private int evaluieren(Brett brett, Figur.Farbe farbe) {
        int ende = brett.getSchachmatt();
        if(farbe == Figur.Farbe.WEISS) {
            if(ende == -1) return 1000000; // Schwarz Matt -> Weiß gewinnt
            if(ende == 1) return -1000000; // Weiß Matt -> Weiß verliert
        }else{
            if(ende == 1) return 1000000; // Weiß Matt -> Schwarz gewinnt
            if(ende == -1) return -1000000; // Schwarz Matt -> Schwarz verliert
        }
        if(ende == 2 || ende == -2 || ende == 3) return 0;

        return materialWert(brett, farbe) - materialWert(brett, (farbe == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS);
    }

    private int materialWert(Brett brett, Figur.Farbe farbe) {
        int wert = 0;
        // Nutze die Listen direkt aus dem Brett, falls möglich, oder iteriere (hier vereinfacht über ArrayList)
        java.util.ArrayList<Figur> figuren = (farbe == Figur.Farbe.WEISS) ? brett.getWeisseFiguren() : brett.getSchwarzeFiguren();
        for(Figur figur : figuren) {
            if(figur instanceof Bauer) {
                wert += 1;
            }else if(figur instanceof Springer) {
                wert += 3;
            }else if(figur instanceof Laeufer) {
                wert += 3;
            }else if(figur instanceof Turm) {
                wert += 5;
            }else if(figur instanceof Koenigin) {
                wert += 9;
            }
        }
        return wert;
    }

    private Figur.Farbe gegenFarbe(Figur.Farbe farbe) {
        return (farbe == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
    }
}
