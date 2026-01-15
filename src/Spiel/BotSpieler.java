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
        int anzahl = 0;

        if(farbe == Figur.Farbe.WEISS) {
            moeglicheFiguren = new ArrayList<>(brett.getWeisseFiguren());
        }else if(farbe == Figur.Farbe.SCHWARZ) {
            moeglicheFiguren = new ArrayList<>(brett.getSchwarzeFiguren());
        }

        for(Figur figur : moeglicheFiguren) {
            int vonZeile = figur.getZeile();
            int vonSpalte = figur.getSpalte();
            
            // Encodieren von vonZeile + vonSpalte...
            int startFeld = (vonZeile << 3) | vonSpalte;

            ArrayList<int[]> zuegeEinerFigur = brett.getLegaleZuege(vonZeile, vonSpalte);
            for(int[] zielKoordinaten : zuegeEinerFigur) {
                // Array vergrößern, falls voll (wir fügen bis zu 4 Züge hinzu)
                if (anzahl + 4 >= zugSpeicher.length) {
                    zugSpeicher = Arrays.copyOf(zugSpeicher, zugSpeicher.length * 2);
                }

                int nachZeile = zielKoordinaten[0];
                int nachSpalte = zielKoordinaten[1];
                int zielFeld = (nachZeile << 3) | nachSpalte;

                int basisZug = (startFeld << 6) | zielFeld;

                // Prüft auf Bauernumwandlung
                // Bauer erreicht die letzte oder erste Zeile
                if (figur instanceof Bauer && (nachZeile == 0 || nachZeile == 7)) {
                    // 4 unterschiedliche Züge für Dame, Turm, Läufer, Springer
                    for (int umwandlungsTyp = 0; umwandlungsTyp < 4; umwandlungsTyp++) {
                        // Bit 12: Promotion?, Bits 13-14: Welcher Typ?
                        int umwandlungsZug = basisZug | (1 << 12) | (umwandlungsTyp << 13);
                        zugSpeicher[anzahl++] = umwandlungsZug;
                    }
                } else {
                    // Normaler Zug ohne Umwandlung
                    zugSpeicher[anzahl++] = basisZug;
                }
            }
        }

        return Arrays.copyOf(zugSpeicher, anzahl);
    }

    // mischt Züge durch, damit bei "gleicher Evaluation" unterschiedliche Züge gespielt werden
    protected void mischeZuege(int[] zuege) {
        java.util.Random zufall = new java.util.Random();
        for (int i = zuege.length - 1; i > 0; i--) {
            int index = zufall.nextInt(i + 1);
            int temp = zuege[index];
            zuege[index] = zuege[i];
            zuege[i] = temp;
        }
    }

    @Override
    public boolean istBot() {
        return true;
    }

    // Hilfsmethode zum Entschlüsseln der Züge
    protected int[] zugDekodieren(int zugKodiert) {
        int zielFeld = zugKodiert & 0x3F;       // Die unteren 6 Bits (111111 in binär ist 0x3F)
        int startFeld = (zugKodiert >> 6) & 0x3F; // 6 Bits nach rechts schieben, dann "isolieren"

        // Bei Bauernumwandlung
        int istPromotion = (zugKodiert >> 12) & 1;
        int umwandlungsTypIndex = (zugKodiert >> 13) & 3;

        int vonZeile = startFeld >> 3;    // Sozusagen durch 8 teilen
        int vonSpalte = startFeld & 7;    // Sozusagen modulo 8
        int nachZeile = zielFeld >> 3;
        int nachSpalte = zielFeld & 7;

        return new int[]{vonZeile, vonSpalte, nachZeile, nachSpalte, istPromotion, umwandlungsTypIndex};
    }
}
