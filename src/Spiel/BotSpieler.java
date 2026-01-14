package Spiel;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class BotSpieler extends Spieler{
    protected BotSpieler(Figur.Farbe farbe) {
        super(farbe);
    }

    public abstract int[] berechneZug(Brett brett);

    protected int[] generiereAlleZuege(Brett brett, Figur.Farbe farbe) {
        ArrayList<Figur> moeglicheFiguren = new ArrayList<>();
        
        // Maximal mögliche Züge im Schach sind selten über 218, 256 ist clean
        int[] zugSpeicher = new int[256];
        int count = 0;

        if(farbe == Figur.Farbe.WEISS) {
            moeglicheFiguren = brett.getWeisseFiguren();
        }else if(farbe == Figur.Farbe.SCHWARZ) {
            moeglicheFiguren = brett.getSchwarzeFiguren();
        }

        for(Figur figur : moeglicheFiguren) {
            int vonZeile = figur.getZeile();
            int vonSpalte = figur.getSpalte();
            
            // Encodieren von vonZeile + vonSpalte...
            int startFeld = (vonZeile << 3) | vonSpalte;

            ArrayList<int[]> zuegeEinerFigur = brett.getLegaleZuege(vonZeile, vonSpalte);
            for(int[] endFeld : zuegeEinerFigur) {
                int nachZeile = endFeld[0];
                int nachSpalte = endFeld[1];
                int zielFeld = (nachZeile << 3) | nachSpalte;

                int basisZug = (startFeld << 6) | zielFeld;

                // Prüft auf Bauernumwandlung
                // Bauer erreicht die letzte oder erste Zeile
                if (figur instanceof Bauer && (nachZeile == 0 || nachZeile == 7)) {
                    // 4 unterschiedliche Züge für Dame, Turm, Läufer, Springer
                    for (int promoTyp = 0; promoTyp < 4; promoTyp++) {
                        // Bit 12: Promotion?, Bits 13-14: Welcher Typ?
                        int promoZug = basisZug | (1 << 12) | (promoTyp << 13);
                        zugSpeicher[count++] = promoZug;
                    }
                } else {
                    // Normaler Zug ohne Umwandlung
                    zugSpeicher[count++] = basisZug;
                }
            }
        }

        return Arrays.copyOf(zugSpeicher, count);
    }

    @Override
    public boolean istBot() {
        return true;
    }

    // Hilfsmethode zum Entschlüsseln der Züge
    protected int[] zugDekodieren(int move) {
        int zielFeld = move & 0x3F;       // Die unteren 6 Bits (111111 in binär ist 0x3F)
        int startFeld = (move >> 6) & 0x3F; // 6 Bits nach rechts schieben, dann "isolieren"

        // Bei Bauernumwandlung
        int istPromotion = (move >> 12) & 1;
        int promoTypIndex = (move >> 13) & 3;

        int vonZeile = startFeld >> 3;    // Sozusagen durch 8 teilen
        int vonSpalte = startFeld & 7;    // Sozusagen modulo 8
        int nachZeile = zielFeld >> 3;
        int nachSpalte = zielFeld & 7;

        return new int[]{vonZeile, vonSpalte, nachZeile, nachSpalte, istPromotion, promoTypIndex};
    }
}
